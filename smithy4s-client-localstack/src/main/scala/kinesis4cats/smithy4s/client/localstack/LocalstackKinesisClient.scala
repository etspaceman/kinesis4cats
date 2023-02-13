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
package localstack

import cats.effect.Async
import cats.effect.kernel.Resource
import cats.effect.syntax.all._
import cats.syntax.all._
import org.http4s.client.Client
import org.typelevel.log4cats.StructuredLogger
import org.typelevel.log4cats.noop.NoOpLogger
import smithy4s.aws._
import smithy4s.aws.kernel.AwsRegion

import kinesis4cats.localstack.LocalstackConfig
import kinesis4cats.logging.LogEncoder
import kinesis4cats.smithy4s.client.KinesisClient
import kinesis4cats.smithy4s.client.middleware._

/** Like KinesisClient, but also includes the
  * [[kinesis4cats.smithy4s.client.middleware.LocalstackProxy LocalstackProxy]]
  * middleware, and leverages mock AWS credentials
  */
object LocalstackKinesisClient {

  def localstackHttp4sClient[F[_]](
      client: Client[F],
      config: LocalstackConfig,
      loggerF: Async[F] => F[StructuredLogger[F]]
  )(implicit
      F: Async[F],
      LE: KinesisClient.LogEncoders[F],
      LELC: LogEncoder[LocalstackConfig]
  ): F[Client[F]] =
    loggerF(F).map(logger => LocalstackProxy[F](config, logger)(client))

  /** Creates a [[cats.effect.Resource Resource]] of a KinesisClient that is
    * compatible with Localstack
    *
    * @param client
    *   [[https://http4s.org/v0.23/docs/client.html Client]]
    * @param region
    *   [[https://github.com/disneystreaming/smithy4s/blob/series/0.17/modules/aws-kernel/src/smithy4s/aws/AwsRegion.scala AwsRegion]]
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
  def clientResource[F[_]](
      client: Client[F],
      region: F[AwsRegion],
      config: LocalstackConfig,
      loggerF: Async[F] => F[StructuredLogger[F]]
  )(implicit
      F: Async[F],
      LE: KinesisClient.LogEncoders[F],
      LELC: LogEncoder[LocalstackConfig]
  ): Resource[F, KinesisClient[F]] = for {
    http4sClient <- localstackHttp4sClient(client, config, loggerF).toResource
    awsClient <- KinesisClient[F](
      http4sClient,
      region,
      loggerF,
      (_: SimpleHttpClient[F], f: Async[F]) =>
        Resource.pure(
          f.pure(AwsCredentials.Default("mock-key-id", "mock-secret-key", None))
        )
    )
  } yield awsClient

  /** Creates a [[cats.effect.Resource Resource]] of a KinesisClient that is
    * compatible with Localstack
    *
    * @param client
    *   [[https://http4s.org/v0.23/docs/client.html Client]]
    * @param region
    *   [[https://github.com/disneystreaming/smithy4s/blob/series/0.17/modules/aws-kernel/src/smithy4s/aws/AwsRegion.scala AwsRegion]].
    * @param prefix
    *   Optional string prefix to apply when loading configuration. Default to
    *   None
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
  def clientResource[F[_]](
      client: Client[F],
      region: F[AwsRegion],
      prefix: Option[String] = None,
      loggerF: Async[F] => F[StructuredLogger[F]] = (f: Async[F]) =>
        f.pure(NoOpLogger[F](f))
  )(implicit
      F: Async[F],
      LE: KinesisClient.LogEncoders[F],
      LELC: LogEncoder[LocalstackConfig]
  ): Resource[F, KinesisClient[F]] = LocalstackConfig
    .resource(prefix)
    .flatMap(clientResource(client, region, _, loggerF))
}
