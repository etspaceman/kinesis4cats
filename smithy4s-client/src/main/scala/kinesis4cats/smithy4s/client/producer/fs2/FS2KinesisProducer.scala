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

import _root_.fs2.concurrent.Channel
import cats.effect._
import cats.effect.syntax.all._
import com.amazonaws.kinesis.PutRecordsInput
import com.amazonaws.kinesis.PutRecordsOutput
import org.http4s.client.Client
import org.typelevel.log4cats.StructuredLogger
import org.typelevel.log4cats.noop.NoOpLogger
import smithy4s.aws.AwsCredentialsProvider
import smithy4s.aws.SimpleHttpClient
import smithy4s.aws.kernel.AwsCredentials
import smithy4s.aws.kernel.AwsRegion

import kinesis4cats.producer._
import kinesis4cats.producer.fs2.FS2Producer

/** A buffered Kinesis producer which will produce batches of data at a
  * configurable rate.
  *
  * @param config
  *   [[kinesis.producer.fs2.FS2Producer.Config FS2Producer.Config]]
  * @param channel
  *   [[https://github.com/typelevel/fs2/blob/main/core/shared/src/main/scala/fs2/concurrent/Channel.scala Channel]]
  *   of [[kinesis4cats.producer.Record Records]] to produce.
  * @param underlying
  *   [[kinesis4cats.smithy4s.client.producer.KinesisProducer KinesisProducer]]
  * @param callback:
  *   Function that can be run after each of the put results from the underlying
  * @param F
  *   [[cats.effect.Async Async]]
  * @param LE
  *   [[kinesis4cats.producer.Producer.LogEncoders Producer.LogEncoders]]
  */
final class FS2KinesisProducer[F[_]] private[kinesis4cats] (
    override val logger: StructuredLogger[F],
    override val config: FS2Producer.Config[F],
    override protected val channel: Channel[F, Record],
    override protected val underlying: KinesisProducer[F]
)(
    override protected val callback: (
        Producer.Res[PutRecordsOutput],
        Async[F]
    ) => F[Unit]
)(implicit
    F: Async[F]
) extends FS2Producer[F, PutRecordsInput, PutRecordsOutput]

object FS2KinesisProducer {

  /** Basic constructor for the
    * [[kinesis4cats.smithy4s.client.producer.fs2.FS2KinesisProducer FS2KinesisProducer]]
    *
    * @param config
    *   [[kinesis4cats.producer.fs2.FS2Producer.Config FS2Producer.Config]]
    * @param client
    *   [[org.http4s.client.Client Client]] instance
    * @param region
    *   [[https://github.com/disneystreaming/smithy4s/blob/series/0.17/modules/aws-kernel/src/smithy4s/aws/AwsRegion.scala AwsRegion]]
    * @param loggerF
    *   [[cats.effect.Async Async]] => F of
    *   [[org.typelevel.log4cats.StructuredLogger StructuredLogger]]. Default
    *   uses [[org.typelevel.log4cats.noop.NoOpLogger NoOpLogger]]
    * @param credsF
    *   (
    *   [[https://github.com/disneystreaming/smithy4s/blob/series/0.17/modules/aws/src/smithy4s/aws/SimpleHttpClient.scala SimpleHttpClient]],
    *   [[cats.effect.Async Async]]) => F of
    *   [[https://github.com/disneystreaming/smithy4s/blob/series/0.17/modules/aws-kernel/src/smithy4s/aws/AwsCredentials.scala AwsCredentials]]
    *   Default uses
    *   [[https://github.com/disneystreaming/smithy4s/blob/series/0.17/modules/aws/src/smithy4s/aws/AwsCredentialsProvider.scala AwsCredentialsProvider.default]]
    * @param callback
    *   Function that can be run after each of the put results from the
    *   underlying
    * @param F
    *   [[cats.effect.Async Async]]
    * @param LE
    *   [[kinesis4cats.producer.Producer.LogEncoders Producer.LogEncoders]]
    * @param KLE
    *   [[kinesis4cats.smithy4s.client.KinesisClient.LogEncoders KinesisClient.LogEncoders]]
    * @param SLE
    *   [[kinesis4cats.producer.ShardMapCache.LogEncoders ShardMapCache.LogEncoders]]
    * @return
    *   [[cats.effect.Resource Resource]] of
    *   [[kinesis4cats.smithy4s.client.producer.KinesisProducer KinesisProducer]]
    */
  def apply[F[_]](
      config: FS2Producer.Config[F],
      client: Client[F],
      region: F[AwsRegion],
      loggerF: Async[F] => F[StructuredLogger[F]] = (f: Async[F]) =>
        f.pure(NoOpLogger[F](f)),
      credsF: (
          SimpleHttpClient[F],
          Async[F]
      ) => Resource[F, F[AwsCredentials]] =
        (x: SimpleHttpClient[F], f: Async[F]) =>
          AwsCredentialsProvider.default[F](x)(f),
      callback: (Producer.Res[PutRecordsOutput], Async[F]) => F[Unit] =
        (_: Producer.Res[PutRecordsOutput], f: Async[F]) => f.unit
  )(implicit
      F: Async[F],
      LE: Producer.LogEncoders,
      KLE: KinesisClient.LogEncoders[F],
      SLE: ShardMapCache.LogEncoders
  ): Resource[F, FS2KinesisProducer[F]] = for {
    logger <- loggerF(F).toResource
    underlying <- KinesisProducer(
      config.producerConfig,
      client,
      region,
      loggerF,
      credsF
    )
    channel <- Channel.bounded[F, Record](config.queueSize).toResource
    producer = new FS2KinesisProducer[F](logger, config, channel, underlying)(
      callback
    )
    _ <- producer.resource
  } yield producer
}
