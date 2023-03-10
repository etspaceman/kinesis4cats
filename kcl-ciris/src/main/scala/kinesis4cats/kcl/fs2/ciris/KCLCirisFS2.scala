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

package kinesis4cats.kcl.fs2
package ciris

import scala.concurrent.duration._

import java.util.concurrent.ExecutorService

import _root_.ciris._
import cats.Parallel
import cats.effect.{Async, Resource}
import com.amazonaws.services.schemaregistry.deserializers.GlueSchemaRegistryDeserializer
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient
import software.amazon.kinesis.common._
import software.amazon.kinesis.coordinator._
import software.amazon.kinesis.leases._
import software.amazon.kinesis.leases.dynamodb.TableCreatorCallback
import software.amazon.kinesis.lifecycle._
import software.amazon.kinesis.metrics._
import software.amazon.kinesis.retrieval.AggregatorUtil

import kinesis4cats.ciris.CirisReader
import kinesis4cats.instances.ciris._
import kinesis4cats.kcl.RecordProcessor
import kinesis4cats.kcl.ciris.KCLCiris

/** Standard configuration loader of env variables and system properties for
  * [[https://github.com/awslabs/amazon-kinesis-producer/blob/master/java/amazon-kinesis-producer/src/main/java/com/amazonaws/services/kinesis/producer/KinesisProducerConfiguration.java KinesisProducerConfiguration]]
  * via [[https://cir.is/ Ciris]] - FS2 offering.
  */
object KCLCirisFS2 {

  /** Reads environment variables and system properties to load a
    * [[kinesis4cats.kcl.fs2.KCLConsumerFS2 KCLConsumerFS2]]
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
    * @param P
    *   [[cats.Parallel Parallel]] instance
    * @param LE
    *   [[kinesis4cats.kcl.RecordProcessor.LogEncoders RecordProcessor.LogEncoders]]
    *   for encoding structured logs
    * @return
    *   [[cats.effect.Resource Resource]] containing the
    *   [[kinesis4cats.kcl.fs2.KCLConsumerFS2 KCLConsumerFS2]]
    */
  def consumer[F[_]](
      kinesisClient: KinesisAsyncClient,
      dynamoClient: DynamoDbAsyncClient,
      cloudwatchClient: CloudWatchAsyncClient,
      prefix: Option[String] = None,
      shardPrioritization: Option[ShardPrioritization] = None,
      workerStateChangeListener: Option[WorkerStateChangeListener] = None,
      coordinatorFactory: Option[CoordinatorFactory] = None,
      customShardDetectorProvider: Option[StreamConfig => ShardDetector] = None,
      tableCreatorCallback: Option[TableCreatorCallback] = None,
      hierarchicalShardSyncer: Option[HierarchicalShardSyncer] = None,
      leaseManagementFactory: Option[LeaseManagementFactory] = None,
      leaseExecutorService: Option[ExecutorService] = None,
      aggregatorUtil: Option[AggregatorUtil] = None,
      taskExecutionListener: Option[TaskExecutionListener] = None,
      metricsFactory: Option[MetricsFactory] = None,
      glueSchemaRegistryDeserializer: Option[GlueSchemaRegistryDeserializer] =
        None
  )(implicit
      F: Async[F],
      P: Parallel[F],
      LE: RecordProcessor.LogEncoders
  ): Resource[F, KCLConsumerFS2[F]] = kclConfig(
    kinesisClient,
    dynamoClient,
    cloudwatchClient,
    prefix,
    shardPrioritization,
    workerStateChangeListener,
    coordinatorFactory,
    customShardDetectorProvider,
    tableCreatorCallback,
    hierarchicalShardSyncer,
    leaseManagementFactory,
    leaseExecutorService,
    aggregatorUtil,
    taskExecutionListener,
    metricsFactory,
    glueSchemaRegistryDeserializer
  ).map(new KCLConsumerFS2[F](_))

