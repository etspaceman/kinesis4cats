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
import software.amazon.kinesis.checkpoint.CheckpointConfig
import software.amazon.kinesis.coordinator.CoordinatorConfig
import software.amazon.kinesis.leases.LeaseManagementConfig
import software.amazon.kinesis.lifecycle.LifecycleConfig
import software.amazon.kinesis.metrics.MetricsConfig
import software.amazon.kinesis.processor.StreamTracker
import software.amazon.kinesis.retrieval.RetrievalConfig
import software.amazon.kinesis.retrieval.polling.PollingConfig

import kinesis4cats.Utils
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

  /** Creates a [[kinesis4cats.kcl.KCLConsumer.Config KCLConsumer.Config]] that
    * is compliant with Localstack.
    *
    * @param config
    *   [[kinesis4cats.localstack.LocalstackConfig LocalstackConfig]]
    * @param streamTracker
    *   Name of stream to consume
    * @param appName
    *   Application name for the consumer. Used for the dynamodb table name as
    *   well as the metrics namespace.
    * @param workerId
    *   Unique identifier for the worker. Typically a UUID.
    * @param position
    *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/common/InitialPositionInStreamExtended.java InitialPositionInStreamExtended]]
    * @param processConfig
    *   [[kinesis4cats.kcl.KCLConsumer.ProcessConfig KCLConsumer.ProcessConfig]]
    * @param cb
    *   User-defined callback function for processing records
    * @param F
    *   [[cats.effect.Async Async]]
    * @param encoders
    *   [[kinesis4cats.kcl.RecordProcessor.LogEncoders RecordProcessor.LogEncoders]]
    * @return
    *   [[kinesis4cats.kcl.KCLConsumer.Config KCLConsumer.Config]]
    */
  def kclConfig[F[_]](
      config: LocalstackConfig,
      streamTracker: StreamTracker,
      appName: String,
      workerId: String,
      processConfig: KCLConsumer.ProcessConfig,
      encoders: RecordProcessor.LogEncoders
  )(
      cb: List[CommittableRecord[F]] => F[Unit]
  )(implicit F: Async[F]): Resource[F, KCLConsumer.Config[F]] = for {
    kinesisClient <- AwsClients.kinesisClientResource(config)
    cloudwatchClient <- AwsClients.cloudwatchClientResource(config)
    dynamoClient <- AwsClients.dynamoClientResource(config)
    initialLeaseManagementConfig = new LeaseManagementConfig(
      appName,
      dynamoClient,
      kinesisClient,
      workerId
    ).shardSyncIntervalMillis(1000L)
    retrievalConfig =
      if (streamTracker.isMultiStream()) new PollingConfig(kinesisClient)
      else
        new PollingConfig(
          streamTracker.streamConfigList.get(0).streamIdentifier.streamName,
          kinesisClient
        )
    result <- KCLConsumer.Config.create[F](
      new CheckpointConfig(),
      new CoordinatorConfig(appName).parentShardPollIntervalMillis(1000L),
      if (streamTracker.isMultiStream()) initialLeaseManagementConfig
      else
        initialLeaseManagementConfig.initialPositionInStream(
          streamTracker.streamConfigList
            .get(0)
            .initialPositionInStreamExtended()
        ),
      new LifecycleConfig(),
      new MetricsConfig(cloudwatchClient, appName),
      new RetrievalConfig(kinesisClient, streamTracker, appName)
        .retrievalSpecificConfig(retrievalConfig)
        .retrievalFactory(retrievalConfig.retrievalFactory()),
      processConfig = processConfig,
      encoders
    )(cb)
  } yield result

  /** Creates a [[kinesis4cats.kcl.KCLConsumer.Config KCLConsumer.Config]] that
    * is compliant with Localstack.
    *
    * @param streamTracker
    *   Name of stream to consume
    * @param appName
    *   Application name for the consumer. Used for the dynamodb table name as
    *   well as the metrics namespace.
    * @param prefix
    *   Optional prefix for parsing configuration. Default to None
    * @param workerId
    *   Unique identifier for the worker. Default is a random UUID
    * @param position
    *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/common/InitialPositionInStreamExtended.java InitialPositionInStreamExtended]]
    *   Default is TRIM_HORIZON
    * @param processConfig
    *   [[kinesis4cats.kcl.KCLConsumer.ProcessConfig KCLConsumer.ProcessConfig]]
    *   Default is `ProcessConfig.default`
    * @param cb
    *   User-defined callback function for processing records
    * @param F
    *   [[cats.effect.Async Async]]
    * @param encoders
    *   [[kinesis4cats.kcl.RecordProcessor.LogEncoders RecordProcessor.LogEncoders]]
    * @return
    *   [[kinesis4cats.kcl.KCLConsumer.Config KCLConsumer.Config]]
    */
  def kclConfig[F[_]](
      streamTracker: StreamTracker,
      appName: String,
      prefix: Option[String] = None,
      workerId: String = Utils.randomUUIDString,
      processConfig: KCLConsumer.ProcessConfig =
        KCLConsumer.ProcessConfig.default,
      encoders: RecordProcessor.LogEncoders = RecordProcessor.LogEncoders.show
  )(cb: List[CommittableRecord[F]] => F[Unit])(implicit
      F: Async[F]
  ): Resource[F, KCLConsumer.Config[F]] = for {
    config <- LocalstackConfig.resource(prefix)
    result <- kclConfig(
      config,
      streamTracker,
      appName,
      workerId,
      processConfig,
      encoders
    )(cb)
  } yield result

  /** Creates a [[kinesis4cats.kcl.KCLConsumer.Config KCLConsumer.Config]] that
    * is compliant with Localstack. Also creates a results
    * [[cats.effect.std.Queue queue]] for the consumer to stick results into.
    * Helpful when confirming data that has been produced to a stream.
    *
    * @param config
    *   [[kinesis4cats.localstack.LocalstackConfig LocalstackConfig]]
    * @param streamTracker
    *   Name of stream to consume
    * @param appName
    *   Application name for the consumer. Used for the dynamodb table name as
    *   well as the metrics namespace.
    * @param workerId
    *   Unique identifier for the worker. Typically a UUID.
    * @param position
    *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/common/InitialPositionInStreamExtended.java InitialPositionInStreamExtended]]
    * @param processConfig
    *   [[kinesis4cats.kcl.KCLConsumer.ProcessConfig KCLConsumer.ProcessConfig]]
    * @param resultsQueueSize
    *   Bounded size of the [[cats.effect.std.Queue Queue]]
    * @param cb
    *   User-defined callback function for processing records. This will run
    *   after the records are enqueued into the results queue
    * @param F
    *   [[cats.effect.Async Async]]
    * @param encoders
    *   [[kinesis4cats.kcl.RecordProcessor.LogEncoders RecordProcessor.LogEncoders]]
    * @return
    *   [[kinesis4cats.kcl.localstack.LocalstackKCLConsumer.ConfigWithResults ConfigWithResults]]
    */
  def kclConfigWithResults[F[_]](
      config: LocalstackConfig,
      streamTracker: StreamTracker,
      appName: String,
      workerId: String,
      processConfig: KCLConsumer.ProcessConfig,
      resultsQueueSize: Int,
      encoders: RecordProcessor.LogEncoders
  )(cb: List[CommittableRecord[F]] => F[Unit])(implicit
      F: Async[F]
  ): Resource[F, ConfigWithResults[F]] = for {
    resultsQueue <- Queue
      .bounded[F, CommittableRecord[F]](resultsQueueSize)
      .toResource
    kclConf <- kclConfig(
      config,
      streamTracker,
      appName,
      workerId,
      processConfig,
      encoders
    )((recs: List[CommittableRecord[F]]) =>
      resultsQueue.tryOfferN(recs) >> cb(recs)
    )
  } yield ConfigWithResults(kclConf, resultsQueue)

  /** Creates a [[kinesis4cats.kcl.KCLConsumer.Config KCLConsumer.Config]] that
    * is compliant with Localstack. Also creates a results
    * [[cats.effect.std.Queue queue]] for the consumer to stick results into.
    * Helpful when confirming data that has been produced to a stream.
    *
    * @param streamTracker
    *   Name of stream to consume
    * @param appName
    *   Application name for the consumer. Used for the dynamodb table name as
    *   well as the metrics namespace.
    * @param prefix
    *   Optional prefix for parsing configuration. Default to None
    * @param workerId
    *   Unique identifier for the worker. Default to random UUID
    * @param position
    *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/common/InitialPositionInStreamExtended.java InitialPositionInStreamExtended]]
    * @param processConfig
    *   [[kinesis4cats.kcl.KCLConsumer.ProcessConfig KCLConsumer.ProcessConfig]]
    *   Default is `ProcessConfig.default`
    * @param resultsQueueSize
    *   Bounded size of the [[cats.effect.std.Queue Queue]]. Default to 50.
    * @param cb
    *   User-defined callback function for processing records. This will run
    *   after the records are enqueued into the results queue
    * @param F
    *   [[cats.effect.Async Async]]
    * @param encoders
    *   [[kinesis4cats.kcl.RecordProcessor.LogEncoders RecordProcessor.LogEncoders]]
    * @return
    *   [[kinesis4cats.kcl.localstack.LocalstackKCLConsumer.ConfigWithResults ConfigWithResults]]
    */
  def kclConfigWithResults[F[_]](
      streamTracker: StreamTracker,
      appName: String,
      prefix: Option[String] = None,
      workerId: String = Utils.randomUUIDString,
      processConfig: KCLConsumer.ProcessConfig =
        KCLConsumer.ProcessConfig.default,
      resultsQueueSize: Int = 50,
      encoders: RecordProcessor.LogEncoders = RecordProcessor.LogEncoders.show
  )(cb: List[CommittableRecord[F]] => F[Unit])(implicit
      F: Async[F]
  ): Resource[F, ConfigWithResults[F]] = for {
    config <- LocalstackConfig.resource(prefix)
    result <- kclConfigWithResults(
      config,
      streamTracker,
      appName,
      workerId,
      processConfig,
      resultsQueueSize,
      encoders
    )(cb)
  } yield result

  /** Runs a [[kinesis4cats.kcl.KCLConsumer KCLConsumer]] that is compliant with
    * Localstack. Also exposes a [[cats.effect.Deferred Deferred]] that will
    * complete when the consumer has started processing records. Useful for
    * allowing tests time for the consumer to start before processing the
    * stream.
    *
    * @param config
    *   [[kinesis4cats.localstack.LocalstackConfig LocalstackConfig]]
    * @param streamTracker
    *   Name of stream to consume
    * @param appName
    *   Application name for the consumer. Used for the dynamodb table name as
    *   well as the metrics namespace.
    * @param workerId
    *   Unique identifier for the worker. Typically a UUID.
    * @param position
    *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/common/InitialPositionInStreamExtended.java InitialPositionInStreamExtended]]
    * @param processConfig
    *   [[kinesis4cats.kcl.KCLConsumer.ProcessConfig KCLConsumer.ProcessConfig]]
    * @param cb
    *   User-defined callback function for processing records
    * @param F
    *   [[cats.effect.Async Async]]
    * @param encoders
    *   [[kinesis4cats.kcl.RecordProcessor.LogEncoders RecordProcessor.LogEncoders]]
    * @return
    *   [[cats.effect.Deferred Deferred]] in a
    *   [[cats.effect.Resource Resource]], which completes when the consumer has
    *   started processing records
    */
  def kclConsumer[F[_]](
      config: LocalstackConfig,
      streamTracker: StreamTracker,
      appName: String,
      workerId: String,
      processConfig: KCLConsumer.ProcessConfig,
      encoders: RecordProcessor.LogEncoders
  )(cb: List[CommittableRecord[F]] => F[Unit])(implicit
      F: Async[F]
  ): Resource[F, Deferred[F, Unit]] = for {
    config <- kclConfig(
      config,
      streamTracker,
      appName,
      workerId,
      processConfig,
      encoders
    )(cb)
    consumer = new KCLConsumer(config)
    deferred <- consumer.runWithDeferredListener()
  } yield deferred

  /** Runs a [[kinesis4cats.kcl.KCLConsumer KCLConsumer]] that is compliant with
    * Localstack. Also exposes a [[cats.effect.Deferred Deferred]] that will
    * complete when the consumer has started processing records. Useful for
    * allowing tests time for the consumer to start before processing the
    * stream.
    *
    * @param streamTracker
    *   Name of stream to consume
    * @param appName
    *   Application name for the consumer. Used for the dynamodb table name as
    *   well as the metrics namespace.
    * @param prefix
    *   Optional prefix for parsing configuration. Default to None
    * @param workerId
    *   Unique identifier for the worker. Default to a random UUID.
    * @param position
    *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/common/InitialPositionInStreamExtended.java InitialPositionInStreamExtended]].
    *   Default to TRIM_HORIZON
    * @param processConfig
    *   [[kinesis4cats.kcl.KCLConsumer.ProcessConfig KCLConsumer.ProcessConfig]]
    *   Default is `ProcessConfig.default`
    * @param cb
    *   User-defined callback function for processing records
    * @param F
    *   [[cats.effect.Async Async]]
    * @param encoders
    *   [[kinesis4cats.kcl.RecordProcessor.LogEncoders RecordProcessor.LogEncoders]]
    * @return
    *   [[cats.effect.Deferred Deferred]] in a
    *   [[cats.effect.Resource Resource]], which completes when the consumer has
    *   started processing records
    */
  def kclConsumer[F[_]](
      streamTracker: StreamTracker,
      appName: String,
      prefix: Option[String] = None,
      workerId: String = Utils.randomUUIDString,
      processConfig: KCLConsumer.ProcessConfig =
        KCLConsumer.ProcessConfig.default,
      encoders: RecordProcessor.LogEncoders = RecordProcessor.LogEncoders.show
  )(cb: List[CommittableRecord[F]] => F[Unit])(implicit
      F: Async[F]
  ): Resource[F, Deferred[F, Unit]] = for {
    config <- LocalstackConfig.resource(prefix)
    result <- kclConsumer(
      config,
      streamTracker,
      appName,
      workerId,
      processConfig,
      encoders
    )(cb)
  } yield result

  /** Runs a [[kinesis4cats.kcl.KCLConsumer KCLConsumer]] that is compliant with
    * Localstack. Exposes a [[cats.effect.Deferred Deferred]] that will complete
    * when the consumer has started processing records, as well as a
    * [[cats.effect.std.Queue Queue]] for tracking the received records. Useful
    * for allowing tests time for the consumer to start before processing the
    * stream, and testing those records that have been received.
    *
    * @param config
    *   [[kinesis4cats.localstack.LocalstackConfig LocalstackConfig]]
    * @param streamTracker
    *   Name of stream to consume
    * @param appName
    *   Application name for the consumer. Used for the dynamodb table name as
    *   well as the metrics namespace.
    * @param workerId
    *   Unique identifier for the worker. Typically a UUID.
    * @param position
    *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/common/InitialPositionInStreamExtended.java InitialPositionInStreamExtended]]
    * @param processConfig
    *   [[kinesis4cats.kcl.KCLConsumer.ProcessConfig KCLConsumer.ProcessConfig]]
    * @param resultsQueueSize
    *   Bounded size of the [[cats.effect.std.Queue Queue]].
    * @param cb
    *   User-defined callback function for processing records
    * @param F
    *   [[cats.effect.Async Async]]
    * @param encoders
    *   [[kinesis4cats.kcl.RecordProcessor.LogEncoders RecordProcessor.LogEncoders]]
    * @return
    *   [[kinesis4cats.kcl.localstack.LocalstackKCLConsumer.DeferredWithResults DeferredWithResults]]
    *   in a [[cats.effect.Resource Resource]], which completes when the
    *   consumer has started processing records
    */
  def kclConsumerWithResults[F[_]](
      config: LocalstackConfig,
      streamTracker: StreamTracker,
      appName: String,
      workerId: String,
      processConfig: KCLConsumer.ProcessConfig,
      resultsQueueSize: Int,
      encoders: RecordProcessor.LogEncoders
  )(cb: List[CommittableRecord[F]] => F[Unit])(implicit
      F: Async[F]
  ): Resource[F, DeferredWithResults[F]] = for {
    configWithResults <- kclConfigWithResults(
      config,
      streamTracker,
      appName,
      workerId,
      processConfig,
      resultsQueueSize,
      encoders
    )(cb)
    consumer = new KCLConsumer(configWithResults.kclConfig)
    deferred <- consumer.runWithDeferredListener()
  } yield DeferredWithResults(deferred, configWithResults.resultsQueue)

  /** Runs a [[kinesis4cats.kcl.KCLConsumer KCLConsumer]] that is compliant with
    * Localstack. Exposes a [[cats.effect.Deferred Deferred]] that will complete
    * when the consumer has started processing records, as well as a
    * [[cats.effect.std.Queue Queue]] for tracking the received records. Useful
    * for allowing tests time for the consumer to start before processing the
    * stream, and testing those records that have been received.
    *
    * @param streamTracker
    *   Name of stream to consume
    * @param appName
    *   Application name for the consumer. Used for the dynamodb table name as
    *   well as the metrics namespace.
    * @param prefix
    *   Optional prefix for parsing configuration. Default to None
    * @param workerId
    *   Unique identifier for the worker. Default to a random UUID
    * @param position
    *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/common/InitialPositionInStreamExtended.java InitialPositionInStreamExtended]].
    *   Default to TRIM_HORIZON.
    * @param processConfig
    *   [[kinesis4cats.kcl.KCLConsumer.ProcessConfig KCLConsumer.ProcessConfig]]
    *   Default is `ProcessConfig.default`
    * @param resultsQueueSize
    *   Bounded size of the [[cats.effect.std.Queue Queue]]. Default to 50.
    * @param cb
    *   User-defined callback function for processing records
    * @param F
    *   [[cats.effect.Async Async]]
    * @param encoders
    *   [[kinesis4cats.kcl.RecordProcessor.LogEncoders RecordProcessor.LogEncoders]]
    * @return
    *   [[kinesis4cats.kcl.localstack.LocalstackKCLConsumer.DeferredWithResults DeferredWithResults]]
    *   in a [[cats.effect.Resource Resource]], which completes when the
    *   consumer has started processing records
    */
  def kclConsumerWithResults[F[_]](
      streamTracker: StreamTracker,
      appName: String,
      prefix: Option[String] = None,
      workerId: String = Utils.randomUUIDString,
      processConfig: KCLConsumer.ProcessConfig =
        KCLConsumer.ProcessConfig.default,
      resultsQueueSize: Int = 50,
      encoders: RecordProcessor.LogEncoders = RecordProcessor.LogEncoders.show
  )(cb: List[CommittableRecord[F]] => F[Unit])(implicit
      F: Async[F]
  ): Resource[F, DeferredWithResults[F]] = for {
    configWithResults <- kclConfigWithResults(
      streamTracker,
      appName,
      prefix,
      workerId,
      processConfig,
      resultsQueueSize,
      encoders
    )(cb)
    consumer = new KCLConsumer(configWithResults.kclConfig)
    deferred <- consumer.runWithDeferredListener()
  } yield DeferredWithResults(deferred, configWithResults.resultsQueue)

}
