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
package ciris

import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.jdk.CollectionConverters._

import java.util.Collection
import java.util.concurrent.ExecutorService

import _root_.ciris._
import cats.effect.{Async, Resource}
import com.amazonaws.services.schemaregistry.deserializers.GlueSchemaRegistryDeserializer
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient
import software.amazon.awssdk.services.dynamodb.model.BillingMode
import software.amazon.awssdk.services.dynamodb.model.Tag
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient
import software.amazon.kinesis.checkpoint.CheckpointConfig
import software.amazon.kinesis.common._
import software.amazon.kinesis.coordinator._
import software.amazon.kinesis.leases.LeaseManagementConfig.WorkerUtilizationAwareAssignmentConfig
import software.amazon.kinesis.leases._
import software.amazon.kinesis.leases.dynamodb.TableCreatorCallback
import software.amazon.kinesis.lifecycle._
import software.amazon.kinesis.metrics._
import software.amazon.kinesis.processor.SingleStreamTracker
import software.amazon.kinesis.retrieval.fanout.FanOutConfig
import software.amazon.kinesis.retrieval.polling.PollingConfig
import software.amazon.kinesis.retrieval.{AggregatorUtil, RetrievalConfig}

import kinesis4cats.Utils
import kinesis4cats.ciris.CirisReader
import kinesis4cats.compat.FunctionConverters._
import kinesis4cats.compat.OptionConverters._
import kinesis4cats.instances.ciris._
import kinesis4cats.kcl.instances.ciris._
import kinesis4cats.models
import kinesis4cats.syntax.id._

/** Standard configuration loader of env variables and system properties for the
  * [[https://github.com/awslabs/amazon-kinesis-client KCL]] via
  * [[https://cir.is/ Ciris]].
  */
object KCLCiris {

  /** Reads environment variables and system properties to load a
    * [[kinesis4cats.kcl.KCLConsumer KCLConsumer]]
    *
    * @param kinesisClient
    *   [[https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/kinesis/KinesisAsyncClient.html KinesisAsyncClient]]
    * @param dynamoClient
    *   [[https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/dynamodb/DynamoDbAsyncClient.html DynamoDbAsyncClient]]
    * @param cloudWatchClient
    *   [[https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/cloudwatch/CloudWatchClient.html CloudWatchClient]]
    * @param prefix
    *   Optional prefix to apply to configuration loaders. Default None
    * @param shardPrioritization
    *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/leases/ShardPrioritization.java ShardPrioritization]]
    * @param workerStateChangeListener
    *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/coordinator/WorkerStateChangeListener.java WorkerStateChangeListener]]
    * @param coordinatorFactory
    *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/coordinator/CoordinatorFactory.java CoordinatorFactory]]
    * @param customShardDetectorProvider
    *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/common/StreamConfig.java StreamConfig]]
    *   \=>
    *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/leases/ShardDetector.java ShardDetector]]
    * @param tableCreatorCallback
    *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/leases/dynamodb/TableCreatorCallback.java TableCreatorCallback]]
    * @param hierarchicalShardSyncer
    *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/leases/HierarchicalShardSyncer.java HierarchicalShardSyncer]]
    * @param leaseManagementFactory
    *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/leases/LeaseManagementFactory.java LeaseManagementFactory]]
    * @param leaseExecutorService
    *   [[https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ExecutorService.html ExecutorService]]
    *   for the lease management
    * @param aggregatorUtil
    *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/retrieval/AggregatorUtil.java AggregatorUtil]]
    * @param taskExecutionListener
    *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/lifecycle/TaskExecutionListener.java TaskExecutionListener]]
    * @param metricsFactory
    *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/metrics/MetricsFactory.java MetricsFactory]]
    * @param glueSchemaRegistryDeserializer
    *   [[https://github.com/awslabs/aws-glue-schema-registry/blob/master/serializer-deserializer/src/main/java/com/amazonaws/services/schemaregistry/deserializers/GlueSchemaRegistryDeserializer.java GlueSchemaRegistryDeserializer]]
    * @param cb
    *   Function to process
    *   [[kinesis4cats.kcl.CommittableRecord CommittableRecords]] received from
    *   Kinesis
    * @param F
    *   [[cats.effect.Async Async]] instance
    * @param LE
    *   [[kinesis4cats.kcl.RecordProcessor.LogEncoders RecordProcessor.LogEncoders]]
    *   for encoding structured logs
    * @return
    *   [[cats.effect.Resource Resource]] containing the
    *   [[kinesis4cats.kcl.KCLConsumer KCLConsumer]]
    */
  def consumer[F[_]](
      kinesisClient: => KinesisAsyncClient = KinesisAsyncClient.builder().build,
      dynamoClient: => DynamoDbAsyncClient =
        DynamoDbAsyncClient.builder().build,
      cloudWatchClient: => CloudWatchAsyncClient =
        CloudWatchAsyncClient.builder().build,
      prefix: Option[String] = None,
      shardPrioritization: Option[ShardPrioritization] = None,
      workerStateChangeListener: Option[WorkerStateChangeListener] = None,
      coordinatorFactory: Option[CoordinatorFactory] = None,
      customShardDetectorProvider: Option[StreamConfig => ShardDetector] = None,
      tableCreatorCallback: Option[TableCreatorCallback] = None,
      leaseManagementFactory: Option[LeaseManagementFactory] = None,
      leaseExecutorService: Option[ExecutorService] = None,
      aggregatorUtil: Option[AggregatorUtil] = None,
      taskExecutionListener: Option[TaskExecutionListener] = None,
      metricsFactory: Option[MetricsFactory] = None,
      glueSchemaRegistryDeserializer: Option[GlueSchemaRegistryDeserializer] =
        None,
      encoders: RecordProcessor.LogEncoders = RecordProcessor.LogEncoders.show,
      managedClients: Boolean = true
  )(cb: List[CommittableRecord[F]] => F[Unit])(implicit
      F: Async[F]
  ): Resource[F, KCLConsumer[F]] = for {
    kClient <-
      if (managedClients)
        Resource.fromAutoCloseable(
          F.delay(kinesisClient)
        )
      else Resource.pure[F, KinesisAsyncClient](kinesisClient)
    dClient <-
      if (managedClients) Resource.fromAutoCloseable(F.delay(dynamoClient))
      else Resource.pure[F, DynamoDbAsyncClient](dynamoClient)
    cClient <-
      if (managedClients)
        Resource.fromAutoCloseable(F.delay(cloudWatchClient))
      else Resource.pure[F, CloudWatchAsyncClient](cloudWatchClient)
    consumer <- kclConfig[F](
      kClient,
      dClient,
      cClient,
      prefix,
      shardPrioritization,
      workerStateChangeListener,
      coordinatorFactory,
      customShardDetectorProvider,
      tableCreatorCallback,
      leaseManagementFactory,
      leaseExecutorService,
      aggregatorUtil,
      taskExecutionListener,
      metricsFactory,
      glueSchemaRegistryDeserializer,
      encoders
    )(cb).map(new KCLConsumer[F](_))
  } yield consumer

