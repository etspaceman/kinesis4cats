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

package kinesis4cats
package kcl.instances

import scala.jdk.CollectionConverters._

import cats.Show
import cats.syntax.all._
import software.amazon.kinesis.common.{StreamConfig, StreamIdentifier}
import software.amazon.kinesis.coordinator.CoordinatorConfig
import software.amazon.kinesis.leases.LeaseManagementConfig
import software.amazon.kinesis.lifecycle.LifecycleConfig
import software.amazon.kinesis.metrics.MetricsConfig
import software.amazon.kinesis.processor.MultiStreamTracker
import software.amazon.kinesis.processor.SingleStreamTracker
import software.amazon.kinesis.processor.StreamTracker
import software.amazon.kinesis.retrieval._
import software.amazon.kinesis.retrieval.fanout.FanOutConfig
import software.amazon.kinesis.retrieval.polling.PollingConfig

import kinesis4cats.compat.OptionConverters._
import kinesis4cats.kcl.{KCLConsumer, RecordProcessor}
import kinesis4cats.logging.instances.show._

object show {
  implicit val coordinatorConfigShow: Show[CoordinatorConfig] = x =>
    ShowBuilder("CoordinatorConfig")
      .add("applicationName", x.applicationName())
      .add("maxInitializationAttempts", x.maxInitializationAttempts())
      .add("parentShardPollIntervalMillis", x.parentShardPollIntervalMillis())
      .add(
        "schedulerInitializationBackoffTimeMillis",
        x.schedulerInitializationBackoffTimeMillis()
      )
      .add(
        "shardConsumerDispatchPollIntervalMillis",
        x.shardConsumerDispatchPollIntervalMillis()
      )
      .add(
        "skipShardSyncAtWorkerInitializationIfLeasesExist",
        x.skipShardSyncAtWorkerInitializationIfLeasesExist()
      )
      .build

  implicit val leaseManagementConfigShow: Show[LeaseManagementConfig] = x =>
    ShowBuilder("LeaseManagementConfig")
      .add("billingMode", x.billingMode().name())
      .add("cacheMissWarningModulus", x.cacheMissWarningModulus())
      .add(
        "cleanupLeasesUponShardCompletion",
        x.cleanupLeasesUponShardCompletion()
      )
      .add("consistentReads", x.consistentReads())
      .add("dynamoDbRequestTimeout", x.dynamoDbRequestTimeout())
      .add("epsilonMillis", x.epsilonMillis())
      .add("failoverTimeMillis", x.failoverTimeMillis())
      .add("ignoreUnexpectedChildShards", x.ignoreUnexpectedChildShards())
      .add("initialLeaseTableReadCapacity", x.initialLeaseTableReadCapacity())
      .add("initialLeaseTableWriteCapacity", x.initialLeaseTableWriteCapacity())
      .add("initialPositionInStream", x.initialPositionInStream().toString())
      .add(
        "leasesRecoveryAuditorExecutionFrequencyMillis",
        x.leasesRecoveryAuditorExecutionFrequencyMillis()
      )
      .add(
        "leasesRecoveryAuditorInconsistencyConfidenceThreshold",
        x.leasesRecoveryAuditorInconsistencyConfidenceThreshold()
      )
      .add("listShardsBackoffTimeInMillis", x.listShardsBackoffTimeInMillis())
      .add("maxCacheMissesBeforeReload", x.maxCacheMissesBeforeReload())
      .add("maxLeaseRenewalThreads", x.maxLeaseRenewalThreads())
      .add("maxLeasesForWorker", x.maxLeasesForWorker())
      .add("maxLeasesToStealAtOneTime", x.maxLeasesToStealAtOneTime())
      .add("maxListShardsRetryAttempts", x.maxListShardsRetryAttempts())
      .add("shardSyncIntervalMillis", x.shardSyncIntervalMillis())
      .add("streamName", x.streamName())
      .add("tableName", x.tableName())
      .add("workerIdentifier", x.workerIdentifier())
      .add(
        "leaseTableDeletionProtectionEnabled",
        x.leaseTableDeletionProtectionEnabled()
      )
      .add("leaseTablePitrEnabled", x.leaseTablePitrEnabled())
      .build

  implicit val lifecycleConfigShow: Show[LifecycleConfig] = x =>
    ShowBuilder("LifecycleConfig")
      .add(
        "logWarningForTaskAfterMillis",
        x.logWarningForTaskAfterMillis().asScala
      )
      .add(
        "readTimeoutsToIgnoreBeforeWarning",
        x.readTimeoutsToIgnoreBeforeWarning()
      )
      .add("taskBackoffTimeMillis", x.taskBackoffTimeMillis())
      .build

  implicit val metricsConfigShow: Show[MetricsConfig] = x =>
    ShowBuilder("MetricsConfig")
      .add("metricsBufferTimeMillis", x.metricsBufferTimeMillis())
      .add(
        "metricsEnabledDimensions",
        x.metricsEnabledDimensions().asScala.toList
      )
      .add("metricsLevel", x.metricsLevel().getName())
      .add("metricsMaxQueueSize", x.metricsMaxQueueSize())
      .add("namespace", x.namespace())
      .add("publisherFlushBuffer", x.publisherFlushBuffer())
      .build

