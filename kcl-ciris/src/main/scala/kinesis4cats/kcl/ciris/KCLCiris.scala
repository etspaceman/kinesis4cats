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

import scala.compat.java8.FunctionConverters._
import scala.compat.java8.OptionConverters._
import scala.concurrent.duration.{Duration, FiniteDuration}

import java.util.UUID
import java.util.concurrent.ExecutorService

import _root_.ciris._
import cats.effect.{Async, Resource}
import com.amazonaws.services.schemaregistry.deserializers.GlueSchemaRegistryDeserializer
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient
import software.amazon.awssdk.services.dynamodb.model.BillingMode
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient
import software.amazon.kinesis.checkpoint.CheckpointConfig
import software.amazon.kinesis.common._
import software.amazon.kinesis.coordinator._
import software.amazon.kinesis.leases._
import software.amazon.kinesis.leases.dynamodb.TableCreatorCallback
import software.amazon.kinesis.lifecycle._
import software.amazon.kinesis.metrics._
import software.amazon.kinesis.retrieval.fanout.FanOutConfig
import software.amazon.kinesis.retrieval.polling.PollingConfig
import software.amazon.kinesis.retrieval.{AggregatorUtil, RetrievalConfig}

import kinesis4cats.ciris.CirisReader
import kinesis4cats.instances.ciris._
import kinesis4cats.kcl.instances.ciris._
import kinesis4cats.models
import kinesis4cats.syntax.id._