  /** Reads environment variables and system properties to load a
    * [[kinesis4cats.kcl.KCLConsumer.Config KCLConsumer.Config]]
    *
    * @param kinesisClient
    *   [[https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/kinesis/KinesisAsyncClient.html KinesisAsyncClient]]
    * @param dynamoClient
    *   [[https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/dynamodb/DynamoDbAsyncClient.html DynamoDbAsyncClient]]
    * @param cloudWatchClient
    *   [[https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/cloudwatch/CloudWatchClient.html CloudWatchClient]]
    * @param prefix
    *   Optional prefix to apply to configuration loaders. Default None
    * @param shardPrioritization
    *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/leases/ShardPrioritization.java ShardPrioritization]]
    * @param workerStateChangeListener
    *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/coordinator/WorkerStateChangeListener.java WorkerStateChangeListener]]
    * @param coordinatorFactory
    *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/coordinator/CoordinatorFactory.java CoordinatorFactory]]
    * @param customShardDetectorProvider
    *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/common/StreamConfig.java StreamConfig]]
    *   \=>
    *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/leases/ShardDetector.java ShardDetector]]
    * @param tableCreatorCallback
    *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/leases/dynamodb/TableCreatorCallback.java TableCreatorCallback]]
    * @param hierarchicalShardSyncer
    *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/leases/HierarchicalShardSyncer.java HierarchicalShardSyncer]]
    * @param leaseManagementFactory
    *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/leases/LeaseManagementFactory.java LeaseManagementFactory]]
    * @param leaseExecutorService
    *   [[https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ExecutorService.html ExecutorService]]
    *   for the lease management
    * @param aggregatorUtil
    *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/retrieval/AggregatorUtil.java AggregatorUtil]]
    * @param taskExecutionListener
    *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/lifecycle/TaskExecutionListener.java TaskExecutionListener]]
    * @param metricsFactory
    *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/metrics/MetricsFactory.java MetricsFactory]]
    * @param glueSchemaRegistryDeserializer
    *   [[https://github.com/awslabs/aws-glue-schema-registry/blob/master/serializer-deserializer/src/main/java/com/amazonaws/services/schemaregistry/deserializers/GlueSchemaRegistryDeserializer.java GlueSchemaRegistryDeserializer]]
    * @param cb
    *   Function to process
    *   [[kinesis4cats.kcl.CommittableRecord CommittableRecords]] received from
    *   Kinesis
    * @param F
    *   [[cats.effect.Async Async]] instance
    * @param encoders
    *   [[kinesis4cats.kcl.RecordProcessor.LogEncoders RecordProcessor.LogEncoders]]
    *   for encoding structured logs
    * @return
    *   [[cats.effect.Resource Resource]] containing the
    *   [[kinesis4cats.kcl.KCLConsumer KCLConsumer]]
    */
  private[kinesis4cats] def kclConfig[F[_]](
      kinesisClient: KinesisAsyncClient,
      dynamoClient: DynamoDbAsyncClient,
      cloudwatchClient: CloudWatchAsyncClient,
      prefix: Option[String],
      shardPrioritization: Option[ShardPrioritization],
      workerStateChangeListener: Option[WorkerStateChangeListener],
      coordinatorFactory: Option[CoordinatorFactory],
      customShardDetectorProvider: Option[StreamConfig => ShardDetector],
      tableCreatorCallback: Option[TableCreatorCallback],
      leaseManagementFactory: Option[LeaseManagementFactory],
      leaseExecutorService: Option[ExecutorService],
      aggregatorUtil: Option[AggregatorUtil],
      taskExecutionListener: Option[TaskExecutionListener],
      metricsFactory: Option[MetricsFactory],
      glueSchemaRegistryDeserializer: Option[GlueSchemaRegistryDeserializer],
      encoders: RecordProcessor.LogEncoders
  )(cb: List[CommittableRecord[F]] => F[Unit])(implicit
      F: Async[F]
  ): Resource[F, KCLConsumer.Config[F]] = for {
    checkpointConfig <- Checkpoint.resource[F]
    coordinatorConfig <- Coordinator.resource[F](
      prefix,
      shardPrioritization,
      workerStateChangeListener,
      coordinatorFactory
    )
    leaseManagementConfig <- Lease.resource[F](
      dynamoClient,
      kinesisClient,
      prefix,
      customShardDetectorProvider,
      tableCreatorCallback,
      leaseManagementFactory,
      leaseExecutorService
    )
    lifecycleConfig <- Lifecycle
      .resource[F](prefix, aggregatorUtil, taskExecutionListener)
    metricsConfig <- Metrics
      .resource[F](cloudwatchClient, prefix, metricsFactory)
    retrievalConfig <- Retrieval
      .resource[F](kinesisClient, prefix, glueSchemaRegistryDeserializer)
    processConfig <- Processor.resource[F](prefix)
    config <- KCLConsumer.Config.create[F](
      checkpointConfig,
      coordinatorConfig,
      leaseManagementConfig,
      lifecycleConfig,
      metricsConfig,
      retrievalConfig,
      processConfig,
      encoders
    )(cb)
  } yield config

  object Common {

    /** Reads the KCL's application name
      *
      * @param prefix
      *   Optional prefix to apply to configuration loaders. Default None
      * @return
      *   [[https://cir.is/api/ciris/ConfigDecoder.html ConfigDecoder]] of the
      *   app name string
      */
    private[kinesis4cats] def readAppName(
        prefix: Option[String]
    ): ConfigValue[Effect, String] =
      CirisReader.read[String](List("kcl", "app", "name"), prefix)

    /** Reads the KCL's stream name to consume
      *
      * @param prefix
      *   Optional prefix to apply to configuration loaders. Default None
      * @return
      *   [[https://cir.is/api/ciris/ConfigDecoder.html ConfigDecoder]] of the
      *   stream name string
      */
    private[kinesis4cats] def readStreamName(
        prefix: Option[String]
    ): ConfigValue[Effect, String] =
      CirisReader.read[String](List("kcl", "stream", "name"), prefix)

    /** Reads the initial position to consume from
      *
      * @param prefix
      *   Optional prefix to apply to configuration loaders. Default None
      * @return
      *   [[https://cir.is/api/ciris/ConfigDecoder.html ConfigDecoder]] of the
      *   initial position
      */
    private[kinesis4cats] def readInitialPosition(
        prefix: Option[String]
    ): ConfigValue[Effect, InitialPositionInStreamExtended] =
      CirisReader.readDefaulted[InitialPositionInStreamExtended](
        List("kcl", "initial", "position"),
        InitialPositionInStreamExtended
          .newInitialPosition(InitialPositionInStream.LATEST),
        prefix
      )
  }

  object Checkpoint {

    /** Reads the
      * [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/checkpoint/CheckpointConfig.java CheckpointConfig]]
      *
      * @return
      *   [[https://cir.is/api/ciris/ConfigDecoder.html ConfigDecoder]] of
      *   [[[[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/checkpoint/CheckpointConfig.java CheckpointConfig]]
      */
    private[kinesis4cats] def read: ConfigValue[Effect, CheckpointConfig] =
      ConfigValue.default(new CheckpointConfig())

    /** Reads the
      * [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/checkpoint/CheckpointConfig.java CheckpointConfig]]
      * into a [[cats.effect.Resource Resource]]
      *
      * @param F
      *   [[cats.effect.Async Async]]
      * @return
      *   [[cats.effect.Resource Resource]] of
      *   [[[[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/checkpoint/CheckpointConfig.java CheckpointConfig]]
      */
    private[kinesis4cats] def resource[F[_]](implicit F: Async[F]) =
      read.resource[F]
  }

