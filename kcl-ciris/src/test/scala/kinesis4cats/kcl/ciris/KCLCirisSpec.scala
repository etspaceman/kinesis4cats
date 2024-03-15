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

package kinesis4cats.kcl.ciris

import scala.jdk.CollectionConverters._

import cats.effect.IO
import cats.syntax.all._
import software.amazon.awssdk.services.dynamodb.model.BillingMode
import software.amazon.kinesis.common._
import software.amazon.kinesis.coordinator.CoordinatorConfig
import software.amazon.kinesis.leases.LeaseManagementConfig
import software.amazon.kinesis.lifecycle.LifecycleConfig
import software.amazon.kinesis.metrics.{MetricsConfig, MetricsLevel}
import software.amazon.kinesis.processor.SingleStreamTracker
import software.amazon.kinesis.retrieval.RetrievalConfig
import software.amazon.kinesis.retrieval.fanout.FanOutConfig
import software.amazon.kinesis.retrieval.polling.PollingConfig

import kinesis4cats.compat.OptionConverters._
import kinesis4cats.kcl.instances.eq._
import kinesis4cats.kcl.instances.show._
import kinesis4cats.kcl.{KCLConsumer, RecordProcessor}
import kinesis4cats.localstack.aws.v2.AwsClients
import kinesis4cats.syntax.id._
import kinesis4cats.syntax.string._

class KCLCirisSpec extends munit.CatsEffectSuite {

  test(
    "It should load the environment variables the same as system properties for CoordinatorConfig"
  ) {
    for {
      configEnv <- KCLCiris.Coordinator
        .read(Some("env"), None, None, None)
        .load[IO]
      configProp <- KCLCiris.Coordinator
        .read(Some("prop"), None, None, None)
        .load[IO]
      expected = new CoordinatorConfig(BuildInfo.kclAppName)
        .safeTransform(BuildInfo.kclCoordinatorMaxInitializationAttempts.toInt)(
          _.maxInitializationAttempts(_)
        )
        .safeTransform(
          BuildInfo.kclCoordinatorParentShardPollInterval.asMillisUnsafe
        )(
          _.parentShardPollIntervalMillis(_)
        )
        .safeTransform(
          BuildInfo.kclCoordinatorSkipShardSyncAtInitializationIfLeasesExist.toBoolean
        )(
          _.skipShardSyncAtWorkerInitializationIfLeasesExist(_)
        )
        .safeTransform(
          BuildInfo.kclCoordinatorShardConsumerDispatchPollInterval.asMillisUnsafe
        )(
          _.shardConsumerDispatchPollIntervalMillis(_)
        )
        .safeTransform(
          BuildInfo.kclCoordinatorSchedulerInitializationBackoffTime.asMillisUnsafe
        )(
          _.schedulerInitializationBackoffTimeMillis(_)
        )

    } yield {
      assert(
        configEnv === configProp,
        s"envi: ${configEnv.show}\nprop: ${configProp.show}"
      )
      assert(
        configEnv === expected,
        s"envi: ${configEnv.show}\nexpe: ${expected.show}"
      )
    }
  }

