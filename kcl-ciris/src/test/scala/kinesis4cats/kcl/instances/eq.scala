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

package kinesis4cats.kcl.instances

import scala.concurrent.duration.Duration
import scala.jdk.CollectionConverters._

import cats.Eq
import cats.syntax.all._
import software.amazon.awssdk.services.dynamodb.model.BillingMode
import software.amazon.awssdk.services.dynamodb.model.Tag
import software.amazon.kinesis.common.InitialPositionInStreamExtended
import software.amazon.kinesis.coordinator.CoordinatorConfig
import software.amazon.kinesis.leases.LeaseManagementConfig
import software.amazon.kinesis.leases.LeaseManagementConfig.WorkerMetricsTableConfig
import software.amazon.kinesis.lifecycle.LifecycleConfig
import software.amazon.kinesis.metrics.MetricsConfig
import software.amazon.kinesis.retrieval._
import software.amazon.kinesis.retrieval.fanout.FanOutConfig
import software.amazon.kinesis.retrieval.polling.PollingConfig

import kinesis4cats.compat.DurationConverters._
import kinesis4cats.kcl.{KCLConsumer, RecordProcessor}

object eq {
  implicit val javaDurationEq: Eq[java.time.Duration] =
    Eq[Duration].contramap(_.toScala)

  implicit val initialPositionInStreamExtendedEq
      : Eq[InitialPositionInStreamExtended] = (x, y) =>
    x.getInitialPositionInStream() == y.getInitialPositionInStream() &&
      Option(x.getTimestamp()).map(_.toInstant().toEpochMilli()) === Option(
        y.getTimestamp()
      ).map(_.toInstant().toEpochMilli())

  implicit val coordinatorConfigEq: Eq[CoordinatorConfig] = (x, y) =>
    x.applicationName() === y.applicationName() &&
      x.maxInitializationAttempts() === y.maxInitializationAttempts() &&
      x.parentShardPollIntervalMillis() === y.parentShardPollIntervalMillis() &&
      x.schedulerInitializationBackoffTimeMillis() === y
        .schedulerInitializationBackoffTimeMillis() &&
      x.shardConsumerDispatchPollIntervalMillis() === y
        .shardConsumerDispatchPollIntervalMillis() &&
      x.skipShardSyncAtWorkerInitializationIfLeasesExist() === y
        .skipShardSyncAtWorkerInitializationIfLeasesExist()

  implicit val tagEq: Eq[Tag] = Eq.by(x => (x.key, x.value))

  implicit def collectionEq[A](implicit
      eqA: Eq[A]
  ): Eq[java.util.Collection[A]] = Eq.by(x => x.asScala.toList)

  implicit val billingModeEq: Eq[BillingMode] = Eq.fromUniversalEquals

  implicit val workerMetricsTableConfigEq: Eq[WorkerMetricsTableConfig] =
    Eq.by(x =>
      (
        x.tableName(),
        x.billingMode(),
        x.readCapacity(),
        x.writeCapacity(),
        x.pointInTimeRecoveryEnabled(),
        x.deletionProtectionEnabled(),
        x.tags()
      )
    )

