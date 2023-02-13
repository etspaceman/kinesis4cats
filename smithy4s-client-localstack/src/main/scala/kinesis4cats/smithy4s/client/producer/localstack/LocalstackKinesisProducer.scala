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

package kinesis4cats.smithy4s.client.producer.localstack

import cats.effect._
import cats.effect.syntax.all._
import org.http4s.client.Client
import org.typelevel.log4cats.StructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import smithy4s.aws.kernel.AwsRegion

import kinesis4cats.localstack.LocalstackConfig
import kinesis4cats.logging.LogEncoder
import kinesis4cats.models.StreamNameOrArn
import kinesis4cats.producer.Producer
import kinesis4cats.producer.ShardMapCache
import kinesis4cats.smithy4s.client.KinesisClient
import kinesis4cats.smithy4s.client.localstack.LocalstackKinesisClient
import kinesis4cats.smithy4s.client.producer.KinesisProducer

object LocalstackKinesisProducer {
  def resource[F[_]](
      client: Client[F],
      region: F[AwsRegion],
      producerConfig: Producer.Config,
      config: LocalstackConfig,
      loggerF: Async[F] => F[StructuredLogger[F]]
  )(implicit
      F: Async[F],
      LE: KinesisClient.LogEncoders[F],
      LELC: LogEncoder[LocalstackConfig],
      SLE: ShardMapCache.LogEncoders,
      PLE: Producer.LogEncoders
  ): Resource[F, KinesisProducer[F]] = for {
    logger <- loggerF(F).toResource
    underlying <- LocalstackKinesisClient
      .clientResource[F](client, region, config, loggerF)
    shardMapCache <- ShardMapCache[F](
      producerConfig.shardMapCacheConfig,
      KinesisProducer.getShardMap(underlying, producerConfig.streamNameOrArn),
      loggerF(F)
    )
    producer = new KinesisProducer[F](
      logger,
      shardMapCache,
      producerConfig,
      underlying
    )
  } yield producer

  def resource[F[_]](
      client: Client[F],
      streamName: String,
      region: F[AwsRegion],
      prefix: Option[String] = None,
      producerConfig: String => Producer.Config = streamName =>
        Producer.Config
          .default(StreamNameOrArn.Name(streamName)),
      loggerF: Async[F] => F[StructuredLogger[F]] = (f: Async[F]) =>
        Slf4jLogger.create[F](f, implicitly)
  )(implicit
      F: Async[F],
      LE: KinesisClient.LogEncoders[F],
      LELC: LogEncoder[LocalstackConfig],
      SLE: ShardMapCache.LogEncoders,
      PLE: Producer.LogEncoders
  ): Resource[F, KinesisProducer[F]] = LocalstackConfig
    .resource[F](prefix)
    .flatMap(
      resource[F](client, region, producerConfig(streamName), _, loggerF)
    )
}
