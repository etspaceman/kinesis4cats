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

package kinesis4cats.localstack.kcl

import java.util.UUID

import cats.effect.std.Queue
import cats.effect.syntax.all._
import cats.effect.{Async, Deferred, Resource}
import cats.syntax.all._
import software.amazon.kinesis.checkpoint.CheckpointConfig
import software.amazon.kinesis.common.{
  InitialPositionInStream,
  InitialPositionInStreamExtended
}
import software.amazon.kinesis.coordinator.CoordinatorConfig
import software.amazon.kinesis.leases.LeaseManagementConfig
import software.amazon.kinesis.lifecycle.LifecycleConfig
import software.amazon.kinesis.metrics.MetricsConfig
import software.amazon.kinesis.retrieval.RetrievalConfig
import software.amazon.kinesis.retrieval.polling.PollingConfig

import kinesis4cats.kcl._
import kinesis4cats.kcl.processor.RecordProcessorLogEncoders
import kinesis4cats.localstack.LocalstackConfig
import kinesis4cats.localstack.aws.v2.AwsClients

/** Helpers for constructing and leveraging the KPL with Localstack.
  */
object LocalstackKCLConsumer {

  final case class ConfigWithResults[F[_]](
      kclConfig: KCLConsumerConfig[F],
      resultsQueue: Queue[F, CommittableRecord[F]]
  )

  final case class DeferredWithResults[F[_]](
      deferred: Deferred[F, Unit],
      resultsQueue: Queue[F, CommittableRecord[F]]
  )

  /** Creates a [[kinesis4cats.kcl.KCLConsumerConfig KCLConsumerConfig]] that is
    * compliant with Localstack.
    *
    * @param config
    *   [[kinesis4cats.localstack.LocalstackConfig LocalstackConfig]]
    * @param streamName
    *   Name of stream to consume
    * @param appName
    *   Application name for the consumer. Used for the dynamodb table name as
    *   well as the metrics namespace.
    * @param workerId
    *   Unique identifier for the worker. Typically a UUID.
    * @param position
    *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/common/InitialPositionInStreamExtended.java InitialPositionInStreamExtended]]
    * @param cb
    *   User-defined callback function for processing records
    * @param F
    *   [[cats.effect.Async Async]]
    * @param LE
    *   [[kinesis4cats.kcl.processor.RecordProcessorLogEncoders RecordProcessorLogEncoders]]
    * @return
    *   [[kinesis4cats.kcl.KCLConsumerConfig KCLConsumerConfig]]
    */
  def kclConfig[F[_]](
      config: LocalstackConfig,
      streamName: String,
      appName: String,
      workerId: String,
      position: InitialPositionInStreamExtended
  )(cb: List[CommittableRecord[F]] => F[Unit])(implicit
      F: Async[F],
      LE: RecordProcessorLogEncoders
  ): Resource[F, KCLConsumerConfig[F]] = for {
    kinesisClient <- AwsClients.kinesisClientResource(config)
    cloudwatchClient <- AwsClients.cloudwatchClientResource(config)
    dynamoClient <- AwsClients.dynamoClientResource(config)
    retrievalConfig = new PollingConfig(streamName, kinesisClient)
    result <- KCLConsumerConfig.create[F](
      new CheckpointConfig(),
      new CoordinatorConfig(appName).parentShardPollIntervalMillis(1000L),
      new LeaseManagementConfig(
        appName,
        dynamoClient,
        kinesisClient,
        workerId
      ).initialPositionInStream(position)
        .shardSyncIntervalMillis(1000L),
      new LifecycleConfig(),
      new MetricsConfig(cloudwatchClient, appName),
      new RetrievalConfig(kinesisClient, streamName, appName)
        .retrievalSpecificConfig(retrievalConfig)
        .retrievalFactory(retrievalConfig.retrievalFactory())
    )(cb)
  } yield result

