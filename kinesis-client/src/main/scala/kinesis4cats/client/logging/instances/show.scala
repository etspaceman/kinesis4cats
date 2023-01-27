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

package kinesis4cats.client.logging.instances

import java.nio.ByteBuffer

import cats.Show
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.kinesis.model._

import kinesis4cats.ShowBuilder
import kinesis4cats.client.KinesisClient
import kinesis4cats.logging.instances.show._

/** KinesisClient [[kinesis4cats.logging.LogEncoder LogEncoder]] instances for
  * string encoding of log structures using [[cats.Show Show]]
  */
object show {

  implicit val kinesisResponseMetadataShow: Show[KinesisResponseMetadata] = x =>
    ShowBuilder("KinesisResponseMetadata")
      .add("extendedRequestId", x.extendedRequestId())
      .add("requestId", x.requestId())
      .build

  implicit val streamModeDetailsShow: Show[StreamModeDetails] = x =>
    ShowBuilder("StreamModeDetails")
      .add("streamMode", x.streamModeAsString())
      .build

  implicit val enhancedMonitoringShow: Show[EnhancedMetrics] = x =>
    ShowBuilder("EnhancedMetrics")
      .add("hasShardLevelMetrics", x.hasShardLevelMetrics())
      .add("shardLevelMetrics", x.shardLevelMetricsAsStrings())
      .build

  implicit val hashKeyRangeShow: Show[HashKeyRange] = x =>
    ShowBuilder("HashKeyRange")
      .add("endingHashKey", x.endingHashKey())
      .add("startingHashKey", x.startingHashKey())
      .build

  implicit val sequenceNumberRangeShow: Show[SequenceNumberRange] = x =>
    ShowBuilder("SequenceNumberRange")
      .add("endingSequenceNumber", x.endingSequenceNumber())
      .add("startingSequenceNumber", x.startingSequenceNumber())
      .build

  implicit val shardShow: Show[Shard] = x =>
    ShowBuilder("Shard")
      .add("adjacentParentShardId", x.adjacentParentShardId())
      .add("hashKeyRange", x.hashKeyRange())
      .add("parentShardId", x.parentShardId())
      .add("sequenceNumberRange", x.sequenceNumberRange())
      .add("shardId", x.shardId())
      .build

  implicit val streamDescriptionShow: Show[StreamDescription] = x =>
    ShowBuilder("StreamDescription")
      .add("encryptionType", x.encryptionTypeAsString())
      .add("enhancedMonitoring", x.enhancedMonitoring())
      .add("hasEnhancedMonitoring", x.hasEnhancedMonitoring())
      .add("hasMoreShards", x.hasMoreShards())
      .add("hasShards", x.hasShards())
      .add("keyId", x.keyId())
      .add("retentionPeriodHours", x.retentionPeriodHours())
      .add("shards", x.shards())
      .add("streamARN", x.streamARN())
      .add("streamCreationTimestamp", x.streamCreationTimestamp())
      .add("streamModeDetails", x.streamModeDetails())
      .add("streamName", x.streamName())
      .add("streamStatus", x.streamStatusAsString())
      .build

  implicit val streamDescriptionSummaryShow: Show[StreamDescriptionSummary] =
    x =>
      ShowBuilder("StreamDescriptionSummary")
        .add("consumerCount", x.consumerCount())
        .add("encryptionType", x.encryptionTypeAsString())
        .add("enhancedMonitoring", x.enhancedMonitoring())
        .add("hasEnhancedMonitoring", x.hasEnhancedMonitoring())
        .add("keyId", x.keyId())
        .add("openShardCount", x.openShardCount())
        .add("retentionPeriodHours", x.retentionPeriodHours())
        .add("streamARN", x.streamARN())
        .add("streamCreationTimestamp", x.streamCreationTimestamp())
        .add("streamModeDetails", x.streamModeDetails())
        .add("streamName", x.streamName())
        .add("streamStatus", x.streamStatusAsString())
        .build

  implicit val consumerDescriptionShow: Show[ConsumerDescription] =
    x =>
      ShowBuilder("ConsumerDescription")
        .add("consumerARN", x.consumerARN())
        .add("consumerCreationTimestamp", x.consumerCreationTimestamp())
        .add("consumerName", x.consumerName())
        .add("consumerStatus", x.consumerStatusAsString())
        .add("streamARN", x.streamARN())
        .build