  test(
    "It should load the environment variables the same as system properties for LeaseManagementConfig"
  ) {
    for {
      dynamoClient <- AwsClients.dynamoClient[IO]()
      kinesisClient <- AwsClients.kinesisClient[IO]()
      configEnv <- KCLCiris.Lease
        .read(
          dynamoClient,
          kinesisClient,
          Some("env"),
          None,
          None,
          None,
          None,
          None
        )
        .load[IO]
      configProp <- KCLCiris.Lease
        .read(
          dynamoClient,
          kinesisClient,
          Some("prop"),
          None,
          None,
          None,
          None,
          None
        )
        .load[IO]
      expected = new LeaseManagementConfig(
        BuildInfo.kclLeaseTableName,
        dynamoClient,
        kinesisClient,
        BuildInfo.kclLeaseWorkerId
      ).initialPositionInStream(
        InitialPositionInStreamExtended.newInitialPosition(
          InitialPositionInStream.valueOf(BuildInfo.kclInitialPosition)
        )
      ).safeTransform(BuildInfo.kclLeaseFailoverTime.asMillisUnsafe)(
        _.failoverTimeMillis(_)
      ).safeTransform(BuildInfo.kclLeaseShardSyncInterval.asMillisUnsafe)(
        _.shardSyncIntervalMillis(_)
      ).safeTransform(
        BuildInfo.kclLeaseCleanupLeasesUponShardCompletion.toBoolean
      )(
        _.cleanupLeasesUponShardCompletion(_)
      ).safeTransform(BuildInfo.kclLeaseMaxLeasesForWorker.toInt)(
        _.maxLeasesForWorker(_)
      ).safeTransform(BuildInfo.kclLeaseMaxLeasesToStealAtOneTime.toInt)(
        _.maxLeasesToStealAtOneTime(_)
      ).safeTransform(BuildInfo.kclLeaseInitialLeaseTableReadCapacity.toInt)(
        _.initialLeaseTableReadCapacity(_)
      ).safeTransform(BuildInfo.kclLeaseInitialLeaseTableWriteCapacity.toInt)(
        _.initialLeaseTableWriteCapacity(_)
      ).safeTransform(BuildInfo.kclLeaseMaxLeaseRenewalThreads.toInt)(
        _.maxLeaseRenewalThreads(_)
      ).safeTransform(BuildInfo.kclLeaseIgnoreUnexpectedChildShards.toBoolean)(
        _.ignoreUnexpectedChildShards(_)
      ).safeTransform(BuildInfo.kclLeaseConsistentReads.toBoolean)(
        _.consistentReads(_)
      ).safeTransform(BuildInfo.kclLeaseListShardsBackoffTime.asMillisUnsafe)(
        _.listShardsBackoffTimeInMillis(_)
      ).safeTransform(BuildInfo.kclLeaseMaxListShardsRetryAttempts.toInt)(
        _.maxListShardsRetryAttempts(_)
      ).safeTransform(BuildInfo.kclLeaseEpsilon.asMillisUnsafe)(
        _.epsilonMillis(_)
      ).safeTransform(
        BuildInfo.kclLeaseDynamoRequestTimeout.asJavaDurationUnsafe
      )(
        _.dynamoDbRequestTimeout(_)
      ).safeTransform(BillingMode.valueOf(BuildInfo.kclLeaseBillingMode))(
        _.billingMode(_)
      ).safeTransform(
        BuildInfo.kclLeaseLeasesRecoveryAuditorExecutionFrequency.asMillisUnsafe
      )(
        _.leasesRecoveryAuditorExecutionFrequencyMillis(_)
      ).safeTransform(
        BuildInfo.kclLeaseLeasesRecoveryAuditorInconsistencyConfidenceThreshold.toInt
      )(
        _.leasesRecoveryAuditorInconsistencyConfidenceThreshold(_)
      ).safeTransform(BuildInfo.kclLeaseMaxCacheMissesBeforeReload.toInt)(
        _.maxCacheMissesBeforeReload(_)
      ).safeTransform(
        BuildInfo.kclLeaseListShardsCacheAllowedAge.asSecondsUnsafe
      )(
        _.listShardsCacheAllowedAgeInSeconds(_)
      ).safeTransform(BuildInfo.kclLeaseCacheMissWarningModulus.toInt)(
        _.cacheMissWarningModulus(_)
      )
    } yield {
      assert(
        configEnv === configProp,
        s"envi: ${configEnv.show}\nprop: ${configProp.show}"
      )
      assert(
        configEnv === expected,
        s"envi: ${configEnv.show}\nexpe: ${expected.show}"
      )
    }
  }

  test(
    "It should load the environment variables the same as system properties for LifecycleConfig"
  ) {
    for {
      configEnv <- KCLCiris.Lifecycle.read(Some("env"), None, None).load[IO]
      configProp <- KCLCiris.Lifecycle.read(Some("prop"), None, None).load[IO]
      expected = new LifecycleConfig()
        .logWarningForTaskAfterMillis(
          java.lang.Long
            .valueOf(
              BuildInfo.kclLifecycleLogWarningForTaskAfter.asMillisUnsafe
            )
            .some
            .asJava
        )
        .safeTransform(BuildInfo.kclLifecycleTaskBackoffTime.asMillisUnsafe)(
          _.taskBackoffTimeMillis(_)
        )
        .safeTransform(
          BuildInfo.kclLifecycleReadTimeoutsToIgnoreBeforeWarning.toInt
        )(
          _.readTimeoutsToIgnoreBeforeWarning(_)
        )
    } yield {
      assert(
        configEnv === configProp,
        s"envi: ${configEnv.show}\nprop: ${configProp.show}"
      )
      assert(
        configEnv === expected,
        s"envi: ${configEnv.show}\nexpe: ${expected.show}"
      )
    }
  }

