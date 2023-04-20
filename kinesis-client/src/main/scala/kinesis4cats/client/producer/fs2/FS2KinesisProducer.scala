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

package kinesis4cats
package client
package producer
package fs2

import _root_.fs2.concurrent.Channel
import cats.effect._
import cats.effect.syntax.all._
import org.typelevel.log4cats.StructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient
import software.amazon.awssdk.services.kinesis.model.PutRecordsRequest
import software.amazon.awssdk.services.kinesis.model.PutRecordsResponse

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
  */
final class FS2KinesisProducer[F[_]] private[kinesis4cats] (
    override val logger: StructuredLogger[F],
    override val config: FS2Producer.Config[F],
    override protected val channel: Channel[F, Record],
    override protected val underlying: KinesisProducer[F]
)(
    override protected val callback: Producer.Res[PutRecordsResponse] => F[Unit]
)(implicit
    F: Async[F]
) extends FS2Producer[F, PutRecordsRequest, PutRecordsResponse]

object FS2KinesisProducer {
  final case class Builder[F[_]] private (
      config: FS2Producer.Config[F],
      clientResource: Resource[F, KinesisClient[F]],
      encoders: KinesisProducer.LogEncoders,
      logger: StructuredLogger[F],
      callback: Producer.Res[PutRecordsResponse] => F[Unit]
  )(implicit F: Async[F]) {
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
    def withLogEncoders(encoders: KinesisProducer.LogEncoders): Builder[F] =
      copy(encoders = encoders)
    def withLogger(logger: StructuredLogger[F]): Builder[F] =
      copy(logger = logger)

    def build: Resource[F, FS2KinesisProducer[F]] = for {
      client <- clientResource
      underlying <- KinesisProducer.Builder
        .default[F](config.producerConfig.streamNameOrArn)
        .withConfig(config.producerConfig)
        .withLogEncoders(encoders)
        .withLogger(logger)
        .withClient(client)
        .build
      channel <- Channel.bounded[F, Record](config.queueSize).toResource
      producer = new FS2KinesisProducer[F](logger, config, channel, underlying)(
        callback
      )
      _ <- producer.resource
    } yield producer
  }

  object Builder {
    def default[F[_]](
        streamNameOrArn: models.StreamNameOrArn
    )(implicit F: Async[F]): Builder[F] = Builder[F](
      FS2Producer.Config.default(streamNameOrArn),
      KinesisClient.Builder.default.build,
      KinesisProducer.LogEncoders.show,
      Slf4jLogger.getLogger,
      (_: Producer.Res[PutRecordsResponse]) => F.unit
    )

    @annotation.unused
    private def unapply[F[_]](builder: Builder[F]): Unit = ()
  }
}