  implicit val childShardShow: Show[ChildShard] = x =>
    ShowBuilder("ChildShard")
      .add("hasParentShards", x.hasParentShards())
      .add("hashKeyRange", x.hashKeyRange())
      .add("parentShards", x.parentShards())
      .add("shardId", x.shardId())
      .build

  implicit val consumerShow: Show[Consumer] = x =>
    ShowBuilder("Consumer")
      .add("consumerARN", x.consumerARN())
      .add("consumerCreationTimestamp", x.consumerCreationTimestamp())
      .add("consumerName", x.consumerName())
      .add("consumerStatus", x.consumerStatusAsString())
      .build

  implicit val sdkBytesShow: Show[SdkBytes] = x =>
    Show[ByteBuffer].show(x.asByteBuffer())

  implicit val recordShow: Show[Record] = x =>
    ShowBuilder("Record")
      .add("approximateArrivalTimestamp", x.approximateArrivalTimestamp())
      .add("data", x.data())
      .add("encryptionTypeAsString", x.encryptionTypeAsString())
      .add("partitionKey", x.partitionKey())
      .add("sequenceNumber", x.sequenceNumber())
      .build

  implicit val shardFilterShow: Show[ShardFilter] = x =>
    ShowBuilder("ShardFilter")
      .add("shardId", x.shardId())
      .add("timestamp", x.timestamp())
      .add("type", x.typeAsString())
      .build

  implicit val streamSummaryShow: Show[StreamSummary] = x =>
    ShowBuilder("StreamSummary")
      .add("streamARN", x.streamARN())
      .add("streamCreationTimestamp", x.streamCreationTimestamp())
      .add("streamModeDetails", x.streamModeDetails())
      .add("streamName", x.streamName())
      .add("streamStatus", x.streamStatusAsString())
      .build

  implicit val tagShow: Show[Tag] = x =>
    ShowBuilder("Tag")
      .add("key", x.key())
      .add("value", x.value())
      .build

  implicit val putRecordsRequestEntryShow: Show[PutRecordsRequestEntry] = x =>
    ShowBuilder("PutRecordsRequestEntry")
      .add("data", x.data())
      .add("explicitHashKey", x.explicitHashKey())
      .add("partitionKey", x.partitionKey())
      .build

  implicit val putRecordsResultEntryShow: Show[PutRecordsResultEntry] = x =>
    ShowBuilder("PutRecordsResultEntry")
      .add("errorCode", x.errorCode())
      .add("errorMessage", x.errorMessage())
      .add("sequenceNumber", x.sequenceNumber())
      .add("shardId", x.shardId())
      .build

  implicit val startingPositionShow: Show[StartingPosition] = x =>
    ShowBuilder("StartingPosition")
      .add("sequenceNumber", x.sequenceNumber())
      .add("timestamp", x.timestamp())
      .add("typeAsString", x.typeAsString())
      .build

  implicit val addTagsToStreamRequestShow: Show[AddTagsToStreamRequest] = x =>
    ShowBuilder("AddTagsToStreamRequest")
      .add("streamName", x.streamName())
      .add("tags", x.tags())
      .add("streamARN", x.streamARN())
      .build

  implicit val addTagsToStreamResponseShow: Show[AddTagsToStreamResponse] = x =>
    ShowBuilder("AddTagsToStreamResponse")
      .add("responseMetadata", x.responseMetadata())
      .build

  implicit val createStreamRequestShow: Show[CreateStreamRequest] = x =>
    ShowBuilder("CreateStreamRequest")
      .add("streamName", x.streamName())
      .add("shardCount", x.shardCount())
      .add("streamARN", x.streamModeDetails())
      .build

  implicit val createStreamResponseShow: Show[CreateStreamResponse] = x =>
    ShowBuilder("CreateStreamResponse")
      .add("responseMetadata", x.responseMetadata())
      .build

  implicit val decreaseStreamRetentionPeriodRequestShow
      : Show[DecreaseStreamRetentionPeriodRequest] = x =>
    ShowBuilder("DecreaseStreamRetentionPeriodRequest")
      .add("streamARN", x.streamARN())
      .add("streamName", x.streamName())
      .add("retentionPeriodHours", x.retentionPeriodHours())
      .build

  implicit val decreaseStreamRetentionPeriodResponseShow
      : Show[DecreaseStreamRetentionPeriodResponse] = x =>
    ShowBuilder("DecreaseStreamRetentionPeriodResponse")
      .add("responseMetadata", x.responseMetadata())
      .build

