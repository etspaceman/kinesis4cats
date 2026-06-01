/*
 * Copyright 2023-2026 etspaceman
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
package fs2
package localstack

import _root_.fs2.compression.Compression
import _root_.fs2.concurrent.Channel
import _root_.fs2.io.file.Files
import cats.effect._
import cats.effect.syntax.all._
import cats.syntax.all._
import com.amazonaws.kinesis.PutRecordsOutput
import org.http4s.client.Client
import org.typelevel.log4cats.StructuredLogger
import org.typelevel.log4cats.noop.NoOpLogger
import org.typelevel.otel4s.metrics.MeterProvider
import smithy4s.aws.kernel.AwsRegion

import kinesis4cats.localstack.LocalstackConfig
import kinesis4cats.localstack.TestStreamConfig
import kinesis4cats.models.StreamNameOrArn
import kinesis4cats.producer.ShardMap
import kinesis4cats.producer.ShardMapCache
import kinesis4cats.producer.fs2.FS2Producer
import kinesis4cats.producer.metrics.FS2ProducerMetrics
import kinesis4cats.producer.metrics.ProducerMetrics
import kinesis4cats.smithy4s.client.producer.localstack.LocalstackKinesisProducer

/** Like KinesisProducer, but also includes the
  * [[kinesis4cats.smithy4s.client.middleware.LocalstackProxy LocalstackProxy]]
  * middleware, and leverages mock AWS credentials
  */
object LocalstackFS2KinesisProducer {
  final case class Builder[F[_]: Compression: Files] private (
      client: Client[F],
      region: AwsRegion,
      localstackConfig: LocalstackConfig,
      config: FS2Producer.Config[F],
      logger: StructuredLogger[F],
      encoders: LocalstackKinesisProducer.LogEncoders[F],
      logRequestsResponses: Boolean,
      streamsToCreate: List[TestStreamConfig[F]],
      shardMapF: (
          KinesisClient[F],
          StreamNameOrArn
      ) => F[Either[ShardMapCache.Error, ShardMap]],
      meterProvider: Option[MeterProvider[F]],
      namespace: String
  )(implicit F: Async[F]) {

    def withLocalstackConfig(localstackConfig: LocalstackConfig): Builder[F] =
      copy(localstackConfig = localstackConfig)
    def withConfig(config: FS2Producer.Config[F]): Builder[F] = copy(
      config = config
    )
    def withClient(client: Client[F]): Builder[F] = copy(client = client)
    def withRegion(region: AwsRegion): Builder[F] = copy(region = region)
    def withLogger(logger: StructuredLogger[F]): Builder[F] =
      copy(logger = logger)
    def withLogEncoders(
        encoders: LocalstackKinesisProducer.LogEncoders[F]
    ): Builder[F] =
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
    def withMetrics(
        meterProvider: MeterProvider[F],
        namespace: String = ProducerMetrics.defaultNamespace
    ): Builder[F] =
      copy(meterProvider = Some(meterProvider), namespace = namespace)

    def build: Resource[F, FS2KinesisProducer[F]] = for {
      fs2Metrics <- meterProvider.fold(
        Resource.pure[F, FS2ProducerMetrics[F]](FS2ProducerMetrics.noop[F])
      )(mp =>
        Resource.eval(
          mp.get(ProducerMetrics.instrumentationScope)
            .flatMap(FS2ProducerMetrics.fromMeter[F](_, namespace))
        )
      )
      putMetrics <- meterProvider.fold(
        Resource.pure[F, ProducerMetrics[F]](ProducerMetrics.noop[F])
      )(mp =>
        Resource.eval(
          mp.get(ProducerMetrics.instrumentationScope)
            .flatMap(ProducerMetrics.fromMeter[F](_, namespace))
        )
      )
      finalConfig = config.copy(
        metrics = fs2Metrics,
        producerConfig = config.producerConfig.copy(metrics = putMetrics)
      )
      underlying <- LocalstackKinesisProducer.Builder
        .default[F](
          client,
          region,
          finalConfig.producerConfig.streamNameOrArn,
          localstackConfig
        )
        .withConfig(finalConfig.producerConfig)
        .withLogEncoders(encoders)
        .withLogger(logger)
        .withLogRequestsResponses(logRequestsResponses)
        .withStreamsToCreate(streamsToCreate)
        .withShardMapF(shardMapF)
        .build
      channel <- Channel
        .bounded[F, FS2Producer.Buffered[F, PutRecordsOutput]](
          finalConfig.queueSize
        )
        .toResource
      producer = new FS2KinesisProducer[F](
        logger,
        finalConfig,
        channel,
        underlying
      )
      _ <- producer.resource
    } yield producer
  }

  object Builder {
    def default[F[_]: Async: Compression: Files](
        client: Client[F],
        region: AwsRegion,
        streamNameOrArn: StreamNameOrArn,
        prefix: Option[String] = None
    ): F[Builder[F]] = LocalstackConfig
      .load(prefix)
      .map(default(client, region, streamNameOrArn, _))

    def default[F[_]: Async: Compression: Files](
        client: Client[F],
        region: AwsRegion,
        streamNameOrArn: StreamNameOrArn,
        config: LocalstackConfig
    ): Builder[F] =
      Builder[F](
        client,
        region,
        config,
        FS2Producer.Config.default[F](streamNameOrArn),
        NoOpLogger[F],
        LocalstackKinesisProducer.LogEncoders.show,
        logRequestsResponses = true,
        Nil,
        (client: KinesisClient[F], snoa: StreamNameOrArn) =>
          KinesisProducer.getShardMap(client, snoa),
        meterProvider = None,
        namespace = ProducerMetrics.defaultNamespace
      )

    @annotation.unused
    private def unapply[F[_]](builder: Builder[F]): Unit = ()
  }
}
