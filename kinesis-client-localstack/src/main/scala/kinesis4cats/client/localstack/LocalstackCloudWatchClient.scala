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

import cats.effect.{Async, Resource}
import cats.syntax.all._
import org.typelevel.log4cats.StructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient

import kinesis4cats.localstack.LocalstackConfig
import kinesis4cats.localstack.aws.v2.AwsClients

object LocalstackCloudWatchClient {

  final case class Builder[F[_]] private (
      encoders: CloudWatchClient.LogEncoders,
      logger: StructuredLogger[F],
      clientResource: Resource[F, CloudWatchAsyncClient]
  )(implicit F: Async[F]) {
    def withClient(
        client: => CloudWatchAsyncClient,
        managed: Boolean = true
    ): Builder[F] = copy(
      clientResource =
        if (managed) Resource.fromAutoCloseable(F.delay(client))
        else Resource.pure(client)
    )

    def withLogEncoders(encoders: CloudWatchClient.LogEncoders): Builder[F] =
      copy(
        encoders = encoders
      )

    def withLogger(logger: StructuredLogger[F]): Builder[F] = copy(
      logger = logger
    )

    def build: Resource[F, CloudWatchClient[F]] = for {
      underlying <- clientResource
      client <- CloudWatchClient.Builder
        .default[F]
        .withClient(underlying, managed = false)
        .withLogEncoders(encoders)
        .withLogger(logger)
        .build
    } yield client
  }

  object Builder {
    def default[F[_]](
        prefix: Option[String] = None
    )(implicit
        F: Async[F]
    ): F[Builder[F]] = LocalstackConfig.load(prefix).map(default(_))

    def default[F[_]](
        localstackConfig: LocalstackConfig
    )(implicit
        F: Async[F]
    ): Builder[F] =
      Builder[F](
        CloudWatchClient.LogEncoders.show,
        Slf4jLogger.getLogger,
        AwsClients.cloudwatchClientResource[F](localstackConfig)
      )

    @annotation.unused
    private def unapply[F[_]](builder: Builder[F]): Unit = ()
  }
}