  implicit val deleteStreamRequestShow: Show[DeleteStreamRequest] = x =>
    ShowBuilder("DeleteStreamRequest")
      .add("streamARN", x.streamARN())
      .add("streamName", x.streamName())
      .add("enforceConsumerDeletion", x.enforceConsumerDeletion())
      .build

  implicit val deleteStreamResponseShow: Show[DeleteStreamResponse] = x =>
    ShowBuilder("DeleteStreamResponse")
      .add("responseMetadata", x.responseMetadata())
      .build

  implicit val deregisterStreamConsumerRequestShow
      : Show[DeregisterStreamConsumerRequest] = x =>
    ShowBuilder("DeregisterStreamConsumerRequest")
      .add("streamARN", x.streamARN())
      .add("consumerName", x.consumerName())
      .add("consumerARN", x.consumerARN())
      .build

  implicit val deregisterStreamConsumerResponseShow
      : Show[DeregisterStreamConsumerResponse] = x =>
    ShowBuilder("DeregisterStreamConsumerRequest")
      .add("responseMetadata", x.responseMetadata())
      .build

  implicit val describeLimitsRequestShow: Show[DescribeLimitsRequest] = _ =>
    ShowBuilder("DescribeLimitsRequest").build

  implicit val describeLimitsResponseShow: Show[DescribeLimitsResponse] = x =>
    ShowBuilder("DescribeLimitsResponse")
      .add("onDemandStreamCount", x.onDemandStreamCount())
      .add("onDemandStreamCountLimit", x.onDemandStreamCountLimit())
      .add("openShardCount", x.openShardCount())
      .add("shardLimit", x.shardLimit())
      .add("responseMetadata", x.responseMetadata())
      .build

  implicit val describeStreamRequestShow: Show[DescribeStreamRequest] = x =>
    ShowBuilder("DescribeStreamRequest")
      .add("exclusiveStartShardId", x.exclusiveStartShardId())
      .add("limit", x.limit())
      .add("streamARN", x.streamARN())
      .add("streamName", x.streamName())
      .build

  implicit val describeStreamResponseShow: Show[DescribeStreamResponse] = x =>
    ShowBuilder("DescribeStreamResponse")
      .add("streamDescription", x.streamDescription())
      .add("responseMetadata", x.responseMetadata())
      .build

  implicit val describeStreamConsumerRequestShow
      : Show[DescribeStreamConsumerRequest] = x =>
    ShowBuilder("DescribeStreamConsumerRequest")
      .add("streamARN", x.streamARN())
      .add("consumerARN", x.consumerARN())
      .add("consumerName", x.consumerName())
      .build

  implicit val describeStreamConsumerResponseShow
      : Show[DescribeStreamConsumerResponse] = x =>
    ShowBuilder("DescribeStreamConsumerResponse")
      .add("consumerDescription", x.consumerDescription())
      .add("responseMetadata", x.responseMetadata())
      .build

  implicit val describeStreamSummaryRequestShow
      : Show[DescribeStreamSummaryRequest] = x =>
    ShowBuilder("DescribeStreamSummaryRequest")
      .add("streamARN", x.streamARN())
      .add("streamName", x.streamName())
      .build

  implicit val describeStreamSummaryResponseShow
      : Show[DescribeStreamSummaryResponse] = x =>
    ShowBuilder("DescribeStreamSummaryResponse")
      .add("streamDescriptionSummary", x.streamDescriptionSummary())
      .add("responseMetadata", x.responseMetadata())
      .build

  implicit val disableEnhancedMonitoringShow
      : Show[DisableEnhancedMonitoringRequest] = x =>
    ShowBuilder("DisableEnhancedMonitoringRequest")
      .add("shardLevelMetrics", x.shardLevelMetricsAsStrings())
      .add("streamName", x.streamName())
      .add("streamARN", x.streamARN())
      .build

  implicit val disableEnhancedMonitoringResponseShow
      : Show[DisableEnhancedMonitoringResponse] = x =>
    ShowBuilder("DisableEnhancedMonitoringResponse")
      .add("currentShardLevelMetrics", x.currentShardLevelMetricsAsStrings())
      .add("desiredShardLevelMetrics", x.desiredShardLevelMetricsAsStrings())
      .add("hasCurrentShardLevelMetrics", x.hasCurrentShardLevelMetrics())
      .add("hasDesiredShardLevelMetrics", x.hasDesiredShardLevelMetrics())
      .add("streamARN", x.streamARN())
      .add("streamName", x.streamName())
      .add("responseMetadata", x.responseMetadata())
      .build