  object Coordinator {

    /** Reads the
      * [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/coordinator/CoordinatorConfig.java CoordinatorConfig]]
      *
      * @param prefix
      *   Optional prefix to apply to configuration loaders. Default None
      * @param shardPrioritization
      *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/leases/ShardPrioritization.java ShardPrioritization]]
      * @param workerStateChangeListener
      *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/coordinator/WorkerStateChangeListener.java WorkerStateChangeListener]]
      * @param coordinatorFactory
      *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/coordinator/CoordinatorFactory.java CoordinatorFactory]]
      * @return
      *   [[https://cir.is/api/ciris/ConfigDecoder.html ConfigDecoder]] of
      *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/coordinator/CoordinatorConfig.java CoordinatorConfig]]
      */
    private[kinesis4cats] def read(
        prefix: Option[String],
        shardPrioritization: Option[ShardPrioritization],
        workerStateChangeListener: Option[WorkerStateChangeListener],
        coordinatorFactory: Option[CoordinatorFactory]
    ): ConfigValue[Effect, CoordinatorConfig] =
      for {
        appName <- Common.readAppName(prefix)
        maxInitializationAttempts <- CirisReader.readOptional[Int](
          List("kcl", "coordinator", "max", "initialization", "attempts"),
          prefix
        )
        parentShardPollInterval <- CirisReader
          .readOptional[Duration](
            List("kcl", "coordinator", "parent", "shard", "poll", "interval"),
            prefix
          )
          .map(_.map(_.toMillis))
        skipShardSyncAtWorkerInitializationIfLeasesExist <- CirisReader
          .readOptional[Boolean](
            List(
              "kcl",
              "coordinator",
              "skip",
              "shard",
              "sync",
              "at",
              "initialization",
              "if",
              "leases",
              "exist"
            ),
            prefix
          )
        shardConsumerDispatchPollInterval <- CirisReader
          .readOptional[Duration](
            List(
              "kcl",
              "coordinator",
              "shard",
              "consumer",
              "dispatch",
              "poll",
              "interval"
            ),
            prefix
          )
          .map(_.map(_.toMillis))
        schedulerInitializationBackoffTime <- CirisReader
          .readOptional[Duration](
            List(
              "kcl",
              "coordinator",
              "scheduler",
              "initialization",
              "backoff",
              "time"
            ),
            prefix
          )
          .map(_.map(_.toMillis))
      } yield new CoordinatorConfig(appName)
        .maybeTransform(maxInitializationAttempts)(
          _.maxInitializationAttempts(_)
        )
        .maybeTransform(parentShardPollInterval)(
          _.parentShardPollIntervalMillis(_)
        )
        .maybeTransform(skipShardSyncAtWorkerInitializationIfLeasesExist)(
          _.skipShardSyncAtWorkerInitializationIfLeasesExist(_)
        )
        .maybeTransform(shardConsumerDispatchPollInterval)(
          _.shardConsumerDispatchPollIntervalMillis(_)
        )
        .maybeTransform(schedulerInitializationBackoffTime)(
          _.schedulerInitializationBackoffTimeMillis(_)
        )
        .maybeTransform(shardPrioritization)(_.shardPrioritization(_))
        .maybeTransform(workerStateChangeListener)(
          _.workerStateChangeListener(_)
        )
        .maybeTransform(coordinatorFactory)(_.coordinatorFactory(_))

    /** Reads the
      * [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/coordinator/CoordinatorConfig.java CoordinatorConfig]]
      * into a [[cats.effect.Resource Resource]]
      *
      * @param prefix
      *   Optional prefix to apply to configuration loaders. Default None
      * @param shardPrioritization
      *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/leases/ShardPrioritization.java ShardPrioritization]]
      * @param workerStateChangeListener
      *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/coordinator/WorkerStateChangeListener.java WorkerStateChangeListener]]
      * @param coordinatorFactory
      *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/coordinator/CoordinatorFactory.java CoordinatorFactory]]
      * @param F
      *   [[cats.effect.Async Async]]
      * @return
      *   [[cats.effect.Resource Resource]] of
      *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/coordinator/CoordinatorConfig.java CoordinatorConfig]]
      */
    private[kinesis4cats] def resource[F[_]](
        prefix: Option[String],
        shardPrioritization: Option[ShardPrioritization],
        workerStateChangeListener: Option[WorkerStateChangeListener],
        coordinatorFactory: Option[CoordinatorFactory]
    )(implicit F: Async[F]): Resource[F, CoordinatorConfig] = read(
      prefix,
      shardPrioritization,
      workerStateChangeListener,
      coordinatorFactory
    ).resource[F]
  }

  object Lease {

