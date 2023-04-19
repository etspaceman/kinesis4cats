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

import cats.Applicative
import cats.effect._
import cats.effect.syntax.all._
import org.http4s.client.Client
import org.typelevel.log4cats.StructuredLogger
import org.typelevel.log4cats.noop.NoOpLogger
import smithy4s.aws.kernel.AwsRegion

import kinesis4cats.localstack.LocalstackConfig
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
    * @param F
    *   [[cats.effect.Async Async]]
    * @return
    *   [[https://github.com/disneystreaming/smithy4s/blob/series/0.17/modules/aws-kernel/src/smithy4s/aws/AwsEnvironment.scala AwsEnvironment]]
    */
  def resource[F[_]](
      client: Client[F],
      region: F[AwsRegion],
      producerConfig: Producer.Config[F],
      config: LocalstackConfig,
      loggerF: Async[F] => F[StructuredLogger[F]],
      shardMapF: (
          KinesisClient[F],
          StreamNameOrArn,
          Async[F]
      ) => F[Either[ShardMapCache.Error, ShardMap]],
      encoders: Producer.LogEncoders,
      shardMapEncoders: ShardMapCache.LogEncoders,
      kinesisClientEncoders: KinesisClient.LogEncoders[F],
      localstackConfigEncoders: LocalstackConfig.LogEncoders
  )(implicit F: Async[F]): Resource[F, KinesisProducer[F]] = for {
    logger <- loggerF(F).toResource
    underlying <- LocalstackKinesisClient
      .clientResource[F](
        client,
        region,
        config,
        loggerF,
        kinesisClientEncoders,
        localstackConfigEncoders
      )
    shardMapCache <- ShardMapCache[F](
      producerConfig.shardMapCacheConfig,
      shardMapF(underlying, producerConfig.streamNameOrArn, F),
      loggerF(F),
      shardMapEncoders
    )
    producer = new KinesisProducer[F](
      logger,
      shardMapCache,
      producerConfig,
      underlying,
      encoders
    )
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
      producerConfig: (String, Applicative[F]) => Producer.Config[F] =
        (streamName: String, f: Applicative[F]) =>
          Producer.Config
            .default[F](StreamNameOrArn.Name(streamName))(f),
      loggerF: Async[F] => F[StructuredLogger[F]] = (f: Async[F]) =>
        f.pure(NoOpLogger[F](f)),
      shardMapF: (
          KinesisClient[F],
          StreamNameOrArn,
          Async[F]
      ) => F[Either[ShardMapCache.Error, ShardMap]] = (
          client: KinesisClient[F],
          streamNameOrArn: StreamNameOrArn,
          f: Async[F]
      ) => KinesisProducer.getShardMap(client, streamNameOrArn)(f),
      encoders: Producer.LogEncoders = Producer.LogEncoders.show,
      shardMapEncoders: ShardMapCache.LogEncoders =
        ShardMapCache.LogEncoders.show,
      kinesisClientEncoders: KinesisClient.LogEncoders[F] =
        KinesisClient.LogEncoders.show[F],
      localstackConfigEncoders: LocalstackConfig.LogEncoders =
        LocalstackConfig.LogEncoders.show
  )(implicit F: Async[F]): Resource[F, KinesisProducer[F]] = LocalstackConfig
    .resource[F](prefix)
    .flatMap(
      resource[F](
        client,
        region,
        producerConfig(streamName, F),
        _,
        loggerF,
        shardMapF,
        encoders,
        shardMapEncoders,
        kinesisClientEncoders,
        localstackConfigEncoders
      )
    )
}
