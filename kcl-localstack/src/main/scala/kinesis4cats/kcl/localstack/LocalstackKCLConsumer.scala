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
package localstack

import cats.effect.std.Queue
import cats.effect.syntax.all._
import cats.effect.{Async, Deferred, Resource}
import cats.syntax.all._
import software.amazon.kinesis.processor.StreamTracker
import software.amazon.kinesis.retrieval.polling.PollingConfig

import kinesis4cats.localstack.LocalstackConfig
import kinesis4cats.localstack.aws.v2.AwsClients

/** Helpers for constructing and leveraging the KPL with Localstack.
  */
object LocalstackKCLConsumer {

  final case class ConfigWithResults[F[_]](
      kclConfig: KCLConsumer.Config[F],
      resultsQueue: Queue[F, CommittableRecord[F]]
  )

  final case class DeferredWithResults[F[_]](
      deferred: Deferred[F, Unit],
      resultsQueue: Queue[F, CommittableRecord[F]]
  )

  final case class Builder[F[_]] private (
      kclBuilder: KCLConsumer.Builder[F]
  )(implicit F: Async[F]) {
    def configure(
        f: KCLConsumer.Builder[F] => KCLConsumer.Builder[F]
    ): Builder[F] = copy(
      kclBuilder = f(kclBuilder)
    )
    def withCallback(f: List[CommittableRecord[F]] => F[Unit]): Builder[F] =
      copy(kclBuilder = kclBuilder.withCallback(f))
    def build: Resource[F, KCLConsumer[F]] = kclBuilder.build
    def run: Resource[F, Deferred[F, Unit]] =
      build.flatMap(_.runWithDeferredListener())
    def runWithResults(
        resultsQueueSize: Int = 50
    ): Resource[F, DeferredWithResults[F]] = for {
      resultsQueue <- Queue
        .bounded[F, CommittableRecord[F]](resultsQueueSize)
        .toResource
      consumer <- kclBuilder
        .configure(x =>
          x.withCallback((recs: List[CommittableRecord[F]]) =>
            resultsQueue.tryOfferN(recs) >> x.callback(recs)
          )
        )
        .build
      deferred <- consumer.runWithDeferredListener()
    } yield DeferredWithResults(deferred, resultsQueue)
  }

  object Builder {
    def default[F[_]](
        localstackConfig: LocalstackConfig,
        streamTracker: StreamTracker,
        appName: String
    )(implicit F: Async[F]): Resource[F, Builder[F]] = for {
      kinesisClient <- AwsClients.kinesisClientResource(localstackConfig)
      cloudWatchClient <- AwsClients.cloudwatchClientResource(localstackConfig)
      dynamoClient <- AwsClients.dynamoClientResource(localstackConfig)
      default = KCLConsumer.Builder
        .default(
          streamTracker,
          appName
        )
        .withKinesisClient(kinesisClient)
        .withDynamoClient(dynamoClient)
        .withCloudWatchClient(cloudWatchClient)
      retrievalConfig =
        if (streamTracker.isMultiStream()) new PollingConfig(kinesisClient)
        else
          new PollingConfig(
            streamTracker.streamConfigList.get(0).streamIdentifier.streamName,
            kinesisClient
          )
      initial = default
        .configure(x =>
          x.configureLeaseManagementConfig(_.shardSyncIntervalMillis(1000L))
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
    )(implicit F: Async[F]): Resource[F, Builder[F]] =
      LocalstackConfig
        .resource[F](prefix)
        .flatMap(default(_, streamTracker, appName))

    @annotation.unused
    def unapply[F[_]](builder: Builder[F]): Unit = ()
  }
}