  implicit val enableEnhancedMonitoringShow
      : Show[EnableEnhancedMonitoringRequest] = x =>
    ShowBuilder("EnableEnhancedMonitoringRequest")
      .add("shardLevelMetrics", x.shardLevelMetricsAsStrings())
      .add("streamName", x.streamName())
      .add("streamARN", x.streamARN())
      .build

  implicit val enableEnhancedMonitoringResponseShow
      : Show[EnableEnhancedMonitoringResponse] = x =>
    ShowBuilder("EnableEnhancedMonitoringResponse")
      .add("currentShardLevelMetrics", x.currentShardLevelMetricsAsStrings())
      .add("desiredShardLevelMetrics", x.desiredShardLevelMetricsAsStrings())
      .add("hasCurrentShardLevelMetrics", x.hasCurrentShardLevelMetrics())
      .add("hasDesiredShardLevelMetrics", x.hasDesiredShardLevelMetrics())
      .add("streamARN", x.streamARN())
      .add("streamName", x.streamName())
      .add("responseMetadata", x.responseMetadata())
      .build

  implicit val getRecordsRequestShow: Show[GetRecordsRequest] = x =>
    ShowBuilder("GetRecordsRequest")
      .add("limit", x.limit())
      .add("shardIterator", x.shardIterator())
      .add("streamARN", x.streamARN())
      .build

  implicit val getRecordsResponseShow: Show[GetRecordsResponse] = x =>
    ShowBuilder("GetRecordsResponse")
      .add("childShards", x.childShards())
      .add("hasChildShards", x.hasChildShards())
      .add("hasRecords", x.hasRecords())
      .add("millisBehindLatest", x.millisBehindLatest())
      .add("nextShardIterator", x.nextShardIterator())
      .add("records", x.records())
      .add("responseMetadata", x.responseMetadata())
      .build

  implicit val getShardIteratorRequestShow: Show[GetShardIteratorRequest] = x =>
    ShowBuilder("GetShardIteratorRequest")
      .add("shardId", x.shardId())
      .add("shardIteratorType", x.shardIteratorTypeAsString())
      .add("startingSequenceNumber", x.startingSequenceNumber())
      .add("streamARN", x.streamARN())
      .add("streamName", x.streamName())
      .add("timestamp", x.timestamp())
      .build

  implicit val getShardIteratorResponseShow: Show[GetShardIteratorResponse] =
    x =>
      ShowBuilder("GetShardIteratorResponse")
        .add("shardIterator", x.shardIterator())
        .build

  implicit val increaseStreamRetentionPeriodRequestShow
      : Show[IncreaseStreamRetentionPeriodRequest] = x =>
    ShowBuilder("IncreaseStreamRetentionPeriodRequest")
      .add("streamARN", x.streamARN())
      .add("streamName", x.streamName())
      .add("retentionPeriodHours", x.retentionPeriodHours())
      .build

  implicit val increaseStreamRetentionPeriodResponseShow
      : Show[IncreaseStreamRetentionPeriodResponse] = x =>
    ShowBuilder("IncreaseStreamRetentionPeriodResponse")
      .add("responseMetadata", x.responseMetadata())
      .build

  implicit val listShardsRequestShow: Show[ListShardsRequest] = x =>
    ShowBuilder("ListShardsRequest")
      .add("maxResults", x.maxResults())
      .add("nextToken", x.nextToken())
      .add("shardFilter", x.shardFilter())
      .add("streamARN", x.streamARN())
      .add("streamCreationTimestamp", x.streamCreationTimestamp())
      .add("streamName", x.streamName())
      .build

  implicit val listShardsResponseShow: Show[ListShardsResponse] = x =>
    ShowBuilder("ListShardsResponse")
      .add("hasShards", x.hasShards())
      .add("nextToken", x.nextToken())
      .add("shards", x.shards())
      .add("responseMetadata", x.responseMetadata())
      .build

  implicit val listStreamConsumersRequestShow
      : Show[ListStreamConsumersRequest] = x =>
    ShowBuilder("ListStreamConsumersRequest")
      .add("maxResults", x.maxResults())
      .add("nextToken", x.nextToken())
      .add("streamARN", x.streamARN())
      .add("streamCreationTimestamp", x.streamCreationTimestamp())
      .build

