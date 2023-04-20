/*
 * Copyright 2023-2023 etspaceman
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kinesis4cats.smithy4s.client
package localstack

import cats.effect.Async
import cats.effect.Resource
import cats.syntax.all._
import com.amazonaws.kinesis._
import org.http4s.client.Client
import org.typelevel.log4cats.StructuredLogger
import org.typelevel.log4cats.noop.NoOpLogger
import smithy4s.aws._
import smithy4s.aws.kernel.AwsRegion

import kinesis4cats.compat.retry._
import kinesis4cats.localstack.LocalstackConfig
import kinesis4cats.localstack.TestStreamConfig
import kinesis4cats.smithy4s.client.KinesisClient
import kinesis4cats.smithy4s.client.middleware._

/** Like KinesisClient, but also includes the
  * [[kinesis4cats.smithy4s.client.middleware.LocalstackProxy LocalstackProxy]]
  * middleware, and leverages mock AWS credentials
  */
object LocalstackKinesisClient {

  final class LogEncoders[F[_]](
      val kinesisClientEncoders: KinesisClient.LogEncoders[F],
      val localstackConfigEncoders: LocalstackConfig.LogEncoders
  )

  object LogEncoders {
    def show[F[_]]: LogEncoders[F] = new LogEncoders(
      KinesisClient.LogEncoders.show[F],
      LocalstackConfig.LogEncoders.show
    )
  }

  final case class Builder[F[_]] private (
      client: Client[F],
      region: AwsRegion,
      localstackConfig: LocalstackConfig,
      logger: StructuredLogger[F],
      encoders: LogEncoders[F],
      logRequestsResponses: Boolean,
      streamsToCreate: List[TestStreamConfig[F]]
  )(implicit F: Async[F]) {

    def withLocalstackConfig(localstackConfig: LocalstackConfig): Builder[F] =
      copy(localstackConfig = localstackConfig)
    def withClient(client: Client[F]): Builder[F] = copy(client = client)
    def withRegion(region: AwsRegion): Builder[F] = copy(region = region)
    def withLogger(logger: StructuredLogger[F]): Builder[F] =
      copy(logger = logger)
    def withLogEncoders(encoders: LogEncoders[F]): Builder[F] =
      copy(encoders = encoders)
    def withLogRequestsResponses(logRequestsResponses: Boolean): Builder[F] =
      copy(logRequestsResponses = logRequestsResponses)
    def enableLogging: Builder[F] = withLogRequestsResponses(true)
    def disableLogging: Builder[F] = withLogRequestsResponses(false)
    def withStreamsToCreate(
        streamsToCreate: List[TestStreamConfig[F]]
    ): Builder[F] =
      copy(streamsToCreate = streamsToCreate)

    def build: Resource[F, KinesisClient[F]] = for {
      client <- KinesisClient.Builder
        .default[F](
          LocalstackProxy[F](localstackConfig, logger, encoders)(client),
          region
        )
        .withLogEncoders(encoders.kinesisClientEncoders)
        .withLogger(logger)
        .withLogRequestsResponses(logRequestsResponses)
        .withCredentials(_ =>
          Resource.pure(
            F.pure(
              AwsCredentials.Default("mock-key-id", "mock-secret-key", None)
            )
          )
        )
        .build
      _ <- streamsToCreate.traverse_(config =>
        Builder.managedStream(config, client)
      )
    } yield client

  }

  object Builder {
    private def managedStream[F[_]](
        config: TestStreamConfig[F],
        client: KinesisClient[F]
    )(implicit F: Async[F]): Resource[F, KinesisClient[F]] =
      Resource.make[F, KinesisClient[F]](
        for {
          _ <- client.createStream(
            StreamName(config.streamName),
            Some(PositiveIntegerObject(config.shardCount))
          )
          _ <- retryingOnFailuresAndAllErrors(
            config.describeRetryPolicy,
            (x: DescribeStreamSummaryOutput) =>
              F.pure(
                x.streamDescriptionSummary.streamStatus == StreamStatus.ACTIVE
              ),
            noop[F, DescribeStreamSummaryOutput],
            noop[F, Throwable]
          )(
            client.describeStreamSummary(Some(StreamName(config.streamName)))
          )
        } yield client
      )(client =>
        for {
          _ <- client.deleteStream(
            Some(StreamName(config.streamName))
          )
          _ <- retryingOnFailuresAndSomeErrors(
            config.describeRetryPolicy,
            (x: Either[Throwable, DescribeStreamSummaryOutput]) =>
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
            noop[F, Either[Throwable, DescribeStreamSummaryOutput]],
            noop[F, Throwable]
          )(
            client
              .describeStreamSummary(Some(StreamName(config.streamName)))
              .attempt
          )
        } yield ()
      )

    def default[F[_]](
        client: Client[F],
        region: AwsRegion,
        prefix: Option[String] = None
    )(implicit
        F: Async[F]
    ): F[Builder[F]] = LocalstackConfig
      .load(prefix)
      .map(default(client, region, _))

    def default[F[_]](
        client: Client[F],
        region: AwsRegion,
        config: LocalstackConfig
    )(implicit
        F: Async[F]
    ): Builder[F] =
      Builder[F](
        client,
        region,
        config,
        NoOpLogger[F],
        LogEncoders.show,
        true,
        Nil
      )
    
    @annotation.unused
    private def unapply[F[_]](builder: Builder[F]): Unit = ()
  }
}