  test(
    "It should load the environment variables the same as system properties for MetricsConfig"
  ) {
    for {
      cloudwatchClient <- AwsClients.cloudwatchClient[IO]()
      configEnv <- KCLCiris.Metrics
        .read(cloudwatchClient, Some("env"), None)
        .load[IO]
      configProp <- KCLCiris.Metrics
        .read(cloudwatchClient, Some("prop"), None)
        .load[IO]
      expected = new MetricsConfig(
        cloudwatchClient,
        BuildInfo.kclMetricsNamespace
      )
        .safeTransform(BuildInfo.kclMetricsBufferTime.asMillisUnsafe)(
          _.metricsBufferTimeMillis(_)
        )
        .safeTransform(BuildInfo.kclMetricsMaxQueueSize.toInt)(
          _.metricsMaxQueueSize(_)
        )
        .safeTransform(MetricsLevel.valueOf(BuildInfo.kclMetricsLevel))(
          _.metricsLevel(_)
        )
        .safeTransform {
          val hs = new java.util.HashSet[String]
          hs.addAll(BuildInfo.kclMetricsEnabledDimensions.asList.toSet.asJava)
          hs
        }(_.metricsEnabledDimensions(_))
        .safeTransform(BuildInfo.kclMetricsPublisherFlushBuffer.toInt)(
          _.publisherFlushBuffer(_)
        )
    } yield {
      assert(
        configEnv === configProp,
        s"envi: ${configEnv.show}\nprop: ${configProp.show}"
      )
      assert(
        configEnv === expected,
        s"envi: ${configEnv.show}\nexpe: ${expected.show}"
      )
    }
  }

