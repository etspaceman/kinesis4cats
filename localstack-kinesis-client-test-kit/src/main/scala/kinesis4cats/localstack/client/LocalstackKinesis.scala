package kinesis4cats.localstack.client

import cats.effect.{Async, Resource}
import scala.concurrent.duration._
import kinesis4cats.localstack.LocalstackConfig
import kinesis4cats.client.KinesisClient
import kinesis4cats.localstack.aws.v2.AwsClients
import cats.syntax.all._
import org.typelevel.log4cats.slf4j.Slf4jLogger
import kinesis4cats.client.KinesisClientLogEncoders
import cats.effect.syntax.all._
import retry._
import retry.RetryPolicies._
import software.amazon.awssdk.services.kinesis.model._

object LocalstackKinesis {

  /** Builds a [[kinesis4cats.client.KinesisClient KinesisClient]] that is
    * compliant for Localstack usage.
    *
    * @param config
    *   [[kinesis4cats.localstack.LocalstackConfig LocalstackConfig]]
    * @param F
    *   F with an [[cats.effect.Async Async]] instance
    * @return
    *   F of [[kinesis4cats.client.KinesisClient KinesisClient]]
    */
  def client[F[_]](
      config: LocalstackConfig
  )(implicit F: Async[F], LE: KinesisClientLogEncoders): F[KinesisClient[F]] =
    for {
      underlying <- AwsClients.kinesisClient(config)
      logger <- Slf4jLogger.create[F]
    } yield new KinesisClient(underlying, logger)

  /** Builds a [[kinesis4cats.client.KinesisClient KinesisClient]] that is
    * compliant for Localstack usage.
    *
    * @param prefix
    *   Optional prefix for parsing configuration. Default to None
    * @param F
    *   F with an [[cats.effect.Async Async]] instance
    * @return
    *   F of [[kinesis4cats.client.KinesisClient KinesisClient]]
    */
  def client[F[_]](
      prefix: Option[String] = None
  )(implicit F: Async[F], LE: KinesisClientLogEncoders): F[KinesisClient[F]] =
    for {
      underlying <- AwsClients.kinesisClient(prefix)
      logger <- Slf4jLogger.create[F]
    } yield new KinesisClient(underlying, logger)

  /** Builds a [[kinesis4cats.client.KinesisClient KinesisClient]] that is
    * compliant for Localstack usage. Lifecycle is managed as a
    * [[cats.effect.Resource Resource]].
    *
    * @param config
    *   [[kinesis4cats.localstack.LocalstackConfig LocalstackConfig]]
    * @param F
    *   F with an [[cats.effect.Async Async]] instance
    * @return
    *   [[cats.effect.Resource Resource]] of
    *   [[kinesis4cats.client.KinesisClient KinesisClient]]
    */
  def clientResource[F[_]](config: LocalstackConfig)(implicit
      F: Async[F],
      LE: KinesisClientLogEncoders
  ): Resource[F, KinesisClient[F]] =
    client[F](config).toResource

  /** Builds a [[kinesis4cats.client.KinesisClient KinesisClient]] that is
    * compliant for Localstack usage. Lifecycle is managed as a
    * [[cats.effect.Resource Resource]].
    *
    * @param prefix
    *   Optional prefix for parsing configuration. Default to None
    * @param F
    *   F with an [[cats.effect.Async Async]] instance
    * @return
    *   [[cats.effect.Resource Resource]] of
    *   [[kinesis4cats.client.KinesisClient KinesisClient]]
    */
  def clientResource[F[_]](
      prefix: Option[String] = None
  )(implicit
      F: Async[F],
      LE: KinesisClientLogEncoders
  ): Resource[F, KinesisClient[F]] =
    client[F](prefix).toResource

