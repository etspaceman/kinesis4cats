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

import kinesis4cats.models
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
    override protected val channel: Channel[F, (Record, Deferred[F, F[Unit]])],
    override protected val underlying: KinesisProducer[F]
)(
    override protected val callback: Producer.Result[PutRecordsOutput] => F[
      Unit
    ]
)(implicit
    F: Async[F]
) extends FS2Producer[F, PutRecordsInput, PutRecordsOutput]

object FS2KinesisProducer {

  final case class Builder[F[_]] private (
      config: FS2Producer.Config[F],
      client: Client[F],
      region: AwsRegion,
      logger: StructuredLogger[F],
      credentialsResourceF: SimpleHttpClient[F] => Resource[F, F[
        AwsCredentials
      ]],
      encoders: KinesisProducer.LogEncoders[F],
      logRequestsResponses: Boolean,
      callback: Producer.Result[PutRecordsOutput] => F[Unit],
      underlyingConfig: Producer.Config[F]
  )(implicit F: Async[F]) {
    def withConfig(config: FS2Producer.Config[F]): Builder[F] =
      copy(config = config)
    def withClient(client: Client[F]): Builder[F] = copy(client = client)
    def withRegion(region: AwsRegion): Builder[F] = copy(region = region)
    def withLogger(logger: StructuredLogger[F]): Builder[F] =
      copy(logger = logger)
    def withCredentials(
        credentialsResourceF: SimpleHttpClient[F] => Resource[F, F[
          AwsCredentials
        ]]
    ): Builder[F] =
      copy(credentialsResourceF = credentialsResourceF)
    def withLogEncoders(encoders: KinesisProducer.LogEncoders[F]): Builder[F] =
      copy(encoders = encoders)
    def withLogRequestsResponses(logRequestsResponses: Boolean): Builder[F] =
      copy(logRequestsResponses = logRequestsResponses)
    def withUnderlyingConfig(underlyingConfig: Producer.Config[F]): Builder[F] =
      copy(underlyingConfig = underlyingConfig)
    def withCallback(
        callback: Producer.Result[PutRecordsOutput] => F[Unit]
    ): Builder[F] =
      copy(callback = callback)
    def enableLogging: Builder[F] = withLogRequestsResponses(true)
    def disableLogging: Builder[F] = withLogRequestsResponses(false)

    def build: Resource[F, FS2KinesisProducer[F]] = for {
      underlying <- KinesisProducer.Builder
        .default[F](config.producerConfig.streamNameOrArn, client, region)
        .withLogger(logger)
        .withConfig(underlyingConfig)
        .withCredentials(credentialsResourceF)
        .withLogEncoders(encoders)
        .withLogRequestsResponses(logRequestsResponses)
        .build
      channel <- Channel
        .bounded[F, (Record, Deferred[F, F[Unit]])](config.queueSize)
        .toResource
      producer = new FS2KinesisProducer[F](logger, config, channel, underlying)(
        callback
      )
      _ <- producer.resource
    } yield producer
  }

  object Builder {
    def default[F[_]](
        streamNameOrArn: models.StreamNameOrArn,
        client: Client[F],
        region: AwsRegion
    )(implicit F: Async[F]): Builder[F] = Builder[F](
      FS2Producer.Config.default(streamNameOrArn),
      client,
      region,
      NoOpLogger[F],
      backend => AwsCredentialsProvider.default(backend),
      KinesisProducer.LogEncoders.show[F],
      true,
      (_: Producer.Result[PutRecordsOutput]) => F.unit,
      Producer.Config.default(streamNameOrArn)
    )

    @annotation.unused
    private def unapply[F[_]](builder: Builder[F]): Unit = ()
  }
}