  implicit val pollingConfigShow: Show[PollingConfig] = x =>
    ShowBuilder("PollingConfig")
      .add("idleTimeBetweenReadsInMillis", x.idleTimeBetweenReadsInMillis())
      .add("kinesisRequestTimeout", x.kinesisRequestTimeout())
      .add("maxGetRecordsThreadPool", x.maxGetRecordsThreadPool().asScala)
      .add("maxRecords", x.maxRecords())
      .add("retryGetRecordsInSeconds", x.retryGetRecordsInSeconds().asScala)
      .add("streamName", x.streamName())
      .add("usePollingConfigIdleTimeValue", x.usePollingConfigIdleTimeValue())
      .build

  implicit val fanOutConfigShow: Show[FanOutConfig] = x =>
    ShowBuilder("FanOutConfig")
      .add("applicationName", x.applicationName())
      .add("consumerArn", x.consumerArn())
      .add("consumerName", x.consumerName())
      .add(
        "maxDescribeStreamConsumerRetries",
        x.maxDescribeStreamConsumerRetries()
      )
      .add(
        "maxDescribeStreamSummaryRetries",
        x.maxDescribeStreamSummaryRetries()
      )
      .add("registerStreamConsumerRetries", x.registerStreamConsumerRetries())
      .add("retryBackoffMillis", x.retryBackoffMillis())
      .add("streamName", x.streamName())
      .build

  implicit val retrievalSpecificConfigShow: Show[RetrievalSpecificConfig] = {
    case x: PollingConfig => pollingConfigShow.show(x)
    case x: FanOutConfig  => fanOutConfigShow.show(x)
    case x                => x.toString()
  }

  implicit def javaEitherShow[A: Show, B: Show]
      : Show[software.amazon.awssdk.utils.Either[A, B]] = x =>
    (x.left().asScala, x.right().asScala) match {
      case (Some(a), _) => a.show
      case (_, Some(b)) => b.show
      case _            => "Either(Nothing)"
    }

  implicit val streamIdentifierShow: Show[StreamIdentifier] = x =>
    ShowBuilder("StreamIdentifier")
      .add("accountIdOptional", x.accountIdOptional().asScala)
      .add(
        "streamCreationEpochOptional",
        x.streamCreationEpochOptional().asScala
      )
      .add("streamName", x.streamName())
      .build

  implicit val streamConfigShow: Show[StreamConfig] = x =>
    ShowBuilder("StreamConfig")
      .add("consumerArn", x.consumerArn())
      .add(
        "initialPositionInStreamExtended",
        x.initialPositionInStreamExtended().toString()
      )
      .add("streamIdentifier", x.streamIdentifier())
      .build

  implicit val mutliStreamTrackerShow: Show[MultiStreamTracker] = x =>
    ShowBuilder("MultiStreamTracker")
      .add("streamConfigList", x.streamConfigList().asScala.toList)
      .add("isMultiStream", x.isMultiStream())
      .build

  implicit val singleStreamTrackerShow: Show[SingleStreamTracker] = x =>
    ShowBuilder("SingleStreamTracker")
      .add("streamConfigList", x.streamConfigList().asScala.toList)
      .add("isMultiStream", x.isMultiStream())
      .build

  implicit val streamTrackerShow: Show[StreamTracker] = {
    case x: MultiStreamTracker  => mutliStreamTrackerShow.show(x)
    case x: SingleStreamTracker => singleStreamTrackerShow.show(x)
    case x                      => x.toString
  }

  implicit val retrievalConfigShow: Show[RetrievalConfig] = x =>
    ShowBuilder("RetrievalConfig")
      .add("streamTracker", x.streamTracker())
      .add("applicationName", x.applicationName())
      .add("listShardsBackoffTimeInMillis", x.listShardsBackoffTimeInMillis())
      .add("maxListShardsRetryAttempts", x.maxListShardsRetryAttempts())
      .add("retrievalSpecificConfig", x.retrievalSpecificConfig())
      .build

  implicit val recordProcessorConfigShow: Show[RecordProcessor.Config] = x =>
    ShowBuilder("Config")
      .add("autoCommit", x.autoCommit)
      .add("checkpointRetries", x.checkpointRetries)
      .add("checkpointRetryInterval", x.checkpointRetryInterval)
      .add("shardEndTimeout", x.shardEndTimeout)
      .build

  implicit val processConfigShow: Show[KCLConsumer.ProcessConfig] = x =>
    ShowBuilder("ProcessConfig")
      .add(
        "callProcessRecordsEvenForEmptyRecordList",
        x.callProcessRecordsEvenForEmptyRecordList
      )
      .add("raiseOnError", x.raiseOnError)
      .add("recordProcessorConfig", x.recordProcessorConfig)
      .build
}