  /** A resources that does the following:
    *
    *   - Builds a [[kinesis4cats.client.KinesisClient KinesisClient]] that is
    *     compliant for Localstack usage.
    *   - Creates a stream with the desired name and shard count, and waits
    *     until the stream is active.
    *   - Destroys the stream when the [[cats.effect.Resource Resource]] is
    *     closed
    *
    * @param config
    *   [[kinesis4cats.localstack.LocalstackConfig LocalstackConfig]]
    * @param streamName
    *   Stream name
    * @param shardCount
    *   Shard count for stream
    * @param describeRetries
    *   How many times to retry DescribeStreamSummary when checking the stream
    *   status
    * @param describeRetryDuration
    *   How long to delay between retries of the DescribeStreamSummary call
    * @param F
    *   F with an [[cats.effect.Async Async]] instance
    * @return
    *   [[cats.effect.Resource Resource]] of
    *   [[kinesis4cats.client.KinesisClient KinesisClient]]
    */
  def streamResource[F[_]](
      config: LocalstackConfig,
      streamName: String,
      shardCount: Int,
      describeRetries: Int,
      describeRetryDuration: FiniteDuration
  )(implicit
      F: Async[F],
      LE: KinesisClientLogEncoders
  ): Resource[F, KinesisClient[F]] = for {
    client <- clientResource(config)
    retryPolicy = constantDelay(describeRetryDuration).join(
      limitRetries(describeRetries)
    )
    result <- Resource.make(
      for {
        _ <- client.createStream(
          CreateStreamRequest
            .builder()
            .streamName(streamName)
            .shardCount(shardCount)
            .build()
        )
        _ <- retryingOnFailuresAndAllErrors(
          retryPolicy,
          (x: DescribeStreamSummaryResponse) =>
            F.pure(
              x.streamDescriptionSummary()
                .streamStatus() == StreamStatus.ACTIVE
            ),
          noop[F, DescribeStreamSummaryResponse],
          noop[F, Throwable]
        )(
          client.describeStreamSummary(
            DescribeStreamSummaryRequest
              .builder()
              .streamName(streamName)
              .build()
          )
        )
      } yield client
    )(client =>
      for {
        _ <- client.deleteStream(
          DeleteStreamRequest.builder().streamName(streamName).build()
        )
        _ <- retryingOnFailuresAndSomeErrors(
          retryPolicy,
          (x: Either[Throwable, DescribeStreamSummaryResponse]) =>
            F.pure(
              x.swap.exists {
                case _: ResourceNotFoundException => true
                case _                            => false
              }
            ),
          (e: Throwable) =>
            e match {
              case _: ResourceNotFoundException => F.pure(false)
              case _                            => F.pure(true)
            },
          noop[F, Either[Throwable, DescribeStreamSummaryResponse]],
          noop[F, Throwable]
        )(
          client
            .describeStreamSummary(
              DescribeStreamSummaryRequest
                .builder()
                .streamName(streamName)
                .build()
            )
            .attempt
        )
      } yield ()
    )
  } yield result

  /** A resources that does the following:
    *
    *   - Builds a [[kinesis4cats.client.KinesisClient KinesisClient]] that is
    *     compliant for Localstack usage.
    *   - Creates a stream with the desired name and shard count, and waits
    *     until the stream is active.
    *   - Destroys the stream when the [[cats.effect.Resource Resource]] is
    *     closed
    *
    * @param streamName
    *   Stream name
    * @param shardCount
    *   Shard count for stream
    * @param prefix
    *   Optional prefix for parsing configuration. Default to None
    * @param describeRetries
    *   How many times to retry DescribeStreamSummary when checking the stream
    *   status. Default to 5
    * @param describeRetryDuration
    *   How long to delay between retries of the DescribeStreamSummary call.
    *   Default to 500 ms
    * @param F
    *   F with an [[cats.effect.Async Async]] instance
    * @return
    *   [[cats.effect.Resource Resource]] of
    *   [[kinesis4cats.client.KinesisClient KinesisClient]]
    */
  def streamResource[F[_]](
      streamName: String,
      shardCount: Int,
      prefix: Option[String] = None,
      describeRetries: Int = 5,
      describeRetryDuration: FiniteDuration = 500.millis
  )(implicit
      F: Async[F],
      LE: KinesisClientLogEncoders
  ): Resource[F, KinesisClient[F]] = for {
    config <- LocalstackConfig.resource(prefix)
    result <- streamResource(
      config,
      streamName,
      shardCount,
      describeRetries,
      describeRetryDuration
    )
  } yield result
}