    /** Reads the
      * [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/leases/LeaseManagementConfig.java LeaseManagementConfig]]
      *
      * @param dynamoClient
      *   [[https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/dynamodb/DynamoDbAsyncClient.html DynamoDbAsyncClient]]
      * @param kinesisClient
      *   [[https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/kinesis/KinesisAsyncClient.html KinesisAsyncClient]]
      * @param prefix
      *   Optional prefix to apply to configuration loaders. Default None
      * @param customShardDetectorProvider
      *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/common/StreamConfig.java StreamConfig]]
      *   \=>
      *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/leases/ShardDetector.java ShardDetector]]
      * @param tableCreatorCallback
      *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/leases/dynamodb/TableCreatorCallback.java TableCreatorCallback]]
      * @param hierarchicalShardSyncer
      *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/leases/HierarchicalShardSyncer.java HierarchicalShardSyncer]]
      * @param leaseManagementFactory
      *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/leases/LeaseManagementFactory.java LeaseManagementFactory]]
      * @param leaseExecutorService
      *   [[https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ExecutorService.html ExecutorService]]
      *   for the lease management
      * @return
      *   [[https://cir.is/api/ciris/ConfigDecoder.html ConfigDecoder]] of
      *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/leases/LeaseManagementConfig.java LeaseManagementConfig]]
      */
    private[kinesis4cats] def read(
        dynamoClient: DynamoDbAsyncClient,
        kinesisClient: KinesisAsyncClient,
        prefix: Option[String],
        customShardDetectorProvider: Option[StreamConfig => ShardDetector],
        tableCreatorCallback: Option[TableCreatorCallback],
        leaseManagementFactory: Option[LeaseManagementFactory],
        executorService: Option[ExecutorService]
    ): ConfigValue[Effect, LeaseManagementConfig] = for {
      appName <- Common.readAppName(prefix)
      tableName <- CirisReader
        .read[String](List("kcl", "lease", "table", "name"), prefix)
        .default(appName)
      workerId <- CirisReader.readDefaulted(
        List("kcl", "lease", "worker", "id"),
        Utils.randomUUIDString,
        prefix
      )
      failoverTime <- CirisReader
        .readOptional[Duration](
          List("kcl", "lease", "failover", "time"),
          prefix
        )
        .map(_.map(_.toMillis))
      shardSyncInterval <- CirisReader
        .readOptional[Duration](
          List("kcl", "lease", "shard", "sync", "interval"),
          prefix
        )
        .map(_.map(_.toMillis))
      cleanupLeasesUponShardCompletion <- CirisReader.readOptional[Boolean](
        List(
          "kcl",
          "lease",
          "cleanup",
          "leases",
          "upon",
          "shard",
          "completion"
        ),
        prefix
      )
      maxLeasesForWorker <- CirisReader.readOptional[Int](
        List("kcl", "lease", "max", "leases", "for", "worker"),
        prefix
      )
      maxLeasesToStealAtOneTime <- CirisReader.readOptional[Int](
        List(
          "kcl",
          "lease",
          "max",
          "leases",
          "to",
          "steal",
          "at",
          "one",
          "time"
        ),
        prefix
      )
      initialLeaseTableReadCapacity <- CirisReader.readOptional[Int](
        List("kcl", "lease", "initial", "lease", "table", "read", "capacity"),
        prefix
      )
      initialLeaseTableWriteCapacity <- CirisReader.readOptional[Int](
        List("kcl", "lease", "initial", "lease", "table", "write", "capacity"),
        prefix
      )
      maxLeaseRenewalThreads <- CirisReader.readOptional[Int](
        List("kcl", "lease", "max", "lease", "renewal", "threads"),
        prefix
      )
      ignoreUnexpectedChildShards <- CirisReader.readOptional[Boolean](
        List("kcl", "lease", "ignore", "unexpected", "child", "shards"),
        prefix
      )
      consistentReads <- CirisReader.readOptional[Boolean](
        List("kcl", "lease", "consistent", "reads"),
        prefix
      )
      listShardsBackoffTime <- CirisReader
        .readOptional[Duration](
          List("kcl", "lease", "list", "shards", "backoff", "time"),
          prefix
        )
        .map(_.map(_.toMillis))
      maxListShardsRetryAttempts <- CirisReader.readOptional[Int](
        List("kcl", "lease", "max", "list", "shards", "retry", "attempts"),
        prefix
      )
      epsilon <- CirisReader
        .readOptional[Duration](
          List("kcl", "lease", "epsilon"),
          prefix
        )
        .map(_.map(_.toMillis))
      dynamoDbRequestTimeout <- CirisReader.readOptional[java.time.Duration](
        List("kcl", "lease", "dynamo", "request", "timeout"),
        prefix
      )
      billingMode <- CirisReader.readOptional[BillingMode](
        List("kcl", "lease", "billing", "mode"),
        prefix
      )
      leasesRecoveryAuditorExecutionFrequency <- CirisReader
        .readOptional[Duration](
          List(
            "kcl",
            "lease",
            "leases",
            "recovery",
            "auditor",
            "execution",
            "frequency"
          ),
          prefix
        )
        .map(_.map(_.toMillis))
      leasesRecoveryAuditorInconsistencyConfidenceThreshold <- CirisReader
        .readOptional[Int](
          List(
            "kcl",
            "lease",
            "leases",
            "recovery",
            "auditor",
            "inconsistency",
            "confidence",
            "threshold"
          ),
          prefix
        )
      initialPositionInStream <- Common.readInitialPosition(prefix)
      maxCacheMissesBeforeReload <- CirisReader.readOptional[Int](
        List("kcl", "lease", "max", "cache", "misses", "before", "reload"),
        prefix
      )
      listShardsCacheAllowedAge <- CirisReader
        .readOptional[Duration](
          List("kcl", "lease", "list", "shards", "cache", "allowed", "age"),
          prefix
        )
        .map(_.map(_.toSeconds))
      cacheMissWarningModulus <- CirisReader.readOptional[Int](
        List("kcl", "lease", "cache", "miss", "warning", "modulus"),
        prefix
      )
      inMemoryWorkerMetricsCaptureFrequencyMillis <- CirisReader
        .readOptional[Duration](
          List(
            "kcl",
            "lease",
            "in",
            "memory",
            "worker",
            "metrics",
            "capture",
            "frequency"
          ),
          prefix
        )
        .map(_.map(_.toMillis))
      workerMetricsReporterFreqInMillis <- CirisReader
        .readOptional[Duration](
          List(
            "kcl",
            "lease",
            "worker",
            "metrics",
            "reporter",
            "freq"
          ),
          prefix
        )
        .map(_.map(_.toMillis))
      noOfPersistedMetricsPerWorkerMetrics <- CirisReader
        .readOptional[Int](
          List(
            "kcl",
            "lease",
            "no",
            "of",
            "persisted",
            "metrics",
            "per",
            "worker",
            "metrics"
          ),
          prefix
        )
      disableWorkerMetrics <- CirisReader
        .readOptional[Boolean](
          List(
            "kcl",
            "lease",
            "disable",
            "worker",
            "metrics"
          ),
          prefix
        )
      maxThroughputPerHostKBps <- CirisReader
        .readOptional[Double](
          List(
            "kcl",
            "lease",
            "max",
            "throughput",
            "per",
            "host",
            "kbps"
          ),
          prefix
        )
      dampeningPercentage <- CirisReader
        .readOptional[Int](
          List(
            "kcl",
            "lease",
            "dampening",
            "percentage"
          ),
          prefix
        )
      reBalanceThresholdPercentage <- CirisReader
        .readOptional[Int](
          List(
            "kcl",
            "lease",
            "rebalance",
            "threshold",
            "percentage"
          ),
          prefix
        )
      allowThroughputOvershoot <- CirisReader
        .readOptional[Boolean](
          List(
            "kcl",
            "lease",
            "allow",
            "throughput",
            "overshoot"
          ),
          prefix
        )
      staleWorkerMetricsEntryCleanupDuration <- CirisReader
        .readOptional[Duration](
          List(
            "kcl",
            "lease",
            "stale",
            "worker",
            "metrics",
            "entry",
            "cleanup",
            "duration"
          ),
          prefix
        )
        .map(_.map(d => java.time.Duration.ofMillis(d.toMillis)))
      varianceBalancingFrequency <- CirisReader
        .readOptional[Int](
          List(
            "kcl",
            "lease",
            "variance",
            "balancing",
            "frequency"
          ),
          prefix
        )
      workerMetricsEMAAlpha <- CirisReader
        .readOptional[Double](
          List(
            "kcl",
            "lease",
            "worker",
            "metrics",
            "ema",
            "alpha"
          ),
          prefix
        )
      workerMetricsTableBillingMode <- CirisReader
        .readOptional[BillingMode](
          List(
            "kcl",
            "lease",
            "worker",
            "metrics",
            "table",
            "billing",
            "mode"
          ),
          prefix
        )
      workerMetricsTableReadCapacity <- CirisReader
        .readOptional[Long](
          List(
            "kcl",
            "lease",
            "worker",
            "metrics",
            "table",
            "read",
            "capacity"
          ),
          prefix
        )
      workerMetricsTableWriteCapacity <- CirisReader
        .readOptional[Long](
          List(
            "kcl",
            "lease",
            "worker",
            "metrics",
            "table",
            "write",
            "capacity"
          ),
          prefix
        )
      workerMetricsTablePointInTimeRecoveryEnabled <- CirisReader
        .readOptional[Boolean](
          List(
            "kcl",
            "lease",
            "worker",
            "metrics",
            "table",
            "point",
            "in",
            "time",
            "recovery",
            "enabled"
          ),
          prefix
        )
      workerMetricsTableDeletionProtectionEnabled <- CirisReader
        .readOptional[Boolean](
          List(
            "kcl",
            "lease",
            "worker",
            "metrics",
            "table",
            "deletion",
            "protection",
            "enabled"
          ),
          prefix
        )
      tags <- CirisReader
        .readOptional[List[Tag]](
          List("kcl", "lease", "table", "tags"),
          prefix
        )
        .map(_.map(_.asJava))
      workerMetricsTags <- CirisReader
        .readOptional[List[Tag]](
          List("kcl", "lease", "worker", "metrics", "table", "tags"),
          prefix
        )
        .map(_.map(_.asJava))
      workerMetricsTableConfig = WorkerMetricsTableConfig
        .default(appName)
        .maybeTransform(workerMetricsTableBillingMode)(
          (x: WorkerMetricsTableConfig, y: BillingMode) =>
            x.copy(billingMode = y)
        )
        .maybeTransform(workerMetricsTableReadCapacity)(
          (x: WorkerMetricsTableConfig, y: Long) => x.copy(readCapacity = y)
        )
        .maybeTransform(workerMetricsTableWriteCapacity)(
          (x: WorkerMetricsTableConfig, y: Long) => x.copy(writeCapacity = y)
        )
        .maybeTransform(workerMetricsTablePointInTimeRecoveryEnabled)(
          (x: WorkerMetricsTableConfig, y: Boolean) =>
            x.copy(pointInTimeRecoveryEnabled = y)
        )
        .maybeTransform(workerMetricsTableDeletionProtectionEnabled)(
          (x: WorkerMetricsTableConfig, y: Boolean) =>
            x.copy(deletionProtectionEnabled = y)
        )
        .maybeTransform(workerMetricsTags)(
          (x: WorkerMetricsTableConfig, y: Collection[Tag]) => x.copy(tags = y)
        )
        .underlying
      workerUtilizationAwareAssignmentConfig =
        new WorkerUtilizationAwareAssignmentConfig()
          .maybeTransform(inMemoryWorkerMetricsCaptureFrequencyMillis)(
            _.inMemoryWorkerMetricsCaptureFrequencyMillis(_)
          )
          .maybeTransform(workerMetricsReporterFreqInMillis)(
            _.workerMetricsReporterFreqInMillis(_)
          )
          .maybeTransform(noOfPersistedMetricsPerWorkerMetrics)(
            _.noOfPersistedMetricsPerWorkerMetrics(_)
          )
          .maybeTransform(disableWorkerMetrics)(_.disableWorkerMetrics(_))
          .maybeTransform(maxThroughputPerHostKBps)(
            _.maxThroughputPerHostKBps(_)
          )
          .maybeTransform(dampeningPercentage)(_.dampeningPercentage(_))
          .maybeTransform(reBalanceThresholdPercentage)(
            _.reBalanceThresholdPercentage(_)
          )
          .maybeTransform(allowThroughputOvershoot)(
            _.allowThroughputOvershoot(_)
          )
          .maybeTransform(staleWorkerMetricsEntryCleanupDuration)(
            _.staleWorkerMetricsEntryCleanupDuration(_)
          )
          .maybeTransform(varianceBalancingFrequency)(
            _.varianceBalancingFrequency(_)
          )
          .maybeTransform(workerMetricsEMAAlpha)(_.workerMetricsEMAAlpha(_))
          .workerMetricsTableConfig(workerMetricsTableConfig)
      leaseTableDeletionProtectionEnabled <- CirisReader
        .readOptional[Boolean](
          List(
            "kcl",
            "lease",
            "table",
            "deletion",
            "protection",
            "enabled"
          ),
          prefix
        )
      leaseTablePitrEnabled <- CirisReader
        .readOptional[Boolean](
          List(
            "kcl",
            "lease",
            "table",
            "pitr",
            "enabled"
          ),
          prefix
        )
      leasesRecoveryAuditorExecutionFrequencyMillis <- CirisReader
        .readOptional[Duration](
          List(
            "kcl",
            "lease",
            "recovery",
            "auditor",
            "execution",
            "frequency"
          ),
          prefix
        )
        .map(_.map(_.toMillis))
    } yield new LeaseManagementConfig(
      tableName,
      appName,
      dynamoClient,
      kinesisClient,
      workerId
    ).initialPositionInStream(initialPositionInStream)
      .maybeTransform(failoverTime)(_.failoverTimeMillis(_))
      .maybeTransform(shardSyncInterval)(_.shardSyncIntervalMillis(_))
      .maybeTransform(cleanupLeasesUponShardCompletion)(
        _.cleanupLeasesUponShardCompletion(_)
      )
      .maybeTransform(maxLeasesForWorker)(_.maxLeasesForWorker(_))
      .maybeTransform(maxLeasesToStealAtOneTime)(_.maxLeasesToStealAtOneTime(_))
      .maybeTransform(initialLeaseTableReadCapacity)(
        _.initialLeaseTableReadCapacity(_)
      )
      .maybeTransform(initialLeaseTableWriteCapacity)(
        _.initialLeaseTableWriteCapacity(_)
      )
      .maybeTransform(maxLeaseRenewalThreads)(_.maxLeaseRenewalThreads(_))
      .maybeTransform(ignoreUnexpectedChildShards)(
        _.ignoreUnexpectedChildShards(_)
      )
      .maybeTransform(consistentReads)(_.consistentReads(_))
      .maybeTransform(listShardsBackoffTime)(_.listShardsBackoffTimeInMillis(_))
      .maybeTransform(maxListShardsRetryAttempts)(
        _.maxListShardsRetryAttempts(_)
      )
      .maybeTransform(epsilon)(_.epsilonMillis(_))
      .maybeTransform(dynamoDbRequestTimeout)(_.dynamoDbRequestTimeout(_))
      .maybeTransform(billingMode)(_.billingMode(_))
      .maybeTransform(leasesRecoveryAuditorExecutionFrequency)(
        _.leasesRecoveryAuditorExecutionFrequencyMillis(_)
      )
      .maybeTransform(leasesRecoveryAuditorInconsistencyConfidenceThreshold)(
        _.leasesRecoveryAuditorInconsistencyConfidenceThreshold(_)
      )
      .maybeTransform(maxCacheMissesBeforeReload)(
        _.maxCacheMissesBeforeReload(_)
      )
      .maybeTransform(listShardsCacheAllowedAge)(
        _.listShardsCacheAllowedAgeInSeconds(_)
      )
      .maybeTransform(cacheMissWarningModulus)(_.cacheMissWarningModulus(_))
      .maybeTransform(customShardDetectorProvider) { case (conf, x) =>
        conf.customShardDetectorProvider(x.asJava)
      }
      .maybeTransform(tableCreatorCallback)(_.tableCreatorCallback(_))
      .maybeTransform(executorService)(_.executorService(_))
      .maybeTransform(leaseManagementFactory)(_.leaseManagementFactory(_))
      .maybeTransform(leaseTableDeletionProtectionEnabled)(
        _.leaseTableDeletionProtectionEnabled(_)
      )
      .maybeTransform(leaseTablePitrEnabled)(_.leaseTablePitrEnabled(_))
      .maybeTransform(leasesRecoveryAuditorExecutionFrequencyMillis)(
        _.leasesRecoveryAuditorExecutionFrequencyMillis(_)
      )
      .maybeTransform(leasesRecoveryAuditorInconsistencyConfidenceThreshold)(
        _.leasesRecoveryAuditorInconsistencyConfidenceThreshold(_)
      )
      .maybeTransform(tags)(_.tags(_))
      .workerUtilizationAwareAssignmentConfig(
        workerUtilizationAwareAssignmentConfig
      )