  /** Creates a [[kinesis4cats.kcl.KCLConsumerConfig KCLConsumerConfig]] that is
    * compliant with Localstack.
    *
    * @param streamName
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
    * @param cb
    *   User-defined callback function for processing records
    * @param F
    *   [[cats.effect.Async Async]]
    * @param LE
    *   [[kinesis4cats.kcl.processor.RecordProcessorLogEncoders RecordProcessorLogEncoders]]
    * @return
    *   [[kinesis4cats.kcl.KCLConsumerConfig KCLConsumerConfig]]
    */
  def kclConfig[F[_]](
      streamName: String,
      appName: String,
      prefix: Option[String] = None,
      workerId: String = UUID.randomUUID().toString,
      position: InitialPositionInStreamExtended =
        InitialPositionInStreamExtended.newInitialPosition(
          InitialPositionInStream.TRIM_HORIZON
        )
  )(cb: List[CommittableRecord[F]] => F[Unit])(implicit
      F: Async[F],
      LE: RecordProcessorLogEncoders
  ): Resource[F, KCLConsumerConfig[F]] = for {
    config <- LocalstackConfig.resource(prefix)
    result <- kclConfig(config, streamName, appName, workerId, position)(cb)
  } yield result

  /** Creates a [[kinesis4cats.kcl.KCLConsumerConfig KCLConsumerConfig]] that is
    * compliant with Localstack. Also creates a results
    * [[cats.effect.std.Queue queue]] for the consumer to stick results into.
    * Helpful when confirming data that has been produced to a stream.
    *
    * @param config
    *   [[kinesis4cats.localstack.LocalstackConfig LocalstackConfig]]
    * @param streamName
    *   Name of stream to consume
    * @param appName
    *   Application name for the consumer. Used for the dynamodb table name as
    *   well as the metrics namespace.
    * @param workerId
    *   Unique identifier for the worker. Typically a UUID.
    * @param position
    *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/common/InitialPositionInStreamExtended.java InitialPositionInStreamExtended]]
    * @param resultsQueueSize
    *   Bounded size of the [[cats.effect.std.Queue Queue]]
    * @param cb
    *   User-defined callback function for processing records. This will run
    *   after the records are enqueued into the results queue
    * @param F
    *   [[cats.effect.Async Async]]
    * @param LE
    *   [[kinesis4cats.kcl.processor.RecordProcessorLogEncoders RecordProcessorLogEncoders]]
    * @return
    *   [[kinesis4cats.localstack.kcl.LocalstackKCLConsumer.ConfigWithResults ConfigWithResults]]
    */
  def kclConfigWithResults[F[_]](
      config: LocalstackConfig,
      streamName: String,
      appName: String,
      workerId: String,
      position: InitialPositionInStreamExtended,
      resultsQueueSize: Int
  )(cb: List[CommittableRecord[F]] => F[Unit])(implicit
      F: Async[F],
      LE: RecordProcessorLogEncoders
  ): Resource[F, ConfigWithResults[F]] = for {
    resultsQueue <- Queue
      .bounded[F, CommittableRecord[F]](resultsQueueSize)
      .toResource
    kclConf <- kclConfig(config, streamName, appName, workerId, position)(
      (recs: List[CommittableRecord[F]]) =>
        resultsQueue.tryOfferN(recs) >> cb(recs)
    )
  } yield ConfigWithResults(kclConf, resultsQueue)

