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

package kinesis4cats.kcl
package fs2
package localstack

import cats.Parallel
import cats.effect.{Async, Resource}
import software.amazon.kinesis.processor.StreamTracker
import software.amazon.kinesis.retrieval.polling.PollingConfig

import kinesis4cats.kcl.localstack.LocalstackKCLConsumer
import kinesis4cats.localstack.LocalstackConfig
import kinesis4cats.localstack.aws.v2.AwsClients

/** Helpers for constructing and leveraging the KCL with Localstack via FS2.
  */
object LocalstackKCLConsumerFS2 {

  final case class Builder[F[_]] private (
      kclBuilder: KCLConsumerFS2.Builder[F]
  )(implicit F: Async[F]) {
    def configure(f: KCLConsumerFS2.Builder[F] => KCLConsumerFS2.Builder[F]) =
      copy(
        kclBuilder = f(kclBuilder)
      )

    def build: Resource[F, KCLConsumerFS2[F]] = kclBuilder.build
  }

  object Builder {
    def default[F[_]](
        localstackConfig: LocalstackConfig,
        streamTracker: StreamTracker,
        appName: String
    )(implicit F: Async[F], P: Parallel[F]): Resource[F, Builder[F]] = for {
      kinesisClient <- AwsClients.kinesisClientResource(localstackConfig)
      cloudWatchClient <- AwsClients.cloudwatchClientResource(localstackConfig)
      dynamoClient <- AwsClients.dynamoClientResource(localstackConfig)
      retrievalConfig =
        if (streamTracker.isMultiStream()) new PollingConfig(kinesisClient)
        else
          new PollingConfig(
            streamTracker.streamConfigList.get(0).streamIdentifier.streamName,
            kinesisClient
          )
      initial = KCLConsumerFS2.Builder
        .default(
          streamTracker,
          appName
        )
        .withKinesisClient(kinesisClient)
        .withDynamoClient(dynamoClient)
        .withCloudWatchClient(cloudWatchClient)
        .configure(
          _.configureLeaseManagementConfig(
            LocalstackKCLConsumer.configureTopLevelLeaseManagementConfig
          )
            .configureLeaseManagementConfig(x =>
              LocalstackKCLConsumer.configureLeaseManagementFactory(
                x,
                streamTracker.isMultiStream()
              )
            )
            .configureCoordinatorConfig(_.parentShardPollIntervalMillis(1000L))
            .configureRetrievalConfig(
              _.retrievalSpecificConfig(retrievalConfig)
                .retrievalFactory(retrievalConfig.retrievalFactory())
            )
        )
      kclBuilder =
        if (streamTracker.isMultiStream()) initial
        else
          initial.configure(
            _.configureLeaseManagementConfig(
              _.initialPositionInStream(
                streamTracker.streamConfigList
                  .get(0)
                  .initialPositionInStreamExtended()
              )
            )
          )
    } yield Builder(kclBuilder)

    def default[F[_]](
        streamTracker: StreamTracker,
        appName: String,
        prefix: Option[String] = None
    )(implicit F: Async[F], P: Parallel[F]): Resource[F, Builder[F]] =
      LocalstackConfig
        .resource[F](prefix)
        .flatMap(default(_, streamTracker, appName))

    @annotation.unused
    def unapply[F[_]](builder: Builder[F]): Unit = ()
  }
}
