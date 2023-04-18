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

import cats.Applicative
import cats.effect._
import software.amazon.awssdk.services.kinesis.model.PutRecordsResponse

import kinesis4cats.client.KinesisClient
import kinesis4cats.localstack.LocalstackConfig
import kinesis4cats.localstack.aws.v2.AwsClients
import kinesis4cats.models.StreamNameOrArn
import kinesis4cats.producer.Producer
import kinesis4cats.producer.ShardMapCache
import kinesis4cats.producer.fs2.FS2Producer

object LocalstackFS2KinesisProducer {

  /** Builds a [[kinesis4cats.client.producer.KinesisProducer KinesisProducer]]
    * that is compliant for Localstack usage. Lifecycle is managed as a
    * [[cats.effect.Resource Resource]].
    *
    * @param producerConfig
    *   [[kinesis4cats.producer.fs2.FS2Producer.Config FS2Producer.Config]]
    * @param config
    *   [[kinesis4cats.localstack.LocalstackConfig LocalstackConfig]]
    * @param callback
    *   Function that can be run after each of the put results from the
    *   underlying
    * @param F
    *   F with an [[cats.effect.Async Async]] instance
    * @param encoders
    *   [[kinesis4cats.producer.Producer.LogEncoders Producer.LogEncoders]].
    * @param shardMapEncoders
    *   [[kinesis4cats.producer.ShardMapCache.LogEncoders ShardMapCache.LogEncoders]]
    * @param kinesisClientEncoders
    *   [[kinesis4cats.client.KinesisClient.LogEncoders LogEncoders]]
    * @return
    *   [[cats.effect.Resource Resource]] of
    *   [[kinesis4cats.client.producer.fs2.FS2KinesisProducer FS2KinesisProducer]]
    */
  def resource[F[_]](
      producerConfig: FS2Producer.Config[F],
      config: LocalstackConfig,
      callback: (Producer.Res[PutRecordsResponse], Async[F]) => F[Unit],
      encoders: Producer.LogEncoders,
      shardMapEncoders: ShardMapCache.LogEncoders,
      kinesisClientEncoders: KinesisClient.LogEncoders
  )(implicit F: Async[F]): Resource[F, FS2KinesisProducer[F]] = AwsClients
    .kinesisClientResource[F](config)
    .flatMap(underlying =>
      FS2KinesisProducer.instance[F](
        producerConfig,
        underlying,
        callback,
        encoders,
        shardMapEncoders,
        kinesisClientEncoders
      )
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
    *   String =>
    *   [[kinesis4cats.producer.fs2.FS2Producer.Config FS2Producer.Config]]
    *   function that creates configuration given a stream name. Defaults to
    *   Producer.Config.default
    * @param callback
    *   Function that can be run after each of the put results from the
    *   underlying. Defaults to F.unit.
    * @param F
    *   F with an [[cats.effect.Async Async]] instance
    * @param encoders
    *   [[kinesis4cats.producer.Producer.LogEncoders Producer.LogEncoders]].
    *   Default to show instances
    * @param shardMapEncoders
    *   [[kinesis4cats.producer.ShardMapCache.LogEncoders ShardMapCache.LogEncoders]]
    *   Default to show instances
    * @param kinesisClientEncoders
    *   [[kinesis4cats.client.KinesisClient.LogEncoders LogEncoders]] Default to
    *   show instances
    * @return
    *   [[cats.effect.Resource Resource]] of
    *   [[kinesis4cats.client.producer.fs2.FS2KinesisProducer FS2KinesisProducer]]
    */
  def resource[F[_]](
      streamName: String,
      prefix: Option[String] = None,
      producerConfig: (String, Applicative[F]) => FS2Producer.Config[F] =
        (streamName: String, f: Applicative[F]) =>
          FS2Producer.Config.default[F](StreamNameOrArn.Name(streamName))(f),
      callback: (Producer.Res[PutRecordsResponse], Async[F]) => F[Unit] =
        (_: Producer.Res[PutRecordsResponse], f: Async[F]) => f.unit,
      encoders: Producer.LogEncoders = Producer.LogEncoders.show,
      shardMapEncoders: ShardMapCache.LogEncoders =
        ShardMapCache.LogEncoders.show,
      kinesisClientEncoders: KinesisClient.LogEncoders =
        KinesisClient.LogEncoders.show
  )(implicit F: Async[F]): Resource[F, FS2KinesisProducer[F]] = LocalstackConfig
    .resource[F](prefix)
    .flatMap(
      resource[F](
        producerConfig(streamName, F),
        _,
        callback,
        encoders,
        shardMapEncoders,
        kinesisClientEncoders
      )
    )
}
