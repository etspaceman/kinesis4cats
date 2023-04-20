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
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient

import kinesis4cats.localstack.LocalstackConfig
import kinesis4cats.localstack.TestStreamConfig
import kinesis4cats.localstack.aws.v2.AwsClients

object LocalstackKinesisClient {

  final case class Builder[F[_]] private (
      localstackConfig: LocalstackConfig,
      encoders: KinesisClient.LogEncoders,
      logger: StructuredLogger[F],
      streamsToCreate: List[TestStreamConfig[F]],
      clientResource: Resource[F, KinesisAsyncClient]
  )(implicit F: Async[F]) {
    def withClient(
        client: => KinesisAsyncClient,
        managed: Boolean = true
    ): Builder[F] = copy(
      clientResource =
        if (managed) Resource.fromAutoCloseable(F.delay(client))
        else Resource.pure(client)
    )

    def withLocalstackConfig(localstackConfig: LocalstackConfig): Builder[F] =
      copy(localstackConfig = localstackConfig)

    def withLogEncoders(encoders: KinesisClient.LogEncoders): Builder[F] = copy(
      encoders = encoders
    )

    def withLogger(logger: StructuredLogger[F]): Builder[F] = copy(
      logger = logger
    )

    def withStreamsToCreate(
        streamsToCreate: List[TestStreamConfig[F]]
    ): Builder[F] =
      copy(
        streamsToCreate = streamsToCreate
      )

    def build: Resource[F, KinesisClient[F]] = for {
      underlying <- clientResource
      client <- KinesisClient.Builder
        .default[F]
        .withClient(underlying, false)
        .withLogEncoders(encoders)
        .withLogger(logger)
        .build
      _ <- streamsToCreate.traverse_(config =>
        Builder.managedStream(config, client)
      )
    } yield client
  }

  object Builder {
    private def managedStream[F[_]](
        config: TestStreamConfig[F],
        client: KinesisClient[F]
    )(implicit F: Async[F]): Resource[F, KinesisClient[F]] = Resource.make(
      AwsClients.createStream(client.client, config).as(client)
    )(c => AwsClients.deleteStream(c.client, config))

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
        localstackConfig,
        KinesisClient.LogEncoders.show,
        Slf4jLogger.getLogger,
        Nil,
        AwsClients.kinesisClientResource[F](localstackConfig)
      )

    @annotation.unused
    private def unapply[F[_]](builder: Builder[F]): Unit = ()
  }
}