    /** Reads the
      * [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/leases/LeaseManagementConfig.java LeaseManagementConfig]]
      * as a [[cats.effect.Resource Resource]]
      *
      * @param dynamoClient
      *   [[https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/dynamodb/DynamoDbAsyncClient.html DynamoDbAsyncClient]]
      * @param kinesisClient
      *   [[https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/kinesis/KinesisAsyncClient.html KinesisAsyncClient]]
      * @param prefix
      *   Optional prefix to apply to configuration loaders. Default None
      * @param customShardDetectorProvider
      *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/common/StreamConfig.java StreamConfig]]
      *   \=>
      *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/leases/ShardDetector.java ShardDetector]]
      * @param tableCreatorCallback
      *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/leases/dynamodb/TableCreatorCallback.java TableCreatorCallback]]
      * @param hierarchicalShardSyncer
      *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/leases/HierarchicalShardSyncer.java HierarchicalShardSyncer]]
      * @param leaseManagementFactory
      *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/leases/LeaseManagementFactory.java LeaseManagementFactory]]
      * @param leaseExecutorService
      *   [[https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ExecutorService.html ExecutorService]]
      *   for the lease management
      * @param F
      *   [[cats.effect.Async Async]]
      * @return
      *   [[cats.effect.Resource Resource]] of
      *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/leases/LeaseManagementConfig.java LeaseManagementConfig]]
      */
    private[kinesis4cats] def resource[F[_]](
        dynamoClient: DynamoDbAsyncClient,
        kinesisClient: KinesisAsyncClient,
        prefix: Option[String],
        customShardDetectorProvider: Option[StreamConfig => ShardDetector],
        tableCreatorCallback: Option[TableCreatorCallback],
        leaseManagementFactory: Option[LeaseManagementFactory],
        executorService: Option[ExecutorService]
    )(implicit F: Async[F]): Resource[F, LeaseManagementConfig] = read(
      dynamoClient,
      kinesisClient,
      prefix,
      customShardDetectorProvider,
      tableCreatorCallback,
      leaseManagementFactory,
      executorService
    ).resource[F]
  }

