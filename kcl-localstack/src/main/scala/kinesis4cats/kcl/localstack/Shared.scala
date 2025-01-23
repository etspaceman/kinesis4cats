package kinesis4cats.kcl.localstack

import scala.concurrent.duration._

import software.amazon.awssdk.services.kinesis.KinesisAsyncClient
import software.amazon.kinesis.common.LeaseCleanupConfig
import software.amazon.kinesis.coordinator.CoordinatorConfig
import software.amazon.kinesis.leases.LeaseManagementConfig
import software.amazon.kinesis.leases.dynamodb.DynamoDBLeaseManagementFactory
import software.amazon.kinesis.leases.dynamodb.DynamoDBLeaseSerializer
import software.amazon.kinesis.leases.dynamodb.DynamoDBMultiStreamLeaseSerializer
import software.amazon.kinesis.processor.StreamTracker
import software.amazon.kinesis.retrieval.RetrievalConfig
import software.amazon.kinesis.retrieval.polling.PollingConfig

private[kcl] object Shared {
  private[kcl] def leaseManagement(
      defaultLeaseManagement: LeaseManagementConfig,
      streamTracker: StreamTracker
  ): LeaseManagementConfig = {
    val withTopLevel = defaultLeaseManagement
      .shardSyncIntervalMillis(1000L)
      .failoverTimeMillis(1000L)

    val withManagementFactory = withTopLevel.leaseManagementFactory(
      new DynamoDBLeaseManagementFactory(
        withTopLevel.kinesisClient(),
        withTopLevel.dynamoDBClient(),
        withTopLevel.tableName(),
        withTopLevel.workerIdentifier(),
        withTopLevel.executorService(),
        withTopLevel.failoverTimeMillis(),
        withTopLevel.enablePriorityLeaseAssignment(),
        withTopLevel.epsilonMillis(),
        withTopLevel.maxLeasesForWorker(),
        withTopLevel.maxLeasesToStealAtOneTime(),
        withTopLevel.maxLeaseRenewalThreads(),
        withTopLevel.cleanupLeasesUponShardCompletion(),
        withTopLevel.ignoreUnexpectedChildShards(),
        withTopLevel.shardSyncIntervalMillis(),
        withTopLevel.consistentReads(),
        withTopLevel.listShardsBackoffTimeInMillis(),
        withTopLevel.maxListShardsRetryAttempts(),
        withTopLevel.maxCacheMissesBeforeReload(),
        withTopLevel.listShardsCacheAllowedAgeInSeconds(),
        withTopLevel.cacheMissWarningModulus(),
        withTopLevel.initialLeaseTableReadCapacity().toLong,
        withTopLevel.initialLeaseTableWriteCapacity().toLong,
        withTopLevel.tableCreatorCallback(),
        withTopLevel.dynamoDbRequestTimeout(),
        withTopLevel.billingMode(),
        withTopLevel.leaseTableDeletionProtectionEnabled(),
        withTopLevel.leaseTablePitrEnabled(),
        withTopLevel.tags(),
        if (streamTracker.isMultiStream())
          new DynamoDBMultiStreamLeaseSerializer()
        else new DynamoDBLeaseSerializer(),
        withTopLevel.customShardDetectorProvider(),
        streamTracker.isMultiStream(),
        LeaseCleanupConfig
          .builder()
          .completedLeaseCleanupIntervalMillis(500L)
          .garbageLeaseCleanupIntervalMillis(500L)
          .leaseCleanupIntervalMillis(10.seconds.toMillis)
          .build(),
        withTopLevel
          .workerUtilizationAwareAssignmentConfig()
          .disableWorkerMetrics(true),
        withTopLevel.gracefulLeaseHandoffConfig()
      )
    )

    if (streamTracker.isMultiStream()) withManagementFactory
    else
      withManagementFactory.initialPositionInStream(
        streamTracker.streamConfigList
          .get(0)
          .initialPositionInStreamExtended()
      )
  }

  private[kcl] def retrievalConfig(
      defaultRetrieval: RetrievalConfig,
      streamTracker: StreamTracker,
      kinesisClient: KinesisAsyncClient
  ): RetrievalConfig = {

    val retrievalSpecificConfig =
      if (streamTracker.isMultiStream()) new PollingConfig(kinesisClient)
      else
        new PollingConfig(
          streamTracker.streamConfigList.get(0).streamIdentifier.streamName,
          kinesisClient
        )

    defaultRetrieval
      .retrievalSpecificConfig(retrievalSpecificConfig)
      .retrievalFactory(retrievalSpecificConfig.retrievalFactory())
  }

  private[kcl] def coordinatorConfig(
      defaultCoordinator: CoordinatorConfig
  ): CoordinatorConfig =
    defaultCoordinator.parentShardPollIntervalMillis(1000L)
}
