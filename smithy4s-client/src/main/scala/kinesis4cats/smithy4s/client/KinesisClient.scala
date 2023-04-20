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
package smithy4s.client

import _root_.smithy4s.aws._
import _root_.smithy4s.aws.http4s._
import cats.Show
import cats.effect.{Async, Resource}
import cats.syntax.all._
import com.amazonaws.kinesis._
import org.http4s.client.Client
import org.http4s.{Request, Response}
import org.typelevel.log4cats.StructuredLogger
import org.typelevel.log4cats.noop.NoOpLogger

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
  * [[https://docs.aws.amazon.com/kinesis/latest/APIReference/API_SubscribeToShard.html SubscribeToShard]]
  * is not a currently supported operation.
  */
object KinesisClient {

  final case class Builder[F[_]] private (
      client: Client[F],
      region: AwsRegion,
      logger: StructuredLogger[F],
      credentialsResourceF: SimpleHttpClient[F] => Resource[F, F[
        AwsCredentials
      ]],
      encoders: LogEncoders[F],
      logRequestsResponses: Boolean
  )(implicit F: Async[F]) {

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
    def withLogEncoders(encoders: LogEncoders[F]): Builder[F] =
      copy(encoders = encoders)
    def withLogRequestsResponses(logRequestsResponses: Boolean): Builder[F] = 
      copy(logRequestsResponses = logRequestsResponses)
    def enableLogging: Builder[F] = withLogRequestsResponses(true)
    def disableLogging: Builder[F] = withLogRequestsResponses(false)

    def build: Resource[F, KinesisClient[F]] = {
      val clnt =
        if (logRequestsResponses)
          RequestResponseLogger(logger, encoders)(client)
        else client
      val backend =
        AwsHttp4sBackend[F](RequestResponseLogger(logger, encoders)(clnt))
      for {
        credentials <- credentialsResourceF(backend)
        environment = AwsEnvironment.make[F](
          backend,
          F.pure(region),
          credentials,
          F.realTime.map(_.toSeconds).map(Timestamp(_, 0))
        )
        awsClient <- AwsClient.simple(Kinesis.service, environment)
      } yield awsClient
    }
  }

  object Builder {
    def default[F[_]](client: Client[F], region: AwsRegion)(implicit
        F: Async[F]
    ): Builder[F] =
      Builder[F](
        client,
        region,
        NoOpLogger[F],
        backend => AwsCredentialsProvider.default(backend),
        LogEncoders.show[F],
        true
      )

    @annotation.unused
    private def unapply[F[_]](builder: Builder[F]): Unit = ()
  }

  /** Helper class containing required
    * [[kinesis4cats.logging.LogEncoder LogEncoders]] for the
    * [[kinesis4cats.smithy4s.client.KinesisClient KinesisClient]]
    */
  final class LogEncoders[F[_]](implicit
      val http4sRequestLogEncoder: LogEncoder[Request[F]],
      val http4sResponseLogEncoder: LogEncoder[Response[F]]
  )

  object LogEncoders {
    def show[F[_]]: LogEncoders[F] = {
      import kinesis4cats.logging.instances.show._

      implicit val requestShow: Show[Request[F]] = x =>
        ShowBuilder("Request")
          .add("method", x.method)
          .add("headers", x.headers)
          .add("httpVersion", x.httpVersion)
          .add("uri", x.uri)
          .build

      implicit val responseShow: Show[Response[F]] = x =>
        ShowBuilder("Response")
          .add("headers", x.headers)
          .add("httpVersion", x.httpVersion)
          .add("status", x.status)
          .build

      new KinesisClient.LogEncoders[F]()
    }
  }
}
