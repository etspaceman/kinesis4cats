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

package kinesis4cats.client
package localstack

import scala.concurrent.duration._

import cats.effect.std.Dispatcher
import cats.effect.syntax.all._
import cats.effect.{Async, Resource}
import cats.syntax.all._
import org.typelevel.log4cats.slf4j.Slf4jLogger

import kinesis4cats.localstack.LocalstackConfig
import kinesis4cats.localstack.aws.v2.AwsClients

object LocalstackKinesisClient {

  /** Builds a [[kinesis4cats.client.KinesisClient KinesisClient]] that is
    * compliant for Localstack usage. Lifecycle is managed as a
    * [[cats.effect.Resource Resource]].
    *
    * @param config
    *   [[kinesis4cats.localstack.LocalstackConfig LocalstackConfig]]
    * @param F
    *   F with an [[cats.effect.Async Async]] instance
    * @param encoders
    *   [[kinesis4cats.client.KinesisClient.LogEncoders LogEncoders]]
    * @return
    *   [[cats.effect.Resource Resource]] of
    *   [[kinesis4cats.client.KinesisClient KinesisClient]]
    */
  def clientResource[F[_]](
      config: LocalstackConfig,
      encoders: KinesisClient.LogEncoders
  )(implicit
      F: Async[F]
  ): Resource[F, KinesisClient[F]] = for {
    underlying <- AwsClients.kinesisClient(config).toResource
    logger <- Slf4jLogger.create[F].toResource
    dispatcher <- Dispatcher.parallel[F]
  } yield new KinesisClient(underlying, logger, dispatcher, encoders)

  /** Builds a [[kinesis4cats.client.KinesisClient KinesisClient]] that is
    * compliant for Localstack usage. Lifecycle is managed as a
    * [[cats.effect.Resource Resource]].
    *
    * @param prefix
    *   Optional prefix for parsing configuration. Default to None
    * @param encoders
    *   [[kinesis4cats.client.KinesisClient.LogEncoders KinesisClient.LogEncoders]]
    * @param F
    *   F with an [[cats.effect.Async Async]] instance
    * @return
    *   [[cats.effect.Resource Resource]] of
    *   [[kinesis4cats.client.KinesisClient KinesisClient]]
    */
  def clientResource[F[_]](
      prefix: Option[String] = None,
      encoders: KinesisClient.LogEncoders = KinesisClient.LogEncoders.show
  )(implicit
      F: Async[F]
  ): Resource[F, KinesisClient[F]] =
    LocalstackConfig.resource(prefix).flatMap(clientResource[F](_, encoders))

  /** A resources that does the following:
    *
    *   - Builds a [[kinesis4cats.client.KinesisClient KinesisClient]] that is
    *     compliant for Localstack usage.
    *   - Creates a stream with the desired name and shard count, and waits
    *     until the stream is active.
    *   - Destroys the stream when the [[cats.effect.Resource Resource]] is
    *     closed
    *
    * @param config
    *   [[kinesis4cats.localstack.LocalstackConfig LocalstackConfig]]
    * @param streamName
    *   Stream name
    * @param shardCount
    *   Shard count for stream
    * @param describeRetries
    *   How many times to retry DescribeStreamSummary when checking the stream
    *   status
    * @param describeRetryDuration
    *   How long to delay between retries of the DescribeStreamSummary call
    * @param F
    *   F with an [[cats.effect.Async Async]] instance
    * @param LE
    *   [[kinesis4cats.client.KinesisClient.LogEncoders LogEncoders]]
    * @return
    *   [[cats.effect.Resource Resource]] of
    *   [[kinesis4cats.client.KinesisClient KinesisClient]]
    */
  def streamResource[F[_]](
      config: LocalstackConfig,
      streamName: String,
      shardCount: Int,
      describeRetries: Int,
      describeRetryDuration: FiniteDuration,
      encoders: KinesisClient.LogEncoders
  )(implicit
      F: Async[F]
  ): Resource[F, KinesisClient[F]] = for {
    client <- clientResource(config, encoders)
    result <- Resource.make(
      AwsClients
        .createStream(
          client.client,
          streamName,
          shardCount,
          describeRetries,
          describeRetryDuration
        )
        .as(client)
    )(client =>
      AwsClients
        .deleteStream(
          client.client,
          streamName,
          describeRetries,
          describeRetryDuration
        )
    )
  } yield result

  /** A resources that does the following:
    *
    *   - Builds a [[kinesis4cats.client.KinesisClient KinesisClient]] that is
    *     compliant for Localstack usage.
    *   - Creates a stream with the desired name and shard count, and waits
    *     until the stream is active.
    *   - Destroys the stream when the [[cats.effect.Resource Resource]] is
    *     closed
    *
    * @param streamName
    *   Stream name
    * @param shardCount
    *   Shard count for stream
    * @param prefix
    *   Optional prefix for parsing configuration. Default to None
    * @param describeRetries
    *   How many times to retry DescribeStreamSummary when checking the stream
    *   status. Default to 5
    * @param describeRetryDuration
    *   How long to delay between retries of the DescribeStreamSummary call.
    *   Default to 500 ms
    * @param F
    *   F with an [[cats.effect.Async Async]] instance
    * @param encoders
    *   [[kinesis4cats.client.KinesisClient.LogEncoders LogEncoders]]. Default
    *   to show instances
    * @return
    *   [[cats.effect.Resource Resource]] of
    *   [[kinesis4cats.client.KinesisClient KinesisClient]]
    */
  def streamResource[F[_]](
      streamName: String,
      shardCount: Int,
      prefix: Option[String] = None,
      describeRetries: Int = 5,
      describeRetryDuration: FiniteDuration = 500.millis,
      encoders: KinesisClient.LogEncoders = KinesisClient.LogEncoders.show
  )(implicit
      F: Async[F]
  ): Resource[F, KinesisClient[F]] = for {
    config <- LocalstackConfig.resource(prefix)
    result <- streamResource(
      config,
      streamName,
      shardCount,
      describeRetries,
      describeRetryDuration,
      encoders
    )
  } yield result
}
