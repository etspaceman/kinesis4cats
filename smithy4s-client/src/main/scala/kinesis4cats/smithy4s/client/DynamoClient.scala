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
import com.amazonaws.dynamodb._
import org.http4s.client.Client
import org.typelevel.log4cats.StructuredLogger
import org.typelevel.log4cats.noop.NoOpLogger
import smithy4s.aws._
import smithy4s.aws.http4s._

import kinesis4cats.smithy4s.client.logging.LogEncoders
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
  * [[https://docs.aws.amazon.com/kinesis/latest/APIReference/API_SubscribeToShard.html SubscribeToShard]]
  * is not a currently supported operation.
  */
object DynamoClient {

  def awsEnv[F[_]](
      client: Client[F],
      region: F[AwsRegion],
      credsF: (
          SimpleHttpClient[F],
          Async[F]
      ) => Resource[F, F[AwsCredentials]] =
        (x: SimpleHttpClient[F], f: Async[F]) =>
          AwsCredentialsProvider.default[F](x)(f),
      backendF: (Client[F], Async[F]) => SimpleHttpClient[F] =
        (client: Client[F], f: Async[F]) => AwsHttp4sBackend[F](client)(f)
  )(implicit F: Async[F]) = {
    val backend = backendF(client, F)
    credsF(backend, F).map(creds =>
      AwsEnvironment
        .make[F](
          backend,
          region,
          creds,
          F.realTime.map(_.toSeconds).map(Timestamp(_, 0))
        )
    )
  }

  /** Create a DynamoClient [[cats.effect.Resource Resource]]
    *
    * @param client
    *   [[https://http4s.org/v0.23/docs/client.html Client]] implementation for
    *   the api calls
    * @param region
    *   [[https://github.com/disneystreaming/smithy4s/blob/series/0.17/modules/aws-kernel/src/smithy4s/aws/AwsRegion.scala AwsRegion]]
    *   in use
    * @param loggerF
    *   [[cats.effect.Async Async]] => [[cats.effect.Async Async]] of
    *   [[https://github.com/typelevel/log4cats/blob/main/core/shared/src/main/scala/org/typelevel/log4cats/StructuredLogger.scala StructuredLogger]].
    *   Default is
    *   [[https://github.com/typelevel/log4cats/blob/main/noop/shared/src/main/scala/org/typelevel/log4cats/noop/NoOpLogger.scala NoOpLogger]]
    * @param credsF
    *   [[https://github.com/disneystreaming/smithy4s/blob/series/0.17/modules/aws/src/smithy4s/aws/SimpleHttpClient.scala SimpleHttpClient]]
    *   \=>
    *   [[https://github.com/disneystreaming/smithy4s/blob/series/0.17/modules/aws-kernel/src/smithy4s/aws/AwsCredentials.scala AwsCredentials]].
    *   Default to
    *   [[https://github.com/disneystreaming/smithy4s/blob/series/0.17/modules/aws/src/smithy4s/aws/AwsCredentialsProvider.scala AwsCredentialsProvider.default]]
    * @param F
    *   [[cats.effect.Async Async]]
    * @param LE
    *   [[kinesis4cats.smithy4s.client.logging.LogEncoders LogEncoders]]
    * @return
    *   [[cats.effect.Resource Resource]] of a Kinesis Client.
    */
  def apply[F[_]](
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
      backendF: (Client[F], Async[F]) => SimpleHttpClient[F] =
        (client: Client[F], f: Async[F]) => AwsHttp4sBackend[F](client)(f)
  )(implicit
      F: Async[F],
      LE: LogEncoders[F]
  ): Resource[F, DynamoClient[F]] = for {
    logger <- loggerF(F).toResource
    env <- awsEnv(
      RequestResponseLogger(logger)(client),
      region,
      credsF,
      backendF
    )
    awsClient <- AwsClient.simple(DynamoDB.service, env)
  } yield awsClient
}
