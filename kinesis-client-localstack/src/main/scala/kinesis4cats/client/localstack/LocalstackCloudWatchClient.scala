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

import cats.effect.syntax.all._
import cats.effect.{Async, Resource}
import cats.syntax.all._
import org.typelevel.log4cats.slf4j.Slf4jLogger

import kinesis4cats.localstack.LocalstackConfig
import kinesis4cats.localstack.aws.v2.AwsClients

object LocalstackCloudWatchClient {

  /** Builds a [[kinesis4cats.client.CloudWatchClient CloudWatchClient]] that is
    * compliant for Localstack usage.
    *
    * @param config
    *   [[kinesis4cats.localstack.LocalstackConfig LocalstackConfig]]
    * @param F
    *   F with an [[cats.effect.Async Async]] instance
    * @param encoders
    *   [[kinesis4cats.client.CloudWatchClient.LogEncoders LogEncoders]]
    * @return
    *   F of [[kinesis4cats.client.CloudWatchClient CloudWatchClient]]
    */
  def client[F[_]](
      config: LocalstackConfig,
      encoders: CloudWatchClient.LogEncoders
  )(implicit
      F: Async[F]
  ): F[CloudWatchClient[F]] =
    for {
      underlying <- AwsClients.cloudwatchClient(config)
      logger <- Slf4jLogger.create[F]
    } yield new CloudWatchClient(underlying, logger, encoders)

  /** Builds a [[kinesis4cats.client.CloudWatchClient CloudWatchClient]] that is
    * compliant for Localstack usage.
    *
    * @param prefix
    *   Optional prefix for parsing configuration. Default to None
    * @param F
    *   F with an [[cats.effect.Async Async]] instance
    * @param encoders
    *   [[kinesis4cats.client.CloudWatchClient.LogEncoders LogEncoders]]
    * @return
    *   F of [[kinesis4cats.client.CloudWatchClient CloudWatchClient]]
    */
  def client[F[_]](
      prefix: Option[String] = None,
      encoders: CloudWatchClient.LogEncoders = CloudWatchClient.LogEncoders.show
  )(implicit F: Async[F]): F[CloudWatchClient[F]] =
    for {
      underlying <- AwsClients.cloudwatchClient(prefix)
      logger <- Slf4jLogger.create[F]
    } yield new CloudWatchClient(underlying, logger, encoders)

  /** Builds a [[kinesis4cats.client.CloudWatchClient CloudWatchClient]] that is
    * compliant for Localstack usage. Lifecycle is managed as a
    * [[cats.effect.Resource Resource]].
    *
    * @param config
    *   [[kinesis4cats.localstack.LocalstackConfig LocalstackConfig]]
    * @param F
    *   F with an [[cats.effect.Async Async]] instance
    * @param encoders
    *   [[kinesis4cats.client.CloudWatchClient.LogEncoders LogEncoders]]
    * @return
    *   [[cats.effect.Resource Resource]] of
    *   [[kinesis4cats.client.CloudWatchClient CloudWatchClient]]
    */
  def clientResource[F[_]](
      config: LocalstackConfig,
      encoders: CloudWatchClient.LogEncoders
  )(implicit
      F: Async[F]
  ): Resource[F, CloudWatchClient[F]] =
    client[F](config, encoders).toResource

  /** Builds a [[kinesis4cats.client.CloudWatchClient CloudWatchClient]] that is
    * compliant for Localstack usage. Lifecycle is managed as a
    * [[cats.effect.Resource Resource]].
    *
    * @param prefix
    *   Optional prefix for parsing configuration. Default to None
    * @param F
    *   F with an [[cats.effect.Async Async]] instance
    * @return
    *   [[cats.effect.Resource Resource]] of
    *   [[kinesis4cats.client.CloudWatchClient CloudWatchClient]]
    */
  def clientResource[F[_]](
      prefix: Option[String] = None,
      encoders: CloudWatchClient.LogEncoders = CloudWatchClient.LogEncoders.show
  )(implicit F: Async[F]): Resource[F, CloudWatchClient[F]] =
    client[F](prefix, encoders).toResource
}