  implicit val leaseManagementConfigEq: Eq[LeaseManagementConfig] = (x, y) =>
    x.billingMode() === y.billingMode() &&
      x.cacheMissWarningModulus() === y.cacheMissWarningModulus() &&
      x.cleanupLeasesUponShardCompletion() === y
        .cleanupLeasesUponShardCompletion() &&
      x.consistentReads() === y.consistentReads() &&
      x.dynamoDbRequestTimeout() == y.dynamoDbRequestTimeout() &&
      x.epsilonMillis() === y.epsilonMillis() &&
      x.failoverTimeMillis() === y.failoverTimeMillis() &&
      x.ignoreUnexpectedChildShards() === y.ignoreUnexpectedChildShards() &&
      x.initialLeaseTableReadCapacity() === y.initialLeaseTableReadCapacity() &&
      x.initialLeaseTableWriteCapacity() === y
        .initialLeaseTableWriteCapacity() &&
      x.initialPositionInStream() === y.initialPositionInStream() &&
      x.leasesRecoveryAuditorExecutionFrequencyMillis() === y
        .leasesRecoveryAuditorExecutionFrequencyMillis() &&
      x.leasesRecoveryAuditorInconsistencyConfidenceThreshold() === y
        .leasesRecoveryAuditorInconsistencyConfidenceThreshold() &&
      x.listShardsBackoffTimeInMillis() === y.listShardsBackoffTimeInMillis() &&
      x.maxCacheMissesBeforeReload() === y.maxCacheMissesBeforeReload() &&
      x.maxLeaseRenewalThreads() === y.maxLeaseRenewalThreads() &&
      x.maxLeasesForWorker() === y.maxLeasesForWorker() &&
      x.maxLeasesToStealAtOneTime() === y.maxLeasesToStealAtOneTime() &&
      x.maxListShardsRetryAttempts() === y.maxListShardsRetryAttempts() &&
      x.shardSyncIntervalMillis() === y.shardSyncIntervalMillis() &&
      x.streamName() === y.streamName() &&
      x.tableName() === y.tableName() &&
      x.workerIdentifier() === y.workerIdentifier() &&
      x.leaseTableDeletionProtectionEnabled() === y
        .leaseTableDeletionProtectionEnabled() &&
      x.leaseTablePitrEnabled() === y.leaseTablePitrEnabled() &&
      x.workerUtilizationAwareAssignmentConfig
        .inMemoryWorkerMetricsCaptureFrequencyMillis() === y.workerUtilizationAwareAssignmentConfig
        .inMemoryWorkerMetricsCaptureFrequencyMillis() &&
      x.workerUtilizationAwareAssignmentConfig
        .workerMetricsReporterFreqInMillis() === y.workerUtilizationAwareAssignmentConfig
        .workerMetricsReporterFreqInMillis() &&
      x.workerUtilizationAwareAssignmentConfig
        .noOfPersistedMetricsPerWorkerMetrics() === y.workerUtilizationAwareAssignmentConfig
        .noOfPersistedMetricsPerWorkerMetrics() &&
      x.workerUtilizationAwareAssignmentConfig
        .disableWorkerMetrics() === y.workerUtilizationAwareAssignmentConfig
        .disableWorkerMetrics() &&
      x.workerUtilizationAwareAssignmentConfig
        .maxThroughputPerHostKBps() === y.workerUtilizationAwareAssignmentConfig
        .maxThroughputPerHostKBps() &&
      x.workerUtilizationAwareAssignmentConfig
        .dampeningPercentage() === y.workerUtilizationAwareAssignmentConfig
        .dampeningPercentage() &&
      x.workerUtilizationAwareAssignmentConfig
        .reBalanceThresholdPercentage() === y.workerUtilizationAwareAssignmentConfig
        .reBalanceThresholdPercentage() &&
      x.workerUtilizationAwareAssignmentConfig
        .allowThroughputOvershoot() === y.workerUtilizationAwareAssignmentConfig
        .allowThroughputOvershoot() &&
      x.workerUtilizationAwareAssignmentConfig
        .staleWorkerMetricsEntryCleanupDuration() === y.workerUtilizationAwareAssignmentConfig
        .staleWorkerMetricsEntryCleanupDuration() &&
      x.workerUtilizationAwareAssignmentConfig
        .workerMetricsTableConfig() === y.workerUtilizationAwareAssignmentConfig
        .workerMetricsTableConfig() &&
      x.workerUtilizationAwareAssignmentConfig
        .varianceBalancingFrequency() === y.workerUtilizationAwareAssignmentConfig
        .varianceBalancingFrequency() &&
      x.workerUtilizationAwareAssignmentConfig
        .workerMetricsEMAAlpha() === y.workerUtilizationAwareAssignmentConfig
        .workerMetricsEMAAlpha()

