package kinesis4cats.consumer
package fetch

import scala.concurrent.duration.FiniteDuration

import cats.effect._
import cats.effect.syntax.all._
import cats.syntax.all._
import fs2.Chunk
import fs2.Stream
import fs2.concurrent.Channel
import fs2.concurrent.SignallingRef
import org.typelevel.log4cats.StructuredLogger

import kinesis4cats.compat.retry._
import kinesis4cats.logging.LogContext
import kinesis4cats.models.ConsumerArn
import kinesis4cats.models.ShardId

abstract class Fetcher[F[_]] private[kinesis4cats] {
  def shardId: ShardId

  protected def logger: StructuredLogger[F]
  protected def channel: Channel[F, Chunk[CommittableRecord]]
}

object Fetcher {
  abstract class Polling[
      F[_],
      GetRecordsReq,
      GetRecordsRes,
      GetShardIteratorReq,
      GetShardIteratorRes
  ](implicit
      F: Async[F]
  ) extends Fetcher[F] {

    def config: Polling.Config[F]

    protected def getRecords(req: GetRecordsReq): F[GetRecordsRes]

    protected def getShardIterator(
        req: GetShardIteratorReq
    ): F[GetShardIteratorRes]

    protected def initialShardIteratorReq: F[GetShardIteratorReq]

    protected def getRecordsReq(shardIterator: String): GetRecordsReq

    protected def isThrottlingError(e: Throwable): Boolean

    protected def toCommittableRecords(
        x: GetRecordsRes
    ): List[CommittableRecord]

    protected def toShardIterator(x: GetShardIteratorRes): String

    protected def isEndOfShard(x: CommittableRecord): Boolean

    private def poll(
        iteratorCache: Ref[F, String]
    ): F[List[CommittableRecord]] = {
      val ctx = LogContext().addEncoded("shardId", shardId.shardId)
      for {
        _ <- logger.debug(ctx.context)("Polling data")
        shardIterator <- iteratorCache.get
        records <- retryingOnSomeErrors(
          config.throttledRetryPolicy,
          (e: Throwable) => F.delay(isThrottlingError(e)),
          (x: Throwable, _: RetryDetails) =>
            logger.warn(ctx.context, x)(
              "Throttling error getting records, retrying"
            )
        )(getRecords(getRecordsReq(shardIterator)))
      } yield toCommittableRecords(records)
    }

    /** Stop the processing of records
      */
    private[kinesis4cats] def stop(f: Fiber[F, Throwable, Unit]): F[Unit] = {
      val ctx = LogContext().addEncoded("shardId", shardId.shardId)
      for {
        _ <- logger.debug(ctx.context)("Stopping the PollingFetcher")
        _ <- channel.close
        _ <- f.join.void.timeoutTo(config.gracefulShutdownWait, f.cancel)
      } yield ()
    }

    /** Start the processing of records
      */
    private[kinesis4cats] def start(): F[Unit] = {
      val ctx = LogContext().addEncoded("shardId", shardId.shardId)

      for {
        _ <- logger
          .debug(ctx.context)("Starting the Polling fetcher")
        initialRequeset <- initialShardIteratorReq
        shardIterator <-
          retryingOnSomeErrors(
            config.throttledRetryPolicy,
            (e: Throwable) => F.delay(isThrottlingError(e)),
            (x: Throwable, _: RetryDetails) =>
              logger.warn(ctx.context, x)(
                "Throttling error getting shard iterator, retrying"
              )
          )(getShardIterator(initialRequeset))
        iteratorRef <- Ref.of(toShardIterator(shardIterator))
        interruptSignal <- SignallingRef[F, Boolean](false)
        res <- Stream
          .eval(poll(iteratorRef).map(Chunk.seq))
          .prefetchN(config.cachedResponses)
          .interruptWhen(interruptSignal)
          .evalMap(x =>
            channel.send(x).flatMap {
              _.bitraverse(
                _ =>
                  logger.warn(ctx.context)(
                    "Fetcher has been shut down and will not accept further requests. Shutting down prefetch loop."
                  ) >> interruptSignal.set(true),
                _ =>
                  x.maximumOption.traverse { r =>
                    if (isEndOfShard(r))
                      for {
                        _ <- logger.warn(ctx.context)(
                          "Prefetch batch sent downstream. The final record for the shard has been consumed. Shutting down prefetch loop."
                        )
                        _ <- channel.close
                        _ <- interruptSignal.set(true)
                      } yield ()
                    else
                      logger
                        .debug(ctx.context)("Prefetch batch sent downstream")
                  }
              )
            }
          )
          .compile
          .drain
      } yield res
    }

    private[kinesis4cats] def resource: Resource[F, Unit] =
      Resource.make(start().start)(stop).void
  }

  object Polling {
    final case class Config[F[_]](
        bufferSize: Int,
        cachedResponses: Int,
        maxRecordsPerResponse: Int,
        throttledRetryPolicy: RetryPolicy[F],
        gracefulShutdownWait: FiniteDuration
    )
  }

  abstract class FanOut[
      F[_],
      SubscribeToShardReq,
      SubscribeToShardEv,
      RegisterConsumerReq
  ](implicit
      F: Async[F]
  ) extends Fetcher[F] {
    def config: Polling.Config[F]

    protected def subscribeToShard(
        req: SubscribeToShardReq
    ): Stream[F, SubscribeToShardEv]

    protected def registerConsumerIfNotExists(
        req: RegisterConsumerReq
    ): F[ConsumerArn]

    protected def initialRegisterConsumerReq: RegisterConsumerReq

    private[kinesis4cats] def start(): F[Unit] = {
      val ctx = LogContext().addEncoded("shardId", shardId.shardId)

      for {
        _ <- logger
          .debug(ctx.context)("Starting the FanOut fetcher")
        consumerArn <- registerConsumerIfNotExists(initialRegisterConsumerReq)
      } yield ()

      F.unit
    }
  }

  object FanOut {
    final case class Config()
  }
}
