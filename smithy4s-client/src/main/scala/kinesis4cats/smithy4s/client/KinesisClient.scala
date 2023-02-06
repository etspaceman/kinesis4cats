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

import cats.effect.syntax.all._
import cats.effect.{Async, Resource}
import cats.syntax.all._
import com.amazonaws.kinesis._
import org.http4s.client.Client
import org.http4s.{Request, Response}
import org.typelevel.log4cats.slf4j.Slf4jLogger
import smithy4s.aws._
import smithy4s.aws.http4s._

import kinesis4cats.logging._
import kinesis4cats.smithy4s.client.middleware.RequestResponseLogger

object KinesisClient {

  /** Helper class containing required
    * [[kinesis4cats.logging.LogEncoder LogEncoders]] for the
    * [[kinesis4cats.smithy4s.client.KinesisClient KinesisClient]]
    */
  final class LogEncoders[F[_]](implicit
      val http4sRequestLogEncoder: LogEncoder[Request[F]],
      val http4sResponseLogEncoder: LogEncoder[Response[F]]
  )

  def apply[F[_]](
      client: Client[F],
      region: AwsRegion,
      creds: F[AwsCredentials]
  )(implicit
      F: Async[F],
      LE: LogEncoders[F]
  ): Resource[F, KinesisClient[F]] = for {
    logger <- Slf4jLogger.create[F].toResource
    env = AwsEnvironment
      .make(
        AwsHttp4sBackend(RequestResponseLogger(logger)(client)),
        F.pure(region),
        creds,
        F.realTime.map(_.toSeconds).map(Timestamp(_, 0))
      )
    awsClient <- AwsClient(Kinesis.service, env)
  } yield awsClient

}
