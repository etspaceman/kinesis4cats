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
import com.amazonaws.kinesis._
import org.http4s.client.Client
import org.typelevel.log4cats.slf4j.Slf4jLogger
import smithy4s.aws._
import smithy4s.aws.http4s._
import smithy4s.aws.kernel.AwsRegion

import kinesis4cats.localstack.LocalstackConfig
import kinesis4cats.logging.LogEncoder
import kinesis4cats.smithy4s.client.KinesisClient
import kinesis4cats.smithy4s.client.middleware._

object LocalstackKinesisClient {
  private def localstackEnv[F[_]](client: Client[F], region: AwsRegion)(implicit
      F: Async[F]
  ) = AwsEnvironment.make[F](
    AwsHttp4sBackend(client),
    F.pure(region),
    F.pure(AwsCredentials.Default("mock-key-id", "mock-secret-key", None)),
    F.realTime.map(_.toSeconds).map(Timestamp(_, 0))
  )

  def clientResource[F[_]](
      client: Client[F],
      region: AwsRegion,
      config: LocalstackConfig
  )(implicit
      F: Async[F],
      LE: KinesisClient.LogEncoders[F],
      LELC: LogEncoder[LocalstackConfig]
  ): Resource[F, KinesisClient[F]] = for {
    logger <- Slf4jLogger.create[F].toResource
    clnt = LocalstackProxy[F](config, logger)(
      RequestResponseLogger(logger)(client)
    )
    env = localstackEnv(clnt, region)
    awsClient <- AwsClient(Kinesis.service, env)
  } yield awsClient

  def clientResource[F[_]](
      client: Client[F],
      region: AwsRegion,
      prefix: Option[String] = None
  )(implicit
      F: Async[F],
      LE: KinesisClient.LogEncoders[F],
      LELC: LogEncoder[LocalstackConfig]
  ): Resource[F, KinesisClient[F]] = for {
    logger <- Slf4jLogger.create[F].toResource
    clnt = LocalstackProxy[F](logger, prefix)(
      RequestResponseLogger(logger)(client)
    )
    env = localstackEnv(clnt, region)
    awsClient <- AwsClient(Kinesis.service, env)
  } yield awsClient
}