  implicit val listStreamConsumersResponseShow
      : Show[ListStreamConsumersResponse] = x =>
    ShowBuilder("ListStreamConsumersResponse")
      .add("consumers", x.consumers())
      .add("hasConsumers", x.hasConsumers())
      .add("nextToken", x.nextToken())
      .add("responseMetadata", x.responseMetadata())
      .build

  implicit val listStreamsRequestShow: Show[ListStreamsRequest] = x =>
    ShowBuilder("ListStreamsRequest")
      .add("exclusiveStartStreamName", x.exclusiveStartStreamName())
      .add("limit", x.limit())
      .add("nextToken", x.nextToken())
      .build

  implicit val listStreamsResponseShow: Show[ListStreamsResponse] = x =>
    ShowBuilder("ListStreamsResponse")
      .add("hasMoreStreams", x.hasMoreStreams())
      .add("hasStreamNames", x.hasStreamNames())
      .add("hasStreamSummaries", x.hasStreamSummaries())
      .add("nextToken", x.nextToken())
      .add("streamNames", x.streamNames())
      .add("streamSummaries", x.streamSummaries())
      .add("responseMetadata", x.responseMetadata())
      .build

  implicit val listTagsForStreamRequestShow: Show[ListTagsForStreamRequest] =
    x =>
      ShowBuilder("ListTagsForStreamRequest")
        .add("exclusiveStartTagKey", x.exclusiveStartTagKey())
        .add("limit", x.limit())
        .add("streamARN", x.streamARN())
        .add("streamName", x.streamName())
        .build

  implicit val listTagsForStreamResponseShow: Show[ListTagsForStreamResponse] =
    x =>
      ShowBuilder("ListTagsForStreamResponse")
        .add("hasMoreTags", x.hasMoreTags())
        .add("hasTags", x.hasTags())
        .add("tags", x.tags())
        .add("responseMetadata", x.responseMetadata())
        .build

  implicit val mergeShardsRequestShow: Show[MergeShardsRequest] =
    x =>
      ShowBuilder("MergeShardsRequest")
        .add("adjacentShardToMerge", x.adjacentShardToMerge())
        .add("shardToMerge", x.shardToMerge())
        .add("streamARN", x.streamARN())
        .add("streamName", x.streamName())
        .build

  implicit val mergeShardsResponseShow: Show[MergeShardsResponse] =
    x =>
      ShowBuilder("MergeShardsResponse")
        .add("responseMetadata", x.responseMetadata())
        .build

  implicit val putRecordRequestShow: Show[PutRecordRequest] =
    x =>
      ShowBuilder("PutRecordRequest")
        .add("data", x.data())
        .add("explicitHashKey", x.explicitHashKey())
        .add("partitionKey", x.partitionKey())
        .add("sequenceNumberForOrdering", x.sequenceNumberForOrdering())
        .add("streamARN", x.streamARN())
        .add("streamName", x.streamName())
        .build

  implicit val putRecordResponseShow: Show[PutRecordResponse] =
    x =>
      ShowBuilder("PutRecordResponse")
        .add("encryptionType", x.encryptionTypeAsString())
        .add("sequenceNumber", x.sequenceNumber())
        .add("shardId", x.shardId())
        .add("responseMetadata", x.responseMetadata())
        .build

  implicit val putRecordsRequestShow: Show[PutRecordsRequest] =
    x =>
      ShowBuilder("PutRecordsRequest")
        .add("hasRecords", x.hasRecords())
        .add("records", x.records())
        .add("streamARN", x.streamARN())
        .add("streamName", x.streamName())
        .build

  implicit val putRecordsResponseShow: Show[PutRecordsResponse] =
    x =>
      ShowBuilder("PutRecordsResponse")
        .add("encryptionType", x.encryptionTypeAsString())
        .add("failedRecordCount", x.failedRecordCount())
        .add("hasRecords", x.hasRecords())
        .add("records", x.records())
        .add("responseMetadata", x.responseMetadata())
        .build

  implicit val registerStreamConsumerRequestShow
      : Show[RegisterStreamConsumerRequest] =
    x =>
      ShowBuilder("RegisterStreamConsumerRequest")
        .add("consumerName", x.consumerName())
        .add("streamARN", x.streamARN())
        .build

