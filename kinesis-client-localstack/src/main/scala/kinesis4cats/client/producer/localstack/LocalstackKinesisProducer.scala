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

package kinesis4cats.client.producer.localstack

import cats.effect._
import cats.effect.syntax.all._
import cats.syntax.all._
import org.typelevel.log4cats.slf4j.Slf4jLogger

import kinesis4cats.client.KinesisClient
import kinesis4cats.client.producer.KinesisProducer
import kinesis4cats.localstack.LocalstackConfig
import kinesis4cats.localstack.aws.v2.AwsClients
import kinesis4cats.models.StreamNameOrArn
import kinesis4cats.producer.Producer
import kinesis4cats.producer.ShardMap
import kinesis4cats.producer.ShardMapCache

object LocalstackKinesisProducer {

  /** Builds a [[kinesis4cats.client.producer.KinesisProducer KinesisProducer]]
    * that is compliant for Localstack usage. Lifecycle is managed as a
    * [[cats.effect.Resource Resource]].
    *
    * @param producerConfig
    *   [[kinesis4cats.producer.Producer.Config Producer.Config]]
    * @param config
    *   [[kinesis4cats.localstack.LocalstackConfig LocalstackConfig]]
    * @param F
    *   F with an [[cats.effect.Async Async]] instance
    * @param LE
    *   [[kinesis4cats.client.KinesisClient.LogEncoders LogEncoders]]
    * @param SLE
    *   [[kinesis4cats.producer.ShardMapCache.LogEncoders ShardMapCache.LogEncoders]]
    * @param PLE
    *   [[kinesis4cats.producer.Producer.LogEncoders Producer.LogEncoders]]
    * @return
    *   [[cats.effect.Resource Resource]] of
    *   [[kinesis4cats.client.producer.KinesisProducer KinesisProducer]]
    */
  def resource[F[_]](
      producerConfig: Producer.Config,
      config: LocalstackConfig,
      shardMapF: (
          KinesisClient[F],
          StreamNameOrArn,
          Async[F]
      ) => F[Either[ShardMapCache.Error, ShardMap]]
  )(implicit
      F: Async[F],
      LE: KinesisClient.LogEncoders,
      SLE: ShardMapCache.LogEncoders,
      PLE: Producer.LogEncoders
  ): Resource[F, KinesisProducer[F]] = AwsClients
    .kinesisClientResource[F](config)
    .flatMap(_underlying =>
      for {
        logger <- Slf4jLogger.create[F].toResource
        underlying <- KinesisClient[F](_underlying)
        shardMapCache <- ShardMapCache[F](
          producerConfig.shardMapCacheConfig,
          shardMapF(underlying, producerConfig.streamNameOrArn, F),
          Slf4jLogger.create[F].widen
        )
        producer = new KinesisProducer[F](
          logger,
          shardMapCache,
          producerConfig,
          underlying
        )
      } yield producer
    )

  /** Builds a [[kinesis4cats.client.producer.KinesisProducer KinesisProducer]]
    * that is compliant for Localstack usage. Lifecycle is managed as a
    * [[cats.effect.Resource Resource]].
    *
    * @param streamName
    *   Name of stream for the producer to produce to
    * @param prefix
    *   Optional prefix for parsing configuration. Default to None
    * @param producerConfig
    *   String => [[kinesis4cats.producer.Producer.Config Producer.Config]]
    *   function that creates configuration given a stream name. Defaults to
    *   Producer.Config.default
    * @param F
    *   F with an [[cats.effect.Async Async]] instance
    * @param LE
    *   [[kinesis4cats.client.KinesisClient.LogEncoders LogEncoders]]
    * @param SLE
    *   [[kinesis4cats.producer.ShardMapCache.LogEncoders ShardMapCache.LogEncoders]]
    * @param PLE
    *   [[kinesis4cats.producer.Producer.LogEncoders Producer.LogEncoders]]
    * @return
    *   [[cats.effect.Resource Resource]] of
    *   [[kinesis4cats.client.producer.KinesisProducer KinesisProducer]]
    */
  def resource[F[_]](
      streamName: String,
      prefix: Option[String] = None,
      producerConfig: String => Producer.Config = (streamName: String) =>
        Producer.Config.default(StreamNameOrArn.Name(streamName)),
      shardMapF: (
          KinesisClient[F],
          StreamNameOrArn,
          Async[F]
      ) => F[Either[ShardMapCache.Error, ShardMap]] = (
          client: KinesisClient[F],
          streamNameOrArn: StreamNameOrArn,
          f: Async[F]
      ) => KinesisProducer.getShardMap(client, streamNameOrArn)(f)
  )(implicit
      F: Async[F],
      LE: KinesisClient.LogEncoders,
      SLE: ShardMapCache.LogEncoders,
      PLE: Producer.LogEncoders
  ): Resource[F, KinesisProducer[F]] = LocalstackConfig
    .resource[F](prefix)
    .flatMap(resource[F](producerConfig(streamName), _, shardMapF))
}