  object Lifecycle {

    /** Reads the
      * [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/lifecycle/LifecycleConfig.java LifecycleConfig]]
      *
      * @param prefix
      *   Optional prefix to apply to configuration loaders. Default None
      * @param aggregatorUtil
      *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/retrieval/AggregatorUtil.java AggregatorUtil]]
      * @param taskExecutionListener
      *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/lifecycle/TaskExecutionListener.java TaskExecutionListener]]
      * @return
      *   [[https://cir.is/api/ciris/ConfigDecoder.html ConfigDecoder]] of
      *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/lifecycle/LifecycleConfig.java LifecycleConfig]]
      */
    private[kinesis4cats] def read(
        prefix: Option[String],
        aggregatorUtil: Option[AggregatorUtil],
        taskExecutionListener: Option[TaskExecutionListener]
    ): ConfigValue[Effect, LifecycleConfig] = for {
      logWarningForTaskAfter <- CirisReader
        .readOptional[Duration](
          List("kcl", "lifecycle", "log", "warning", "for", "task", "after"),
          prefix
        )
        .map(_.map(x => java.lang.Long.valueOf(x.toMillis)))
        .map(_.asJava)
      taskBackoffTime <- CirisReader
        .readOptional[Duration](
          List("kcl", "lifecycle", "task", "backoff", "time"),
          prefix
        )
        .map(_.map(_.toMillis))
      readTimeoutsToIgnoreBeforeWarning <- CirisReader
        .readOptional[Int](
          List(
            "kcl",
            "lifecycle",
            "read",
            "timeouts",
            "to",
            "ignore",
            "before",
            "warning"
          ),
          prefix
        )
    } yield new LifecycleConfig()
      .logWarningForTaskAfterMillis(logWarningForTaskAfter)
      .maybeTransform(taskBackoffTime)(_.taskBackoffTimeMillis(_))
      .maybeTransform(readTimeoutsToIgnoreBeforeWarning)(
        _.readTimeoutsToIgnoreBeforeWarning(_)
      )
      .maybeTransform(aggregatorUtil)(_.aggregatorUtil(_))
      .maybeTransform(taskExecutionListener)(_.taskExecutionListener(_))

    /** Reads the
      * [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/lifecycle/LifecycleConfig.java LifecycleConfig]]
      * as a [[cats.effect.Resource Resource]]
      *
      * @param prefix
      *   Optional prefix to apply to configuration loaders. Default None
      * @param aggregatorUtil
      *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/retrieval/AggregatorUtil.java AggregatorUtil]]
      * @param taskExecutionListener
      *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/lifecycle/TaskExecutionListener.java TaskExecutionListener]]
      * @param F
      *   [[cats.effect.Async Async]]
      * @return
      *   [[cats.effect.Resource Resource]] of
      *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/lifecycle/LifecycleConfig.java LifecycleConfig]]
      */
    private[kinesis4cats] def resource[F[_]](
        prefix: Option[String],
        aggregatorUtil: Option[AggregatorUtil],
        taskExecutionListener: Option[TaskExecutionListener]
    )(implicit F: Async[F]): Resource[F, LifecycleConfig] =
      read(prefix, aggregatorUtil, taskExecutionListener).resource[F]
  }

  object Metrics {

    /** Reads the
      * [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/metrics/MetricsConfig.java MetricsConfig]]
      *
      * @param cloudWatchClient
      *   [[https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/cloudwatch/CloudWatchClient.html CloudWatchClient]]
      * @param prefix
      *   Optional prefix to apply to configuration loaders. Default None
      * @param metricsFactory
      *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/metrics/MetricsFactory.java MetricsFactory]]
      * @return
      *   [[https://cir.is/api/ciris/ConfigDecoder.html ConfigDecoder]] of
      *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/metrics/MetricsConfig.java MetricsConfig]]
      */
    private[kinesis4cats] def read(
        cloudwatchClient: CloudWatchAsyncClient,
        prefix: Option[String],
        metricsFactory: Option[MetricsFactory]
    ): ConfigValue[Effect, MetricsConfig] = for {
      namespace <- CirisReader
        .read[String](
          List("kcl", "metrics", "namespace"),
          prefix
        )
        .or(Common.readAppName(prefix))
      metricsBufferTime <- CirisReader
        .readOptional[Duration](
          List("kcl", "metrics", "buffer", "time"),
          prefix
        )
        .map(_.map(_.toMillis))
      metricsMaxQueueSize <- CirisReader
        .readOptional[Int](
          List("kcl", "metrics", "max", "queue", "size"),
          prefix
        )
      metricsLevel <- CirisReader
        .readOptional[MetricsLevel](
          List("kcl", "metrics", "level"),
          prefix
        )
      metricsEnabledDimensions <- CirisReader
        .readOptional[java.util.HashSet[String]](
          List("kcl", "metrics", "enabled", "dimensions"),
          prefix
        )
      publisherFlushBuffer <- CirisReader
        .readOptional[Int](
          List("kcl", "metrics", "publisher", "flush", "buffer"),
          prefix
        )
    } yield new MetricsConfig(cloudwatchClient, namespace)
      .maybeTransform(metricsBufferTime)(_.metricsBufferTimeMillis(_))
      .maybeTransform(metricsMaxQueueSize)(_.metricsMaxQueueSize(_))
      .maybeTransform(metricsLevel)(_.metricsLevel(_))
      .maybeTransform(metricsEnabledDimensions)(_.metricsEnabledDimensions(_))
      .maybeTransform(publisherFlushBuffer)(_.publisherFlushBuffer(_))
      .maybeTransform(metricsFactory)(_.metricsFactory(_))

