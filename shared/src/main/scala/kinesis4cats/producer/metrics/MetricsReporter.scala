package kinesis4cats.producer.metrics

import scala.concurrent.duration._

import cats.Applicative
import cats.effect.Async
import cats.effect.Fiber
import cats.effect.Resource
import cats.effect.syntax.all._
import cats.syntax.all._
import fs2.Chunk
import fs2.concurrent.Channel
import org.typelevel.log4cats.StructuredLogger

import kinesis4cats.compat.retry._
import kinesis4cats.logging._

abstract class MetricsReporter[F[_], PutRes](
    encoders: MetricsReporter.LogEncoders
)(implicit F: Async[F]) {
  def config: MetricsReporter.Config[F]
  def logger: StructuredLogger[F]
  protected def channel: Channel[F, Metric]

  import encoders._

  /** A user defined function that can be run against the results of a request
    */
  protected def callback: PutRes => F[Unit]

  def _put(metrics: Chunk[Metric]): F[PutRes]

  def put(metric: Metric): F[Unit] = {
    val ctx = LogContext()

    for {
      _ <- logger.debug(ctx.context)("Received metric to put")
      res <- channel.send(metric)
      _ <- res.bitraverse(
        _ =>
          logger.warn(ctx.context)(
            "MetricsReporter has been shut down and will not accept further requests"
          ),
        _ =>
          logger.debug(ctx.context)(
            "Successfully put metric into processing queue"
          )
      )
    } yield ()
  }

  /** Stop the processing of records
    */
  private[kinesis4cats] def stop(f: Fiber[F, Throwable, Unit]): F[Unit] = {
    val ctx = LogContext()
    for {
      _ <- logger.debug(ctx.context)("Stopping the MetricsReporter")
      _ <- channel.close
      _ <- f.join.void.timeoutTo(config.gracefulShutdownWait, f.cancel)
    } yield ()
  }

  /** Start the processing of records
    */
  private[kinesis4cats] def start(): F[Unit] = {
    val ctx = LogContext()

    for {
      _ <- logger
        .debug(ctx.context)("Starting the MetricsReporter")
      _ <- channel.stream
        .groupWithin(config.putMaxChunk, config.putMaxWait)
        .evalMap { x =>
          val c = ctx.addEncoded("batchSize", x.size)
          for {
            _ <- logger.debug(c.context)(
              "Received metrics batch to process"
            )
            res <- retryingOnAllErrors(
              config.retryPolicy,
              (e: Throwable, details: RetryDetails) =>
                logger
                  .error(ctx.addEncoded("retryDetails", details).context, e)(
                    "Exception when putting metrics, retrying"
                  )
            )(_put(x)).attempt
            _ <- res.leftTraverse { e =>
              if (config.raiseOnExhaustedRetries) F.raiseError(e).void
              else if (config.warnOnFailures)
                logger.warn(ctx.context, e)(
                  "Batch of metrics failed to upload"
                )
              else F.unit
            }
            _ <- logger.debug(c.context)(
              "Finished processing metrics batch"
            )
          } yield ()
        }
        .compile
        .drain
    } yield ()
  }

  private[kinesis4cats] def resource: Resource[F, Unit] =
    Resource.make(start().start)(stop).void
}

object MetricsReporter {

  /** [[kinesis4cats.logging.LogEncoder LogEncoder]] instances for the
    * [[kinesis4cats.producer.Producer]]
    *
    * @param recordLogEncoder
    * @param finiteDurationEncoder
    */
  final class LogEncoders(implicit
      val retryDetailsEncoder: LogEncoder[RetryDetails]
  )

  object LogEncoders {
    val show = {
      import kinesis4cats.logging.instances.show._
      new LogEncoders()
    }
  }

  final case class Config[F[_]](
      queueSize: Int,
      putMaxChunk: Int,
      putMaxWait: FiniteDuration,
      level: Level,
      granularity: Granularity,
      gracefulShutdownWait: FiniteDuration,
      raiseOnExhaustedRetries: Boolean,
      warnOnFailures: Boolean,
      retryPolicy: RetryPolicy[F]
  )

  object Config {
    def default[F[_]](implicit F: Applicative[F]): Config[F] = Config[F](
      1000,
      500,
      100.millis,
      Level.Detailed,
      Granularity.Shard,
      30.seconds,
      false,
      false,
      RetryPolicies.alwaysGiveUp[F]
    )
  }
}
