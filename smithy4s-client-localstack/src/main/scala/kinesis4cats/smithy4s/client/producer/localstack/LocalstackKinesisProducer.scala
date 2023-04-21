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
package producer
package localstack

import cats.effect._
import cats.syntax.all._
import org.http4s.client.Client
import org.typelevel.log4cats.StructuredLogger
import org.typelevel.log4cats.noop.NoOpLogger
import smithy4s.aws.kernel.AwsRegion

import kinesis4cats.localstack.LocalstackConfig
import kinesis4cats.localstack.TestStreamConfig
import kinesis4cats.models.StreamNameOrArn
import kinesis4cats.producer.Producer
import kinesis4cats.producer.ShardMap
import kinesis4cats.producer.ShardMapCache
import kinesis4cats.smithy4s.client.localstack.LocalstackKinesisClient

/** Like KinesisProducer, but also includes the
  * [[kinesis4cats.smithy4s.client.middleware.LocalstackProxy LocalstackProxy]]
  * middleware, and leverages mock AWS credentials
  */
object LocalstackKinesisProducer {

  final class LogEncoders[F[_]](
      val kinesisProducerEncoders: KinesisProducer.LogEncoders[F],
      val localstackConfigEncoders: LocalstackConfig.LogEncoders
  )

  object LogEncoders {
    def show[F[_]]: LogEncoders[F] = new LogEncoders(
      KinesisProducer.LogEncoders.show[F],
      LocalstackConfig.LogEncoders.show
    )
  }

  final case class Builder[F[_]] private (
      client: Client[F],
      region: AwsRegion,
      localstackConfig: LocalstackConfig,
      config: Producer.Config[F],
      logger: StructuredLogger[F],
      encoders: LogEncoders[F],
      logRequestsResponses: Boolean,
      streamsToCreate: List[TestStreamConfig[F]],
      shardMapF: (
          KinesisClient[F],
          StreamNameOrArn
      ) => F[Either[ShardMapCache.Error, ShardMap]]
  )(implicit F: Async[F]) {

    def withLocalstackConfig(localstackConfig: LocalstackConfig): Builder[F] =
      copy(localstackConfig = localstackConfig)
    def withConfig(config: Producer.Config[F]): Builder[F] = copy(
      config = config
    )
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
    def withStreamsToCreate(streamsToCreate: List[TestStreamConfig[F]]) =
      copy(streamsToCreate = streamsToCreate)
    def withShardMapF(
        shardMapF: (
            KinesisClient[F],
            StreamNameOrArn
        ) => F[Either[ShardMapCache.Error, ShardMap]]
    ): Builder[F] = copy(
      shardMapF = shardMapF
    )

    def build: Resource[F, KinesisProducer[F]] = for {
      underlying <- LocalstackKinesisClient.Builder
        .default[F](
          client,
          region,
          localstackConfig
        )
        .withLogEncoders(
          new LocalstackKinesisClient.LogEncoders(
            encoders.kinesisProducerEncoders.kinesisClientLogEncoders,
            encoders.localstackConfigEncoders
          )
        )
        .withLogger(logger)
        .withLogRequestsResponses(logRequestsResponses)
        .withStreamsToCreate(streamsToCreate)
        .build
      shardMapCache <- ShardMapCache.Builder
        .default[F](shardMapF(underlying, config.streamNameOrArn), logger)
        .withLogEncoders(
          encoders.kinesisProducerEncoders.producerLogEncoders.shardMapLogEncoders
        )
        .build
    } yield new KinesisProducer[F](
      logger,
      shardMapCache,
      config,
      underlying,
      encoders.kinesisProducerEncoders.producerLogEncoders
    )
  }

  object Builder {
    def default[F[_]](
        client: Client[F],
        region: AwsRegion,
        streamNameOrArn: StreamNameOrArn,
        prefix: Option[String] = None
    )(implicit
        F: Async[F]
    ): F[Builder[F]] = LocalstackConfig
      .load(prefix)
      .map(default(client, region, streamNameOrArn, _))

    def default[F[_]](
        client: Client[F],
        region: AwsRegion,
        streamNameOrArn: StreamNameOrArn,
        config: LocalstackConfig
    )(implicit
        F: Async[F]
    ): Builder[F] =
      Builder[F](
        client,
        region,
        config,
        Producer.Config.default[F](streamNameOrArn),
        NoOpLogger[F],
        LogEncoders.show,
        true,
        Nil,
        (client: KinesisClient[F], snoa: StreamNameOrArn) =>
          KinesisProducer.getShardMap(client, snoa)
      )

    @annotation.unused
    private def unapply[F[_]](builder: Builder[F]): Unit = ()
  }
}