  /** Reads environment variables and system properties to load a
    * [[kinesis4cats.kcl.fs2.KCLConsumerFS2.Config KCLConsumerFS2.Config]]
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
    *   [[kinesis4cats.kcl.fs2.KCLConsumerFS2.Config KCLConsumerFS2.Config]]
    */
  def kclConfig[F[_]](
      kinesisClient: KinesisAsyncClient,
      dynamoClient: DynamoDbAsyncClient,
      cloudwatchClient: CloudWatchAsyncClient,
      prefix: Option[String] = None,
      shardPrioritization: Option[ShardPrioritization] = None,
      workerStateChangeListener: Option[WorkerStateChangeListener] = None,
      coordinatorFactory: Option[CoordinatorFactory] = None,
      customShardDetectorProvider: Option[StreamConfig => ShardDetector] = None,
      tableCreatorCallback: Option[TableCreatorCallback] = None,
      hierarchicalShardSyncer: Option[HierarchicalShardSyncer] = None,
      leaseManagementFactory: Option[LeaseManagementFactory] = None,
      leaseExecutorService: Option[ExecutorService] = None,
      aggregatorUtil: Option[AggregatorUtil] = None,
      taskExecutionListener: Option[TaskExecutionListener] = None,
      metricsFactory: Option[MetricsFactory] = None,
      glueSchemaRegistryDeserializer: Option[GlueSchemaRegistryDeserializer] =
        None
  )(implicit
      F: Async[F],
      LE: RecordProcessor.LogEncoders
  ): Resource[F, KCLConsumerFS2.Config[F]] = for {
    autoCommit <- CirisReader
      .readDefaulted[Boolean](
        List("kcl", "processor", "auto", "commit"),
        false,
        prefix
      )
      .resource[F]
    fs2Config <- readFS2Config(prefix).resource[F]
    checkpointConfig <- KCLCiris.Checkpoint.resource[F]
    coordinatorConfig <- KCLCiris.Coordinator.resource[F](
      prefix,
      shardPrioritization,
      workerStateChangeListener,
      coordinatorFactory
    )
    leaseManagementConfig <- KCLCiris.Lease.resource[F](
      dynamoClient,
      kinesisClient,
      prefix,
      customShardDetectorProvider,
      tableCreatorCallback,
      hierarchicalShardSyncer,
      leaseManagementFactory,
      leaseExecutorService
    )
    lifecycleConfig <- KCLCiris.Lifecycle
      .resource[F](prefix, aggregatorUtil, taskExecutionListener)
    metricsConfig <- KCLCiris.Metrics
      .resource[F](cloudwatchClient, prefix, metricsFactory)
    retrievalConfig <- KCLCiris.Retrieval
      .resource[F](kinesisClient, prefix, glueSchemaRegistryDeserializer)
    processConfig <- KCLCiris.Processor.resource[F](prefix)
    config <- KCLConsumerFS2.Config.create[F](
      checkpointConfig,
      coordinatorConfig,
      leaseManagementConfig,
      lifecycleConfig,
      metricsConfig,
      retrievalConfig,
      fs2Config,
      processConfig.copy(recordProcessorConfig =
        processConfig.recordProcessorConfig.copy(autoCommit = autoCommit)
      )
    )

  } yield config

  def readFS2Config(
      prefix: Option[String] = None
  ): ConfigValue[Effect, KCLConsumerFS2.FS2Config] = for {
    queueSize <- CirisReader
      .readDefaulted[Int](List("kcl", "fs2", "queue", "size"), 100, prefix)
    commitMaxChunk <- CirisReader
      .readDefaulted[Int](
        List("kcl", "fs2", "commit", "max", "chunk"),
        1000,
        prefix
      )
    commitMaxWait <- CirisReader
      .readDefaulted[FiniteDuration](
        List("kcl", "fs2", "commit", "max", "wait"),
        10.seconds,
        prefix
      )
    commitMaxRetries <- CirisReader
      .readDefaulted[Int](
        List("kcl", "fs2", "commit", "max", "retries"),
        5,
        prefix
      )
    commitRetryInterval <- CirisReader
      .readDefaulted[FiniteDuration](
        List("kcl", "fs2", "commit", "retry", "interval"),
        0.seconds,
        prefix
      )
  } yield KCLConsumerFS2.FS2Config(
    queueSize,
    commitMaxChunk,
    commitMaxWait,
    commitMaxRetries,
    commitRetryInterval
  )
}