  implicit val registerStreamConsumerResponseShow
      : Show[RegisterStreamConsumerResponse] =
    x =>
      ShowBuilder("RegisterStreamConsumerResponse")
        .add("consumer", x.consumer())
        .add("responseMetadata", x.responseMetadata())
        .build

  implicit val removeTagsFromStreamRequestShow
      : Show[RemoveTagsFromStreamRequest] = x =>
    ShowBuilder("RemoveTagsFromStreamRequest")
      .add("hasTagKeys", x.hasTagKeys())
      .add("streamName", x.streamName())
      .add("streamARN", x.streamARN())
      .add("tagKeys", x.tagKeys())
      .build

  implicit val removeTagsFromStreamResponseShow
      : Show[RemoveTagsFromStreamResponse] = x =>
    ShowBuilder("RemoveTagsFromStreamResponse")
      .add("responseMetadata", x.responseMetadata())
      .build

  implicit val splitShardRequestShow: Show[SplitShardRequest] = x =>
    ShowBuilder("SplitShardRequest")
      .add("newStartingHashKey", x.newStartingHashKey())
      .add("shardToSplit", x.shardToSplit())
      .add("streamName", x.streamName())
      .add("streamARN", x.streamARN())
      .build

  implicit val splitShardResponseShow: Show[SplitShardResponse] = x =>
    ShowBuilder("SplitShardResponse")
      .add("responseMetadata", x.responseMetadata())
      .build

  implicit val startStreamEncryptionRequestShow
      : Show[StartStreamEncryptionRequest] = x =>
    ShowBuilder("StartStreamEncryptionRequest")
      .add("encryptionTypeAsString", x.encryptionTypeAsString())
      .add("keyId", x.keyId())
      .add("streamName", x.streamName())
      .add("streamARN", x.streamARN())
      .build

  implicit val startStreamEncryptionResponseShow
      : Show[StartStreamEncryptionResponse] = x =>
    ShowBuilder("StartStreamEncryptionResponse")
      .add("responseMetadata", x.responseMetadata())
      .build

  implicit val stopStreamEncryptionRequestShow
      : Show[StopStreamEncryptionRequest] = x =>
    ShowBuilder("StopStreamEncryptionRequest")
      .add("encryptionTypeAsString", x.encryptionTypeAsString())
      .add("keyId", x.keyId())
      .add("streamName", x.streamName())
      .add("streamARN", x.streamARN())
      .build

  implicit val stopStreamEncryptionResponseShow
      : Show[StopStreamEncryptionResponse] = x =>
    ShowBuilder("StopStreamEncryptionResponse")
      .add("responseMetadata", x.responseMetadata())
      .build

  implicit val subscribeToShardRequestShow: Show[SubscribeToShardRequest] = x =>
    ShowBuilder("SubscribeToShardRequest")
      .add("consumerARN", x.consumerARN())
      .add("shardId", x.shardId())
      .add("startingPosition", x.startingPosition())
      .build

  implicit val subscribeToShardResponseShow: Show[SubscribeToShardResponse] =
    x =>
      ShowBuilder("SubscribeToShardResponse")
        .add("responseMetadata", x.responseMetadata())
        .build

  implicit val updateShardCountRequestShow: Show[UpdateShardCountRequest] = x =>
    ShowBuilder("UpdateShardCountRequest")
      .add("scalingType", x.scalingTypeAsString())
      .add("streamARN", x.streamARN())
      .add("streamName", x.streamName())
      .add("targetShardCount", x.targetShardCount())
      .build

  implicit val updateShardCountResponseShow: Show[UpdateShardCountResponse] =
    x =>
      ShowBuilder("UpdateShardCountResponse")
        .add("currentShardCount", x.currentShardCount())
        .add("streamARN", x.streamARN())
        .add("streamName", x.streamName())
        .add("targetShardCount", x.targetShardCount())
        .add("responseMetadata", x.responseMetadata())
        .build

  implicit val updateStreamModeRequestShow: Show[UpdateStreamModeRequest] = x =>
    ShowBuilder("UpdateStreamModeRequest")
      .add("streamARN", x.streamARN())
      .add("streamModeDetails", x.streamModeDetails())
      .build

  implicit val updateStreamModeResponseShow: Show[UpdateStreamModeResponse] =
    x =>
      ShowBuilder("UpdateStreamModeResponse")
        .add("responseMetadata", x.responseMetadata())
        .build

  implicit val kinesisClientLogEncoders: KinesisClient.LogEncoders =
    new KinesisClient.LogEncoders()

}