    /** Reads the
      * [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/metrics/MetricsConfig.java MetricsConfig]]
      * into a [[cats.effect.Resource Resource]]
      *
      * @param cloudWatchClient
      *   [[https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/cloudwatch/CloudWatchClient.html CloudWatchClient]]
      * @param prefix
      *   Optional prefix to apply to configuration loaders. Default None
      * @param metricsFactory
      *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/metrics/MetricsFactory.java MetricsFactory]]
      * @param F
      *   [[cats.effect.Async Async]]
      * @return
      *   [[cats.effect.Resource Resource]] of
      *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/metrics/MetricsConfig.java MetricsConfig]]
      */
    private[kinesis4cats] def resource[F[_]](
        cloudwatchClient: CloudWatchAsyncClient,
        prefix: Option[String],
        metricsFactory: Option[MetricsFactory]
    )(implicit F: Async[F]): Resource[F, MetricsConfig] =
      read(cloudwatchClient, prefix, metricsFactory).resource[F]
  }

  object Retrieval {

    /** Reads the
      * [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/retrieval/polling/PollingConfig.java PollingConfig]]
      *
      * @param kinesisClient
      *   [[https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/kinesis/KinesisAsyncClient.html KinesisAsyncClient]]
      * @param prefix
      *   Optional prefix to apply to configuration loaders. Default None
      * @return
      *   [[https://cir.is/api/ciris/ConfigDecoder.html ConfigDecoder]] of
      *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/retrieval/polling/PollingConfig.java PollingConfig]]
      */
    private[kinesis4cats] def readPollingConfig(
        kinesisClient: KinesisAsyncClient,
        prefix: Option[String]
    ): ConfigValue[Effect, PollingConfig] = for {
      streamName <- Common.readStreamName(prefix)
      maxRecords <- CirisReader.readOptional[Int](
        List("kcl", "retrieval", "polling", "max", "records"),
        prefix
      )
      idleTimeBetweenReads <- CirisReader
        .readOptional[Duration](
          List(
            "kcl",
            "retrieval",
            "polling",
            "idle",
            "time",
            "between",
            "reads"
          ),
          prefix
        )
        .map(_.map(_.toMillis))
      retryGetRecords <- CirisReader
        .readOptional[Duration](
          List(
            "kcl",
            "retrieval",
            "polling",
            "retry",
            "get",
            "records",
            "interval"
          ),
          prefix
        )
        .map(_.map(x => Integer.valueOf(x.toSeconds.toInt)).asJava)
      maxGetRecordsThreadPool <- CirisReader
        .readOptional[Integer](
          List(
            "kcl",
            "retrieval",
            "polling",
            "max",
            "get",
            "records",
            "thread",
            "pool"
          ),
          prefix
        )
        .map(_.asJava)
      usePollingConfigIdleTimeValue <- CirisReader.readOptional[Boolean](
        List(
          "kcl",
          "retrieval",
          "polling",
          "use",
          "polling",
          "config",
          "idle",
          "time",
          "value"
        ),
        prefix
      )
    } yield new PollingConfig(streamName, kinesisClient)
      .maybeTransform(maxRecords) { case (pollingConfig, mr) =>
        pollingConfig.maxRecords(mr)
        pollingConfig
      }
      .maybeTransform(idleTimeBetweenReads)(_.idleTimeBetweenReadsInMillis(_))
      .maybeTransform(usePollingConfigIdleTimeValue)(
        _.usePollingConfigIdleTimeValue(_)
      )
      .retryGetRecordsInSeconds(retryGetRecords)
      .maxGetRecordsThreadPool(maxGetRecordsThreadPool)

    /** Reads the
      * [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/retrieval/fanout/FanOutConfig.java FanOutConfig]]
      *
      * @param kinesisClient
      *   [[https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/kinesis/KinesisAsyncClient.html KinesisAsyncClient]]
      * @param prefix
      *   Optional prefix to apply to configuration loaders. Default None
      * @return
      *   [[https://cir.is/api/ciris/ConfigDecoder.html ConfigDecoder]] of
      *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/retrieval/fanout/FanOutConfig.java FanOutConfig]]
      */
    private[kinesis4cats] def readFanOutConfig(
        kinesisClient: KinesisAsyncClient,
        prefix: Option[String]
    ): ConfigValue[Effect, FanOutConfig] = for {
      streamName <- Common.readStreamName(prefix)
      appName <- Common.readAppName(prefix)
      consumerArn <- CirisReader
        .readOptional[models.ConsumerArn](
          List("kcl", "retrieval", "fanout", "consumer", "arn"),
          prefix
        )
        .map(_.map(_.consumerArn))
      consumerName <- CirisReader.readOptional[String](
        List("kcl", "retrieval", "fanout", "consumer", "name"),
        prefix
      )
      maxDescribeStreamSummaryRetries <- CirisReader.readOptional[Int](
        List(
          "kcl",
          "retrieval",
          "fanout",
          "max",
          "describe",
          "stream",
          "summary",
          "retries"
        ),
        prefix
      )
      maxDescribeStreamConsumerRetries <- CirisReader.readOptional[Int](
        List(
          "kcl",
          "retrieval",
          "fanout",
          "max",
          "describe",
          "stream",
          "consumer",
          "retries"
        ),
        prefix
      )
      registerStreamConsumerRetries <- CirisReader.readOptional[Int](
        List(
          "kcl",
          "retrieval",
          "fanout",
          "register",
          "stream",
          "consumer",
          "retries"
        ),
        prefix
      )
      retryBackoff <- CirisReader
        .readOptional[Duration](
          List("kcl", "retrieval", "fanout", "retry", "backoff"),
          prefix
        )
        .map(_.map(_.toMillis))
    } yield new FanOutConfig(kinesisClient)
      .streamName(streamName)
      .applicationName(appName)
      .maybeTransform(consumerArn)(_.consumerArn(_))
      .maybeTransform(consumerName)(_.consumerName(_))
      .maybeTransform(maxDescribeStreamSummaryRetries)(
        _.maxDescribeStreamSummaryRetries(_)
      )
      .maybeTransform(maxDescribeStreamConsumerRetries)(
        _.maxDescribeStreamConsumerRetries(_)
      )
      .maybeTransform(registerStreamConsumerRetries)(
        _.registerStreamConsumerRetries(_)
      )
      .maybeTransform(retryBackoff)(_.retryBackoffMillis(_))