  test(
    "It should load the environment variables the same as system properties for RetrievalConfig"
  ) {
    for {
      kinesisClient <- AwsClients.kinesisClient[IO]()
      fanoutConfigEnv <- KCLCiris.Retrieval
        .read(kinesisClient, Some("FANOUT_ENV"), None)
        .load[IO]
      fanoutConfigProp <- KCLCiris.Retrieval
        .read(kinesisClient, Some("fanout.prop"), None)
        .load[IO]
      pollingConfigEnv <- KCLCiris.Retrieval
        .read(kinesisClient, Some("POLLING_ENV"), None)
        .load[IO]
      pollingConfigProp <- KCLCiris.Retrieval
        .read(kinesisClient, Some("polling.prop"), None)
        .load[IO]
      pollingExpected = new RetrievalConfig(
        kinesisClient,
        new SingleStreamTracker(
          StreamIdentifier.singleStreamInstance(BuildInfo.pollingKclStreamName),
          InitialPositionInStreamExtended.newInitialPosition(
            InitialPositionInStream.valueOf(BuildInfo.pollingKclInitialPosition)
          )
        ),
        BuildInfo.pollingKclAppName
      )
        .retrievalSpecificConfig(
          new PollingConfig(BuildInfo.pollingKclStreamName, kinesisClient)
            .safeTransform(
              BuildInfo.pollingKclRetrievalPollingMaxRecords.toInt
            ) { case (pollingConfig, mr) =>
              pollingConfig.maxRecords(mr)
              pollingConfig
            }
            .safeTransform(
              BuildInfo.pollingKclRetrievalPollingIdleTimeBetweenReads.asMillisUnsafe
            )(
              _.idleTimeBetweenReadsInMillis(_)
            )
            .safeTransform(
              BuildInfo.pollingKclRetrievalPollingUsePollingConfigIdleTimeValue.toBoolean
            )(
              _.usePollingConfigIdleTimeValue(_)
            )
            .retryGetRecordsInSeconds(
              java.lang.Integer
                .valueOf(
                  BuildInfo.pollingKclRetrievalPollingRetryGetRecordsInterval.asSecondsUnsafe.toInt
                )
                .some
                .asJava
            )
            .maxGetRecordsThreadPool(
              java.lang.Integer
                .valueOf(
                  BuildInfo.kclRetrievalPollingMaxGetRecordsThreadPool.toInt
                )
                .some
                .asJava
            )
        )
        .safeTransform(
          BuildInfo.pollingKclRetrievalListShardsBackoffTime.asMillisUnsafe
        )(
          _.listShardsBackoffTimeInMillis(_)
        )
        .safeTransform(
          BuildInfo.pollingKclRetrievalMaxListShardsRetryAttempts.toInt
        )(
          _.maxListShardsRetryAttempts(_)
        )

      fanoutExpected = new RetrievalConfig(
        kinesisClient,
        new SingleStreamTracker(
          StreamIdentifier.singleStreamInstance(BuildInfo.fanoutKclStreamName),
          InitialPositionInStreamExtended.newInitialPosition(
            InitialPositionInStream.valueOf(BuildInfo.fanoutKclInitialPosition)
          )
        ),
        BuildInfo.fanoutKclAppName
      )
        .retrievalSpecificConfig(
          new FanOutConfig(kinesisClient)
            .streamName(BuildInfo.fanoutKclStreamName)
            .applicationName(BuildInfo.fanoutKclAppName)
            .safeTransform(BuildInfo.fanoutKclRetrievalFanoutConsumerArn)(
              _.consumerArn(_)
            )
            .safeTransform(BuildInfo.fanoutKclRetrievalFanoutConsumerName)(
              _.consumerName(_)
            )
            .safeTransform(
              BuildInfo.fanoutKclRetrievalFanoutMaxDescribeStreamSummaryRetries.toInt
            )(
              _.maxDescribeStreamSummaryRetries(_)
            )
            .safeTransform(
              BuildInfo.fanoutKclRetrievalFanoutMaxDescribeStreamConsumerRetries.toInt
            )(
              _.maxDescribeStreamConsumerRetries(_)
            )
            .safeTransform(
              BuildInfo.fanoutKclRetrievalFanoutRegisterStreamConsumerRetries.toInt
            )(
              _.registerStreamConsumerRetries(_)
            )
            .safeTransform(
              BuildInfo.fanoutKclRetrievalFanoutRetryBackoff.asMillisUnsafe
            )(_.retryBackoffMillis(_))
        )
        .safeTransform(
          BuildInfo.fanoutKclRetrievalListShardsBackoffTime.asMillisUnsafe
        )(
          _.listShardsBackoffTimeInMillis(_)
        )
        .safeTransform(
          BuildInfo.fanoutKclRetrievalMaxListShardsRetryAttempts.toInt
        )(
          _.maxListShardsRetryAttempts(_)
        )
    } yield {
      assert(
        fanoutConfigEnv === fanoutConfigProp,
        s"envi: ${fanoutConfigEnv.show}\nprop: ${fanoutConfigProp.show}"
      )
      assert(
        pollingConfigEnv === pollingConfigProp,
        s"envi: ${pollingConfigEnv.show}\nprop: ${pollingConfigProp.show}"
      )
      assert(
        pollingConfigEnv === pollingExpected,
        s"envi: ${pollingConfigEnv.show}\nexpe: ${pollingExpected.show}"
      )
      assert(
        fanoutConfigEnv === fanoutExpected,
        s"envi: ${fanoutConfigEnv.show}\nexpe: ${fanoutExpected.show}"
      )
    }
  }

  test(
    "It should load the environment variables the same as system properties for ProcessorConfig"
  ) {
    for {
      configEnv <- KCLCiris.Processor.read(prefix = Some("env"))
      configProp <- KCLCiris.Processor.read(prefix = Some("prop"))
      expected = KCLConsumer.ProcessConfig(
        BuildInfo.kclProcessorRaiseOnError.toBoolean,
        RecordProcessor.Config(
          BuildInfo.kclProcessorShardEndTimeout.asFiniteDurationUnsafe.some,
          BuildInfo.kclProcessorCheckpointRetries.toInt,
          BuildInfo.kclProcessorCheckpointRetryInterval.asFiniteDurationUnsafe,
          BuildInfo.kclProcessorAutoCommit.toBoolean
        ),
        BuildInfo.kclProcessorCallProcessRecordsEvenForEmptyList.toBoolean.some
      )
    } yield {
      assert(
        configEnv === configProp,
        s"envi: ${configEnv.show}\nprop: ${configProp.show}"
      )
      assert(
        configEnv === expected,
        s"envi: ${configEnv.show}\nexpe: ${expected.show}"
      )
    }
  }

}
