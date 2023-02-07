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

/** A thin wrapper around
  * [[https://disneystreaming.github.io/smithy4s/docs/overview/intro/ Smithy4s's]]
  * [[https://disneystreaming.github.io/smithy4s/docs/protocols/aws/aws AWS]]
  * protocol.
  *
  * Per Smithy4s's documentation, this is an experimental module that is not
  * expected to be production ready.
  *
  * Updates to the smithy file(s) in this module are not intended to be
  * backwards compatible.
  *
  * The only APIs that are supported today are ones that support the
  * [[https://awslabs.github.io/smithy/1.0/spec/aws/index.html?highlight=aws%20protocols#aws-protocols aws-json]]
  * 1.0 and 1.1 protocols.
  *
  * [[https://docs.aws.amazon.com/kinesis/latest/APIReference/API_SubscribeToShard.html SubscribeToShard]]
  * is not a currently supported operation.
  */
object KinesisClient {

  /** Helper class containing required
    * [[kinesis4cats.logging.LogEncoder LogEncoders]] for the
    * [[kinesis4cats.smithy4s.client.KinesisClient KinesisClient]]
    */
  final class LogEncoders[F[_]](implicit
      val http4sRequestLogEncoder: LogEncoder[Request[F]],
      val http4sResponseLogEncoder: LogEncoder[Response[F]]
  )

  /** Create a KinesisClient [[cats.effect.Resource Resource]]
    *
    * @param client
    *   [[https://http4s.org/v0.23/docs/client.html Client]] implementation for
    *   the api calls
    * @param region
    *   [[https://github.com/disneystreaming/smithy4s/blob/series/0.17/modules/aws-kernel/src/smithy4s/aws/AwsRegion.scala AwsRegion]]
    *   in use
    * @param creds
    *   F provider of [[smithy4s.aws.AwsCredentials AwsCredentials]]
    * @param F
    *   [[cats.effect.Async Async]]
    * @param LE
    *   [[kinesis4cats.smithy4s.client.KinesisClient.LogEncoders KinesisClient.LogEncoders]]
    * @return
    *   [[cats.effect.Resource Resource]] of a Kinesis Client.
    */
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