  /** Creates a [[kinesis4cats.kcl.KCLConsumerConfig KCLConsumerConfig]] that is
    * compliant with Localstack. Also creates a results
    * [[cats.effect.std.Queue queue]] for the consumer to stick results into.
    * Helpful when confirming data that has been produced to a stream.
    *
    * @param streamName
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
    * @param resultsQueueSize
    *   Bounded size of the [[cats.effect.std.Queue Queue]]. Default to 50.
    * @param cb
    *   User-defined callback function for processing records. This will run
    *   after the records are enqueued into the results queue
    * @param F
    *   [[cats.effect.Async Async]]
    * @param LE
    *   [[kinesis4cats.kcl.processor.RecordProcessorLogEncoders RecordProcessorLogEncoders]]
    * @return
    *   [[kinesis4cats.localstack.kcl.LocalstackKCLConsumer.ConfigWithResults ConfigWithResults]]
    */
  def kclConfigWithResults[F[_]](
      streamName: String,
      appName: String,
      prefix: Option[String] = None,
      workerId: String = UUID.randomUUID().toString(),
      position: InitialPositionInStreamExtended =
        InitialPositionInStreamExtended.newInitialPosition(
          InitialPositionInStream.TRIM_HORIZON
        ),
      resultsQueueSize: Int = 50
  )(cb: List[CommittableRecord[F]] => F[Unit])(implicit
      F: Async[F],
      LE: RecordProcessorLogEncoders
  ): Resource[F, ConfigWithResults[F]] = for {
    config <- LocalstackConfig.resource(prefix)
    result <- kclConfigWithResults(
      config,
      streamName,
      appName,
      workerId,
      position,
      resultsQueueSize
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
    * @param streamName
    *   Name of stream to consume
    * @param appName
    *   Application name for the consumer. Used for the dynamodb table name as
    *   well as the metrics namespace.
    * @param workerId
    *   Unique identifier for the worker. Typically a UUID.
    * @param position
    *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/common/InitialPositionInStreamExtended.java InitialPositionInStreamExtended]]
    * @param cb
    *   User-defined callback function for processing records
    * @param F
    *   [[cats.effect.Async Async]]
    * @param LE
    *   [[kinesis4cats.kcl.processor.RecordProcessorLogEncoders RecordProcessorLogEncoders]]
    * @return
    *   [[cats.effect.Deferred Deferred]] in a
    *   [[cats.effect.Resource Resource]], which completes when the consumer has
    *   started processing records
    */
  def kclConsumer[F[_]](
      config: LocalstackConfig,
      streamName: String,
      appName: String,
      workerId: String,
      position: InitialPositionInStreamExtended
  )(cb: List[CommittableRecord[F]] => F[Unit])(implicit
      F: Async[F],
      LE: RecordProcessorLogEncoders
  ): Resource[F, Deferred[F, Unit]] = for {
    config <- kclConfig(config, streamName, appName, workerId, position)(cb)
    deferred <- KCLConsumer.runWithDeferredListener(config)
  } yield deferred

  /** Runs a [[kinesis4cats.kcl.KCLConsumer KCLConsumer]] that is compliant with
    * Localstack. Also exposes a [[cats.effect.Deferred Deferred]] that will
    * complete when the consumer has started processing records. Useful for
    * allowing tests time for the consumer to start before processing the
    * stream.
    *
    * @param streamName
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
    * @param cb
    *   User-defined callback function for processing records
    * @param F
    *   [[cats.effect.Async Async]]
    * @param LE
    *   [[kinesis4cats.kcl.processor.RecordProcessorLogEncoders RecordProcessorLogEncoders]]
    * @return
    *   [[cats.effect.Deferred Deferred]] in a
    *   [[cats.effect.Resource Resource]], which completes when the consumer has
    *   started processing records
    */
  def kclConsumer[F[_]](
      streamName: String,
      appName: String,
      prefix: Option[String] = None,
      workerId: String = UUID.randomUUID().toString(),
      position: InitialPositionInStreamExtended =
        InitialPositionInStreamExtended.newInitialPosition(
          InitialPositionInStream.TRIM_HORIZON
        )
  )(cb: List[CommittableRecord[F]] => F[Unit])(implicit
      F: Async[F],
      LE: RecordProcessorLogEncoders
  ): Resource[F, Deferred[F, Unit]] = for {
    config <- LocalstackConfig.resource(prefix)
    result <- kclConsumer(config, streamName, appName, workerId, position)(cb)
  } yield result

