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
package fs2
package localstack

import _root_.fs2.concurrent.Channel
import cats.Applicative
import cats.effect._
import cats.effect.syntax.all._
import com.amazonaws.kinesis.PutRecordsOutput
import org.http4s.client.Client
import org.typelevel.log4cats.StructuredLogger
import org.typelevel.log4cats.noop.NoOpLogger
import smithy4s.aws.kernel.AwsRegion

import kinesis4cats.localstack.LocalstackConfig
import kinesis4cats.models.StreamNameOrArn
import kinesis4cats.producer.Producer
import kinesis4cats.producer.Record
import kinesis4cats.producer.ShardMapCache
import kinesis4cats.producer.fs2.FS2Producer
import kinesis4cats.smithy4s.client.localstack.LocalstackKinesisClient

/** Like KinesisProducer, but also includes the
  * [[kinesis4cats.smithy4s.client.middleware.LocalstackProxy LocalstackProxy]]
  * middleware, and leverages mock AWS credentials
  */
object LocalstackFS2KinesisProducer {

  /** Creates a [[cats.effect.Resource Resource]] of a
    * [[kinesis4cats.smithy4s.client.producer.KinesisProducer KinesisProducer]]
    * that is compatible with Localstack
    *
    * @param client
    *   [[https://http4s.org/v0.23/docs/client.html Client]]
    * @param region
    *   [[https://github.com/disneystreaming/smithy4s/blob/series/0.17/modules/aws-kernel/src/smithy4s/aws/AwsRegion.scala AwsRegion]]
    * @param producerConfig
    *   [[kinesis4cats.producer.Producer.Config Producer.Config]]
    * @param config
    *   [[kinesis4cats.localstack.LocalstackConfig LocalstackConfig]]
    * @param loggerF
    *   [[cats.effect.Async Async]] => [[cats.effect.Async Async]] of
    *   [[https://github.com/typelevel/log4cats/blob/main/core/shared/src/main/scala/org/typelevel/log4cats/StructuredLogger.scala StructuredLogger]].
    * @param callback
    *   Function that can be run after each of the put results from the
    *   underlying
    * @param F
    *   [[cats.effect.Async Async]]
    * @return
    *   [[https://github.com/disneystreaming/smithy4s/blob/series/0.17/modules/aws-kernel/src/smithy4s/aws/AwsEnvironment.scala AwsEnvironment]]
    */
  def resource[F[_]](
      client: Client[F],
      region: F[AwsRegion],
      producerConfig: FS2Producer.Config[F],
      config: LocalstackConfig,
      loggerF: Async[F] => F[StructuredLogger[F]],
      callback: (Producer.Res[PutRecordsOutput], Async[F]) => F[Unit],
      encoders: Producer.LogEncoders,
      shardMapEncoders: ShardMapCache.LogEncoders,
      kinesisClientEncoders: KinesisClient.LogEncoders[F],
      localstackConfigEncoders: LocalstackConfig.LogEncoders
  )(implicit F: Async[F]): Resource[F, FS2KinesisProducer[F]] = for {
    logger <- loggerF(F).toResource
    _underlying <- LocalstackKinesisClient
      .clientResource[F](
        client,
        region,
        config,
        loggerF,
        kinesisClientEncoders,
        localstackConfigEncoders
      )
    shardMapCache <- ShardMapCache[F](
      producerConfig.producerConfig.shardMapCacheConfig,
      KinesisProducer.getShardMap(
        _underlying,
        producerConfig.producerConfig.streamNameOrArn
      ),
      loggerF(F),
      shardMapEncoders
    )
    channel <- Channel
      .bounded[F, Record](producerConfig.queueSize)
      .toResource
    underlying = new KinesisProducer[F](
      logger,
      shardMapCache,
      producerConfig.producerConfig,
      _underlying,
      encoders
    )
    producer = new FS2KinesisProducer[F](
      logger,
      producerConfig,
      channel,
      underlying
    )(
      callback
    )
    _ <- producer.resource
  } yield producer

  /** Creates a [[cats.effect.Resource Resource]] of a
    * [[kinesis4cats.smithy4s.client.producer.KinesisProducer KinesisProducer]]
    * that is compatible with Localstack
    *
    * @param client
    *   [[https://http4s.org/v0.23/docs/client.html Client]]
    * @param streamName
    *   Name of stream that this producer will produce to
    * @param region
    *   [[https://github.com/disneystreaming/smithy4s/blob/series/0.17/modules/aws-kernel/src/smithy4s/aws/AwsRegion.scala AwsRegion]].
    * @param prefix
    *   Optional string prefix to apply when loading configuration. Default to
    *   None
    * @param producerConfig
    *   String => [[kinesis4cats.producer.Producer.Config Producer.Config]]
    *   function that creates configuration given a stream name. Defaults to
    *   Producer.Config.default
    * @param loggerF
    *   [[cats.effect.Async Async]] => [[cats.effect.Async Async]] of
    *   [[https://github.com/typelevel/log4cats/blob/main/core/shared/src/main/scala/org/typelevel/log4cats/StructuredLogger.scala StructuredLogger]].
    *   Default is
    *   [[https://github.com/typelevel/log4cats/blob/main/noop/shared/src/main/scala/org/typelevel/log4cats/noop/NoOpLogger.scala NoOpLogger]]
    * @param callback
    *   Function that can be run after each of the put results from the
    *   underlying. Default is F.unit
    * @param F
    *   [[cats.effect.Async Async]]
    * @return
    *   [[https://github.com/disneystreaming/smithy4s/blob/series/0.17/modules/aws-kernel/src/smithy4s/aws/AwsEnvironment.scala AwsEnvironment]]
    */
  def resource[F[_]](
      client: Client[F],
      streamName: String,
      region: F[AwsRegion],
      prefix: Option[String] = None,
      producerConfig: (String, Applicative[F]) => FS2Producer.Config[F] =
        (streamName: String, f: Applicative[F]) =>
          FS2Producer.Config
            .default[F](StreamNameOrArn.Name(streamName))(f),
      loggerF: Async[F] => F[StructuredLogger[F]] = (f: Async[F]) =>
        f.pure(NoOpLogger[F](f)),
      callback: (Producer.Res[PutRecordsOutput], Async[F]) => F[Unit] =
        (_: Producer.Res[PutRecordsOutput], f: Async[F]) => f.unit,
      encoders: Producer.LogEncoders = Producer.LogEncoders.show,
      shardMapEncoders: ShardMapCache.LogEncoders =
        ShardMapCache.LogEncoders.show,
      kinesisClientEncoders: KinesisClient.LogEncoders[F] =
        KinesisClient.LogEncoders.show[F],
      localstackConfigEncoders: LocalstackConfig.LogEncoders =
        LocalstackConfig.LogEncoders.show
  )(implicit F: Async[F]): Resource[F, FS2KinesisProducer[F]] = LocalstackConfig
    .resource[F](prefix)
    .flatMap(
      resource[F](
        client,
        region,
        producerConfig(streamName, F),
        _,
        loggerF,
        callback,
        encoders,
        shardMapEncoders,
        kinesisClientEncoders,
        localstackConfigEncoders
      )
    )
}