  implicit val lifecycleConfigEq: Eq[LifecycleConfig] = (x, y) =>
    x.logWarningForTaskAfterMillis() == y.logWarningForTaskAfterMillis() &&
      x.readTimeoutsToIgnoreBeforeWarning() === y
        .readTimeoutsToIgnoreBeforeWarning() &&
      x.taskBackoffTimeMillis() === y.taskBackoffTimeMillis()

  implicit val metricsConfigEq: Eq[MetricsConfig] = (x, y) =>
    x.metricsBufferTimeMillis() === y.metricsBufferTimeMillis() &&
      x.metricsEnabledDimensions() == y.metricsEnabledDimensions() &&
      x.metricsLevel() == y.metricsLevel() &&
      x.metricsMaxQueueSize() === y.metricsMaxQueueSize() &&
      x.namespace() === y.namespace() &&
      x.publisherFlushBuffer() === y.publisherFlushBuffer()

  implicit val pollingConfigEq: Eq[PollingConfig] = (x, y) =>
    x.idleTimeBetweenReadsInMillis() === y.idleTimeBetweenReadsInMillis() &&
      x.kinesisRequestTimeout() == y.kinesisRequestTimeout() &&
      x.maxGetRecordsThreadPool() == y.maxGetRecordsThreadPool() &&
      x.maxRecords() === y.maxRecords() &&
      x.retryGetRecordsInSeconds() == y.retryGetRecordsInSeconds() &&
      x.streamName() === y.streamName() &&
      x.usePollingConfigIdleTimeValue() === y.usePollingConfigIdleTimeValue()

  implicit val fanoutConfigEq: Eq[FanOutConfig] = (x, y) =>
    x.applicationName() === y.applicationName() &&
      x.consumerArn() === y.consumerArn() &&
      x.consumerName() === y.consumerName() &&
      x.maxDescribeStreamConsumerRetries() === y
        .maxDescribeStreamConsumerRetries() &&
      x.maxDescribeStreamSummaryRetries() === y
        .maxDescribeStreamSummaryRetries() &&
      x.registerStreamConsumerRetries() === y.registerStreamConsumerRetries() &&
      x.retryBackoffMillis() === y.retryBackoffMillis() &&
      x.streamName() === y.streamName()

  implicit val retrievalSpecificConfigEq: Eq[RetrievalSpecificConfig] = {
    case (x: PollingConfig, y: PollingConfig) => pollingConfigEq.eqv(x, y)
    case (x: FanOutConfig, y: FanOutConfig)   => fanoutConfigEq.eqv(x, y)
    case (x, y)                               => x == y
  }

  implicit val retrievalConfigEq: Eq[RetrievalConfig] = (x, y) =>
    x.streamTracker() == y.streamTracker() &&
      x.applicationName() === y.applicationName() &&
      x.listShardsBackoffTimeInMillis() === y.listShardsBackoffTimeInMillis() &&
      x.maxListShardsRetryAttempts() === y.maxListShardsRetryAttempts() &&
      x.retrievalSpecificConfig() === y.retrievalSpecificConfig()

  implicit val recordProcessorConfigEq: Eq[RecordProcessor.Config] = (x, y) =>
    x.autoCommit === y.autoCommit &&
      x.checkpointRetries === y.checkpointRetries &&
      x.checkpointRetryInterval === y.checkpointRetryInterval &&
      x.shardEndTimeout === y.shardEndTimeout

  implicit val processConfigEq: Eq[KCLConsumer.ProcessConfig] = (x, y) =>
    x.callProcessRecordsEvenForEmptyRecordList === y.callProcessRecordsEvenForEmptyRecordList &&
      x.raiseOnError === y.raiseOnError &&
      x.recordProcessorConfig === y.recordProcessorConfig
}