  /** Runs a [[kinesis4cats.kcl.KCLConsumer KCLConsumer]] that is compliant with
    * Localstack. Exposes a [[cats.effect.Deferred Deferred]] that will complete
    * when the consumer has started processing records, as well as a
    * [[cats.effect.std.Queue Queue]] for tracking the received records. Useful for
    * allowing tests time for the consumer to start before processing the
    * stream, and testing those records that have been received.
    *
    * @param config
    *   [[kinesis4cats.localstack.LocalstackConfig LocalstackConfig]]
    * @param streamName
    *   Name of stream to consume
    * @param appName
    *   Application name for the consumer. Used for the dynamodb table name as
    *   well as the metrics namespace.
    * @param workerId
    *   Unique identifier for the worker. Typically a UUID.
    * @param position
    *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/common/InitialPositionInStreamExtended.java InitialPositionInStreamExtended]]
    * @param resultsQueueSize
    *   Bounded size of the [[cats.effect.std.Queue Queue]].
    * @param cb
    *   User-defined callback function for processing records
    * @param F
    *   [[cats.effect.Async Async]]
    * @param LE
    *   [[kinesis4cats.kcl.processor.RecordProcessorLogEncoders RecordProcessorLogEncoders]]
    * @return
    *   [[kinesis4cats.localstack.kcl.LocalstackKCLConsumer.DeferredWithResults DeferredWithResults]]
    *   in a [[cats.effect.Resource Resource]], which completes when the
    *   consumer has started processing records
    */
  def kclConsumerWithResults[F[_]](
      config: LocalstackConfig,
      streamName: String,
      appName: String,
      workerId: String,
      position: InitialPositionInStreamExtended,
      resultsQueueSize: Int
  )(cb: List[CommittableRecord[F]] => F[Unit])(implicit
      F: Async[F],
      LE: RecordProcessorLogEncoders
  ): Resource[F, DeferredWithResults[F]] = for {
    configWithResults <- kclConfigWithResults(
      config,
      streamName,
      appName,
      workerId,
      position,
      resultsQueueSize
    )(cb)
    deferred <- KCLConsumer.runWithDeferredListener(configWithResults.kclConfig)
  } yield DeferredWithResults(deferred, configWithResults.resultsQueue)

  /** Runs a [[kinesis4cats.kcl.KCLConsumer KCLConsumer]] that is compliant with
    * Localstack. Exposes a [[cats.effect.Deferred Deferred]] that will complete
    * when the consumer has started processing records, as well as a
    * [[cats.effect.std.Queue Queue]] for tracking the received records. Useful for
    * allowing tests time for the consumer to start before processing the
    * stream, and testing those records that have been received.
    *
    * @param streamName
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
    * @param resultsQueueSize
    *   Bounded size of the [[cats.effect.std.Queue Queue]]. Default to 50.
    * @param cb
    *   User-defined callback function for processing records
    * @param F
    *   [[cats.effect.Async Async]]
    * @param LE
    *   [[kinesis4cats.kcl.processor.RecordProcessorLogEncoders RecordProcessorLogEncoders]]
    * @return
    *   [[kinesis4cats.localstack.kcl.LocalstackKCLConsumer.DeferredWithResults DeferredWithResults]]
    *   in a [[cats.effect.Resource Resource]], which completes when the
    *   consumer has started processing records
    */
  def kclConsumerWithResults[F[_]](
      streamName: String,
      appName: String,
      prefix: Option[String] = None,
      workerId: String = UUID.randomUUID().toString(),
      position: InitialPositionInStreamExtended =
        InitialPositionInStreamExtended.newInitialPosition(
          InitialPositionInStream.TRIM_HORIZON
        ),
      resultsQueueSize: Int = 50
  )(cb: List[CommittableRecord[F]] => F[Unit])(implicit
      F: Async[F],
      LE: RecordProcessorLogEncoders
  ): Resource[F, DeferredWithResults[F]] = for {
    configWithResults <- kclConfigWithResults(
      streamName,
      appName,
      prefix,
      workerId,
      position,
      resultsQueueSize
    )(cb)
    deferred <- KCLConsumer.runWithDeferredListener(configWithResults.kclConfig)
  } yield DeferredWithResults(deferred, configWithResults.resultsQueue)

}
