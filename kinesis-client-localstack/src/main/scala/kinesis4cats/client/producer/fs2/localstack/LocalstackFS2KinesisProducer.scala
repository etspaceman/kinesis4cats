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

package kinesis4cats.client.producer
package fs2
package localstack

import _root_.fs2.concurrent.Channel
import cats.effect._
import cats.effect.syntax.all._
import cats.syntax.all._
import org.typelevel.log4cats.StructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient
import software.amazon.awssdk.services.kinesis.model.PutRecordsResponse

import kinesis4cats.client.KinesisClient
import kinesis4cats.client.localstack.LocalstackKinesisClient
import kinesis4cats.client.producer.localstack.LocalstackKinesisProducer
import kinesis4cats.localstack.LocalstackConfig
import kinesis4cats.localstack.TestStreamConfig
import kinesis4cats.models.StreamNameOrArn
import kinesis4cats.producer.Producer
import kinesis4cats.producer.Record
import kinesis4cats.producer.ShardMap
import kinesis4cats.producer.ShardMapCache
import kinesis4cats.producer.fs2.FS2Producer

object LocalstackFS2KinesisProducer {

  final case class Builder[F[_]] private (
      clientResource: Resource[F, KinesisClient[F]],
      localstackConfig: LocalstackConfig,
      config: FS2Producer.Config[F],
      logger: StructuredLogger[F],
      encoders: KinesisProducer.LogEncoders,
      streamsToCreate: List[TestStreamConfig[F]],
      shardMapF: (
          KinesisClient[F],
          StreamNameOrArn
      ) => F[Either[ShardMapCache.Error, ShardMap]],
      callback: Producer.Result[PutRecordsResponse] => F[Unit]
  )(implicit F: Async[F]) {

    def withLocalstackConfig(localstackConfig: LocalstackConfig): Builder[F] =
      copy(localstackConfig = localstackConfig)
    def withConfig(config: FS2Producer.Config[F]): Builder[F] = copy(
      config = config
    )
    def withClient(
        client: => KinesisAsyncClient,
        managed: Boolean = true
    ): Builder[F] = copy(
      clientResource =
        KinesisClient.Builder.default.withClient(client, managed).build
    )
    def withClient(client: KinesisClient[F]): Builder[F] = copy(
      clientResource = Resource.pure(client)
    )
    def withLogger(logger: StructuredLogger[F]): Builder[F] =
      copy(logger = logger)
    def withLogEncoders(encoders: KinesisProducer.LogEncoders): Builder[F] =
      copy(encoders = encoders)
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

    def build: Resource[F, FS2KinesisProducer[F]] = for {
      client <- clientResource
      underlying <- LocalstackKinesisProducer.Builder
        .default[F](
          config.producerConfig.streamNameOrArn,
          localstackConfig
        )
        .withClient(client)
        .withConfig(config.producerConfig)
        .withLogEncoders(encoders)
        .withLogger(logger)
        .withStreamsToCreate(streamsToCreate)
        .withShardMapF(shardMapF)
        .build
      channel <- Channel
        .bounded[
          F,
          (Record, Deferred[F, F[Producer.Result[PutRecordsResponse]]])
        ](config.queueSize)
        .toResource
      producer = new FS2KinesisProducer[F](
        logger,
        config,
        channel,
        underlying
      )
      _ <- producer.resource
    } yield producer
  }

  object Builder {
    def default[F[_]](
        streamNameOrArn: StreamNameOrArn,
        prefix: Option[String] = None
    )(implicit
        F: Async[F]
    ): F[Builder[F]] = LocalstackConfig
      .load(prefix)
      .map(default(streamNameOrArn, _))

    def default[F[_]](
        streamNameOrArn: StreamNameOrArn,
        config: LocalstackConfig
    )(implicit
        F: Async[F]
    ): Builder[F] =
      Builder[F](
        LocalstackKinesisClient.Builder.default[F](config).build,
        config,
        FS2Producer.Config.default[F](streamNameOrArn),
        Slf4jLogger.getLogger[F],
        KinesisProducer.LogEncoders.show,
        Nil,
        (client: KinesisClient[F], snoa: StreamNameOrArn) =>
          KinesisProducer.getShardMap(client, snoa),
        (_: Producer.Result[PutRecordsResponse]) => F.unit
      )

    @annotation.unused
    private def unapply[F[_]](builder: Builder[F]): Unit = ()
  }
}