object KCLCiris {

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
  )(cb: List[CommittableRecord[F]] => F[Unit])(implicit
      F: Async[F],
      LE: RecordProcessor.LogEncoders
  ): Resource[F, KCLConsumer[F]] = kclConfig[F](
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
  )(cb).map(new KCLConsumer[F](_))

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
  )(cb: List[CommittableRecord[F]] => F[Unit])(implicit
      F: Async[F],
      LE: RecordProcessor.LogEncoders
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
      hierarchicalShardSyncer,
      leaseManagementFactory,
      leaseExecutorService
    )
    lifecycleConfig <- Lifecycle
      .resource[F](prefix, aggregatorUtil, taskExecutionListener)
    metricsConfig <- Metrics
      .resource[F](cloudwatchClient, prefix, metricsFactory)
    retrievalConfig <- Retrieval
      .resource[F](kinesisClient, prefix, glueSchemaRegistryDeserializer)
    processorConfig <- Processor.resource[F](prefix)
    config <- KCLConsumer.Config.create[F](
      checkpointConfig,
      coordinatorConfig,
      leaseManagementConfig,
      lifecycleConfig,
      metricsConfig,
      retrievalConfig,
      processorConfig.raiseOnError,
      processorConfig.recordProcessor,
      processorConfig.callProcessRecordsEvenForEmptyRecordList
    )(cb)
  } yield config

  object Common {
    def readAppName(
        prefix: Option[String] = None
    ): ConfigValue[Effect, String] =
      CirisReader.read[String](List("kcl", "app", "name"), prefix)

    def readStreamName(
        prefix: Option[String] = None
    ): ConfigValue[Effect, String] =
      CirisReader.read[String](List("kcl", "stream", "name"), prefix)

    def readInitialPosition(
        prefix: Option[String] = None
    ): ConfigValue[Effect, Option[InitialPositionInStreamExtended]] =
      CirisReader.readOptional[InitialPositionInStreamExtended](
        List("kcl", "initial", "position"),
        prefix
      )
  }

  object Checkpoint {
    def read: ConfigValue[Effect, CheckpointConfig] =
      ConfigValue.default(new CheckpointConfig())

    def load[F[_]](implicit F: Async[F]) = read.load[F]

    def resource[F[_]](implicit F: Async[F]) = read.resource[F]
  }

  object Coordinator {
    def read(
        prefix: Option[String],
        shardPrioritization: Option[ShardPrioritization] = None,
        workerStateChangeListener: Option[WorkerStateChangeListener] = None,
        coordinatorFactory: Option[CoordinatorFactory] = None
    ): ConfigValue[Effect, CoordinatorConfig] =
      for {
        appName <- Common.readAppName(prefix)
        maxInitializationAttempts <- CirisReader.readOptional[Int](
          List("kcl", "coordinator", "max", "initialization", "attempts")
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
            List("scheduler", "initialization", "backoff", "time")
          )
          .map(_.map(_.toMillis))
      } yield new CoordinatorConfig(appName)
        .maybeTransform(maxInitializationAttempts)(
          _.maxInitializationAttempts(_)
        )
        .maybeTransform(parentShardPollInterval)(
          _.parentShardPollIntervalMillis(_)
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

    def load[F[_]](
        prefix: Option[String],
        shardPrioritization: Option[ShardPrioritization] = None,
        workerStateChangeListener: Option[WorkerStateChangeListener] = None,
        coordinatorFactory: Option[CoordinatorFactory] = None
    )(implicit F: Async[F]): F[CoordinatorConfig] = read(
      prefix,
      shardPrioritization,
      workerStateChangeListener,
      coordinatorFactory
    ).load[F]

    def resource[F[_]](
        prefix: Option[String],
        shardPrioritization: Option[ShardPrioritization] = None,
        workerStateChangeListener: Option[WorkerStateChangeListener] = None,
        coordinatorFactory: Option[CoordinatorFactory] = None
    )(implicit F: Async[F]): Resource[F, CoordinatorConfig] = read(
      prefix,
      shardPrioritization,
      workerStateChangeListener,
      coordinatorFactory
    ).resource[F]
  }

  object Lease {
    def read(
        dynamoClient: DynamoDbAsyncClient,
        kinesisClient: KinesisAsyncClient,
        prefix: Option[String] = None,
        customShardDetectorProvider: Option[StreamConfig => ShardDetector] =
          None,
        tableCreatorCallback: Option[TableCreatorCallback] = None,
        hierarchicalShardSyncer: Option[HierarchicalShardSyncer] = None,
        leaseManagementFactory: Option[LeaseManagementFactory] = None,
        executorService: Option[ExecutorService] = None
    ): ConfigValue[Effect, LeaseManagementConfig] = for {
      tableName <- CirisReader
        .read[String](List("kcl", "lease", "table", "name"))
        .or(Common.readAppName(prefix))
      workerId <- CirisReader.readDefaulted(
        List("kcl", "lease", "worker", "id"),
        UUID.randomUUID().toString()
      )
      failoverTime <- CirisReader
        .readOptional[Duration](List("kcl", "lease", "failover", "time"))
        .map(_.map(_.toMillis))
      shardSyncInterval <- CirisReader
        .readOptional[Duration](
          List("kcl", "lease", "shard", "sync", "interval")
        )
        .map(_.map(_.toMillis))
      cleanupLeasesUponShardCompletion <- CirisReader.readOptional[Boolean](
        List("kcl", "lease", "cleanup", "leases", "upon", "shard", "completion")
      )
      maxLeasesForWorker <- CirisReader.readOptional[Int](
        List("kcl", "lease", "max", "leases", "for", "worker")
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
        )
      )
      initialLeaseTableReadCapacity <- CirisReader.readOptional[Int](
        List("kcl", "lease", "initial", "lease", "table", "read", "capacity")
      )
      initialLeaseTableWriteCapacity <- CirisReader.readOptional[Int](
        List("kcl", "lease", "initial", "lease", "table", "write", "capacity")
      )
      maxLeaseRenewalThreads <- CirisReader.readOptional[Int](
        List("kcl", "lease", "max", "lease", "renewal", "threads")
      )
      ignoreUnexpectedChildShards <- CirisReader.readOptional[Boolean](
        List("kcl", "lease", "ignore", "unexpected", "child", "shards")
      )
      consistentReads <- CirisReader.readOptional[Boolean](
        List("kcl", "lease", "consistent", "reads")
      )
      listShardsBackoffTime <- CirisReader
        .readOptional[Duration](
          List("kcl", "lease", "list", "shards", "backoff", "time")
        )
        .map(_.map(_.toMillis))
      maxListShardsRetryAttempts <- CirisReader.readOptional[Int](
        List("kcl", "lease", "max", "list", "shards", "retry", "attempts")
      )
      epsilon <- CirisReader
        .readOptional[Duration](
          List("kcl", "lease", "epsilon")
        )
        .map(_.map(_.toMillis))
      dynamoDbRequestTimeout <- CirisReader.readOptional[java.time.Duration](
        List("kcl", "lease", "dynamo", "request", "timeout")
      )
      billingMode <- CirisReader.readOptional[BillingMode](
        List("kcl", "lease", "billing", "mode")
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
          )
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
          )
        )
      initialPositionInStream <- Common.readInitialPosition(prefix)
      maxCacheMissesBeforeReload <- CirisReader.readOptional[Int](
        List("kcl", "lease", "max", "cache", "misses", "before", "reload")
      )
      listShardsCacheAllowedAge <- CirisReader
        .readOptional[Duration](
          List("kcl", "lease", "list", "shards", "cache", "allowed", "age")
        )
        .map(_.map(_.toSeconds))
      cacheMissWarningModulus <- CirisReader.readOptional[Int](
        List("kcl", "lease", "cache", "miss", "warning", "modulus")
      )
    } yield new LeaseManagementConfig(
      tableName,
      dynamoClient,
      kinesisClient,
      workerId
    ).maybeTransform(failoverTime)(_.failoverTimeMillis(_))
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
      .maybeTransform(initialPositionInStream)(_.initialPositionInStream(_))
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
      .maybeTransform(hierarchicalShardSyncer)(_.hierarchicalShardSyncer(_))
      .maybeTransform(executorService)(_.executorService(_))
      .maybeTransform(leaseManagementFactory)(_.leaseManagementFactory(_))

    def load[F[_]](
        dynamoClient: DynamoDbAsyncClient,
        kinesisClient: KinesisAsyncClient,
        prefix: Option[String] = None,
        customShardDetectorProvider: Option[StreamConfig => ShardDetector] =
          None,
        tableCreatorCallback: Option[TableCreatorCallback] = None,
        hierarchicalShardSyncer: Option[HierarchicalShardSyncer] = None,
        leaseManagementFactory: Option[LeaseManagementFactory] = None,
        executorService: Option[ExecutorService] = None
    )(implicit F: Async[F]): F[LeaseManagementConfig] = read(
      dynamoClient,
      kinesisClient,
      prefix,
      customShardDetectorProvider,
      tableCreatorCallback,
      hierarchicalShardSyncer,
      leaseManagementFactory,
      executorService
    ).load[F]

    def resource[F[_]](
        dynamoClient: DynamoDbAsyncClient,
        kinesisClient: KinesisAsyncClient,
        prefix: Option[String] = None,
        customShardDetectorProvider: Option[StreamConfig => ShardDetector] =
          None,
        tableCreatorCallback: Option[TableCreatorCallback] = None,
        hierarchicalShardSyncer: Option[HierarchicalShardSyncer] = None,
        leaseManagementFactory: Option[LeaseManagementFactory] = None,
        executorService: Option[ExecutorService] = None
    )(implicit F: Async[F]): Resource[F, LeaseManagementConfig] = read(
      dynamoClient,
      kinesisClient,
      prefix,
      customShardDetectorProvider,
      tableCreatorCallback,
      hierarchicalShardSyncer,
      leaseManagementFactory,
      executorService
    ).resource[F]
  }

  object Lifecycle {
    def read(
        prefix: Option[String] = None,
        aggregatorUtil: Option[AggregatorUtil] = None,
        taskExecutionListener: Option[TaskExecutionListener] = None
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

    def load[F[_]](
        prefix: Option[String] = None,
        aggregatorUtil: Option[AggregatorUtil] = None,
        taskExecutionListener: Option[TaskExecutionListener] = None
    )(implicit F: Async[F]): F[LifecycleConfig] =
      read(prefix, aggregatorUtil, taskExecutionListener).load[F]

    def resource[F[_]](
        prefix: Option[String] = None,
        aggregatorUtil: Option[AggregatorUtil] = None,
        taskExecutionListener: Option[TaskExecutionListener] = None
    )(implicit F: Async[F]): Resource[F, LifecycleConfig] =
      read(prefix, aggregatorUtil, taskExecutionListener).resource[F]
  }

  object Metrics {
    def read(
        cloudwatchClient: CloudWatchAsyncClient,
        prefix: Option[String] = None,
        metricsFactory: Option[MetricsFactory] = None
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
        .readOptional[java.util.Set[String]](
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

    def load[F[_]](
        cloudwatchClient: CloudWatchAsyncClient,
        prefix: Option[String] = None,
        metricsFactory: Option[MetricsFactory] = None
    )(implicit F: Async[F]): F[MetricsConfig] =
      read(cloudwatchClient, prefix, metricsFactory).load[F]

    def resource[F[_]](
        cloudwatchClient: CloudWatchAsyncClient,
        prefix: Option[String] = None,
        metricsFactory: Option[MetricsFactory] = None
    )(implicit F: Async[F]): Resource[F, MetricsConfig] =
      read(cloudwatchClient, prefix, metricsFactory).resource[F]
  }

  object Retrieval {
    def readPollingConfig(
        kinesisClient: KinesisAsyncClient,
        prefix: Option[String] = None
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
          List("kcl", "retrieval", "polling", "retry", "get", "records"),
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
    } yield new PollingConfig(streamName, kinesisClient)
      .maybeTransform(maxRecords)(_.maxRecords(_))
      .maybeTransform(idleTimeBetweenReads)(_.idleTimeBetweenReadsInMillis(_))
      .retryGetRecordsInSeconds(retryGetRecords)
      .maxGetRecordsThreadPool(maxGetRecordsThreadPool)

    def readFanOutConfig(
        kinesisClient: KinesisAsyncClient,
        prefix: Option[String] = None
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
          List("kcl", "retrieval", "fanout", "retry", "backoff", "millis"),
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

    def read(
        kinesisClient: KinesisAsyncClient,
        prefix: Option[String] = None,
        glueSchemaRegistryDeserializer: Option[GlueSchemaRegistryDeserializer] =
          None
    ): ConfigValue[Effect, RetrievalConfig] = for {
      appName <- Common.readAppName(prefix)
      streamName <- Common.readStreamName(prefix)
      position <- Common.readInitialPosition(prefix)
      retrievalType <- CirisReader.readDefaulted[RetrievalType](
        List("kcl", "retrieval", "type"),
        RetrievalType.Polling,
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
    } yield new RetrievalConfig(kinesisClient, streamName, appName)
      .retrievalSpecificConfig(retrievalConfig)
      .retrievalFactory(retrievalConfig.retrievalFactory())
      .maybeTransform(position)(
        _.initialPositionInStreamExtended(_)
      )
      .maybeTransform(listShardsBackoffTime)(
        _.listShardsBackoffTimeInMillis(_)
      )
      .maybeTransform(maxListShardsRetryAttempts)(
        _.maxListShardsRetryAttempts(_)
      )
      .maybeTransform(glueSchemaRegistryDeserializer)(
        _.glueSchemaRegistryDeserializer(_)
      )

    def load[F[_]](
        kinesisClient: KinesisAsyncClient,
        prefix: Option[String] = None,
        glueSchemaRegistryDeserializer: Option[GlueSchemaRegistryDeserializer] =
          None
    )(implicit F: Async[F]): F[RetrievalConfig] =
      read(kinesisClient, prefix, glueSchemaRegistryDeserializer).load[F]

    def resource[F[_]](
        kinesisClient: KinesisAsyncClient,
        prefix: Option[String] = None,
        glueSchemaRegistryDeserializer: Option[GlueSchemaRegistryDeserializer] =
          None
    )(implicit F: Async[F]): Resource[F, RetrievalConfig] =
      read(kinesisClient, prefix, glueSchemaRegistryDeserializer).resource[F]
  }

  object Processor {
    final case class Config(
        recordProcessor: RecordProcessor.Config,
        callProcessRecordsEvenForEmptyRecordList: Option[Boolean],
        raiseOnError: Boolean
    )

    def readRecordProcessorConfig(
        prefix: Option[String] = None
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

    def read(
        prefix: Option[String] = None
    ): ConfigValue[Effect, Config] = for {
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
    } yield Config(
      recordProcessor,
      callProcessRecordsEvenForEmptyRecordList,
      raiseOnError
    )

    def load[F[_]](
        prefix: Option[String] = None
    )(implicit F: Async[F]): F[Config] = read(prefix).load[F]

    def resource[F[_]](
        prefix: Option[String] = None
    )(implicit F: Async[F]): Resource[F, Config] = read(prefix).resource[F]
  }

}