    /** Reads the
      * [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/retrieval/RetrievalConfig.java RetrievalConfig]]
      *
      * @param kinesisClient
      *   [[https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/kinesis/KinesisAsyncClient.html KinesisAsyncClient]]
      * @param prefix
      *   Optional prefix to apply to configuration loaders. Default None
      * @param glueSchemaRegistryDeserializer
      *   [[https://github.com/awslabs/aws-glue-schema-registry/blob/master/serializer-deserializer/src/main/java/com/amazonaws/services/schemaregistry/deserializers/GlueSchemaRegistryDeserializer.java GlueSchemaRegistryDeserializer]]
      * @return
      *   [[https://cir.is/api/ciris/ConfigDecoder.html ConfigDecoder]] of
      *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/retrieval/RetrievalConfig.java RetrievalConfig]]
      */
    private[kinesis4cats] def read(
        kinesisClient: KinesisAsyncClient,
        prefix: Option[String],
        glueSchemaRegistryDeserializer: Option[GlueSchemaRegistryDeserializer]
    ): ConfigValue[Effect, RetrievalConfig] = for {
      appName <- Common.readAppName(prefix)
      streamName <- Common.readStreamName(prefix)
      position <- Common.readInitialPosition(prefix)
      retrievalType <- CirisReader.readDefaulted[RetrievalType](
        List("kcl", "retrieval", "type"),
        RetrievalType.FanOut,
        prefix
      )
      retrievalConfig <- retrievalType match {
        case RetrievalType.Polling =>
          readPollingConfig(kinesisClient, prefix)
        case RetrievalType.FanOut =>
          readFanOutConfig(kinesisClient, prefix)
      }
      listShardsBackoffTime <- CirisReader
        .readOptional[Duration](
          List("kcl", "retrieval", "list", "shards", "backoff", "time"),
          prefix
        )
        .map(_.map(_.toMillis))
      maxListShardsRetryAttempts <- CirisReader.readOptional[Int](
        List("kcl", "retrieval", "max", "list", "shards", "retry", "attempts"),
        prefix
      )
    } yield new RetrievalConfig(
      kinesisClient,
      new SingleStreamTracker(
        StreamIdentifier.singleStreamInstance(streamName),
        position
      ),
      appName
    )
      .retrievalSpecificConfig(retrievalConfig)
      .retrievalFactory(retrievalConfig.retrievalFactory())
      .maybeTransform(listShardsBackoffTime)(
        _.listShardsBackoffTimeInMillis(_)
      )
      .maybeTransform(maxListShardsRetryAttempts)(
        _.maxListShardsRetryAttempts(_)
      )
      .maybeTransform(glueSchemaRegistryDeserializer)(
        _.glueSchemaRegistryDeserializer(_)
      )

    /** Reads the
      * [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/retrieval/RetrievalConfig.java RetrievalConfig]]
      * as a [[cats.effect.Resource Resource]]
      *
      * @param kinesisClient
      *   [[https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/kinesis/KinesisAsyncClient.html KinesisAsyncClient]]
      * @param prefix
      *   Optional prefix to apply to configuration loaders. Default None
      * @param glueSchemaRegistryDeserializer
      *   [[https://github.com/awslabs/aws-glue-schema-registry/blob/master/serializer-deserializer/src/main/java/com/amazonaws/services/schemaregistry/deserializers/GlueSchemaRegistryDeserializer.java GlueSchemaRegistryDeserializer]]
      * @param F
      *   [[cats.effect.Async Async]]
      * @return
      *   [[cats.effect.Resource Resource]] of
      *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/retrieval/RetrievalConfig.java RetrievalConfig]]
      */
    private[kinesis4cats] def resource[F[_]](
        kinesisClient: KinesisAsyncClient,
        prefix: Option[String],
        glueSchemaRegistryDeserializer: Option[GlueSchemaRegistryDeserializer]
    )(implicit F: Async[F]): Resource[F, RetrievalConfig] =
      read(kinesisClient, prefix, glueSchemaRegistryDeserializer).resource[F]
  }

  object Processor {

    /** Reads the
      * [[kinesis4cats.kcl.RecordProcessor.Config RecordProcessor.Config]]
      *
      * @param prefix
      *   Optional prefix to apply to configuration loaders. Default None
      * @return
      *   [[https://cir.is/api/ciris/ConfigDecoder.html ConfigDecoder]] of
      *   [[kinesis4cats.kcl.RecordProcessor.Config RecordProcessor.Config]]
      */
    private[kinesis4cats] def readRecordProcessorConfig(
        prefix: Option[String]
    ): ConfigValue[Effect, RecordProcessor.Config] = for {
      shardEndTimeout <- CirisReader.readOptional[FiniteDuration](
        List("kcl", "processor", "shard", "end", "timeout"),
        prefix
      )
      checkpointRetries <- CirisReader.readDefaulted[Int](
        List("kcl", "processor", "checkpoint", "retries"),
        RecordProcessor.Config.default.checkpointRetries,
        prefix
      )
      checkpointRetryInterval <- CirisReader.readDefaulted[FiniteDuration](
        List("kcl", "processor", "checkpoint", "retry", "interval"),
        RecordProcessor.Config.default.checkpointRetryInterval,
        prefix
      )
      autoCommit <- CirisReader.readDefaulted[Boolean](
        List("kcl", "processor", "auto", "commit"),
        RecordProcessor.Config.default.autoCommit,
        prefix
      )
    } yield RecordProcessor.Config(
      shardEndTimeout,
      checkpointRetries,
      checkpointRetryInterval,
      autoCommit
    )

    /** Reads the
      * [[kinesis4cats.kcl.KCLConsumer.ProcessConfig KCLConsumer.ProcessConfig]]
      *
      * @param prefix
      *   Optional prefix to apply to configuration loaders. Default None
      * @return
      *   [[https://cir.is/api/ciris/ConfigDecoder.html ConfigDecoder]] of
      *   [[kinesis4cats.kcl.KCLConsumer.ProcessConfig KCLConsumer.ProcessConfig]]
      */
    private[kinesis4cats] def read(
        prefix: Option[String]
    ): ConfigValue[Effect, KCLConsumer.ProcessConfig] = for {
      recordProcessor <- readRecordProcessorConfig(prefix)
      callProcessRecordsEvenForEmptyRecordList <- CirisReader
        .readOptional[Boolean](
          List(
            "kcl",
            "processor",
            "call",
            "process",
            "records",
            "even",
            "for",
            "empty",
            "list"
          ),
          prefix
        )
      raiseOnError <- CirisReader.readDefaulted[Boolean](
        List("kcl", "processor", "raise", "on", "error"),
        true,
        prefix
      )
    } yield KCLConsumer.ProcessConfig(
      raiseOnError,
      recordProcessor,
      callProcessRecordsEvenForEmptyRecordList
    )

    /** Reads the
      * [[kinesis4cats.kcl.KCLConsumer.ProcessConfig KCLConsumer.ProcessConfig]]
      * as a [[cats.effect.Resource Resource]]
      *
      * @param prefix
      *   Optional prefix to apply to configuration loaders. Default None
      * @param F
      *   [[cats.effect.Async Async]]
      * @return
      *   [[cats.effect.Resource Resource]] of
      *   [[kinesis4cats.kcl.KCLConsumer.ProcessConfig KCLConsumer.ProcessConfig]]
      */
    private[kinesis4cats] def resource[F[_]](
        prefix: Option[String]
    )(implicit F: Async[F]): Resource[F, KCLConsumer.ProcessConfig] = read(
      prefix
    ).resource[F]
  }

}
