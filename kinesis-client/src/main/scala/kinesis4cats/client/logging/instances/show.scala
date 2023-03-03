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
package client
package logging.instances

import scala.jdk.CollectionConverters._

import java.nio.ByteBuffer

import cats.Eval
import cats.Show
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.cloudwatch.{model => cw}
import software.amazon.awssdk.services.dynamodb.{model => ddb}
import software.amazon.awssdk.services.kinesis.{model => kin}

import kinesis4cats.logging.instances.show._

/** KinesisClient [[kinesis4cats.logging.LogEncoder LogEncoder]] instances for
  * string encoding of log structures using [[cats.Show Show]]
  */
object show {

  implicit val kinesisResponseMetadataShow: Show[kin.KinesisResponseMetadata] =
    x =>
      ShowBuilder("KinesisResponseMetadata")
        .add("extendedRequestId", x.extendedRequestId())
        .add("requestId", x.requestId())
        .build

  implicit val streamModeDetailsShow: Show[kin.StreamModeDetails] = x =>
    ShowBuilder("StreamModeDetails")
      .add("streamMode", x.streamModeAsString())
      .build

  implicit val enhancedMonitoringShow: Show[kin.EnhancedMetrics] = x =>
    ShowBuilder("EnhancedMetrics")
      .add("hasShardLevelMetrics", x.hasShardLevelMetrics())
      .add("shardLevelMetrics", x.shardLevelMetricsAsStrings())
      .build

  implicit val hashKeyRangeShow: Show[kin.HashKeyRange] = x =>
    ShowBuilder("HashKeyRange")
      .add("endingHashKey", x.endingHashKey())
      .add("startingHashKey", x.startingHashKey())
      .build

  implicit val sequenceNumberRangeShow: Show[kin.SequenceNumberRange] = x =>
    ShowBuilder("SequenceNumberRange")
      .add("endingSequenceNumber", x.endingSequenceNumber())
      .add("startingSequenceNumber", x.startingSequenceNumber())
      .build

  implicit val shardShow: Show[kin.Shard] = x =>
    ShowBuilder("Shard")
      .add("adjacentParentShardId", x.adjacentParentShardId())
      .add("hashKeyRange", x.hashKeyRange())
      .add("parentShardId", x.parentShardId())
      .add("sequenceNumberRange", x.sequenceNumberRange())
      .add("shardId", x.shardId())
      .build

  implicit val streamDescriptionShow: Show[kin.StreamDescription] = x =>
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

  implicit val streamDescriptionSummaryShow
      : Show[kin.StreamDescriptionSummary] =
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

  implicit val consumerDescriptionShow: Show[kin.ConsumerDescription] =
    x =>
      ShowBuilder("ConsumerDescription")
        .add("consumerARN", x.consumerARN())
        .add("consumerCreationTimestamp", x.consumerCreationTimestamp())
        .add("consumerName", x.consumerName())
        .add("consumerStatus", x.consumerStatusAsString())
        .add("streamARN", x.streamARN())
        .build

  implicit val childShardShow: Show[kin.ChildShard] = x =>
    ShowBuilder("ChildShard")
      .add("hasParentShards", x.hasParentShards())
      .add("hashKeyRange", x.hashKeyRange())
      .add("parentShards", x.parentShards())
      .add("shardId", x.shardId())
      .build

  implicit val consumerShow: Show[kin.Consumer] = x =>
    ShowBuilder("Consumer")
      .add("consumerARN", x.consumerARN())
      .add("consumerCreationTimestamp", x.consumerCreationTimestamp())
      .add("consumerName", x.consumerName())
      .add("consumerStatus", x.consumerStatusAsString())
      .build

  implicit val sdkBytesShow: Show[SdkBytes] = x =>
    Show[ByteBuffer].show(x.asByteBuffer())

  implicit val recordShow: Show[kin.Record] = x =>
    ShowBuilder("Record")
      .add("approximateArrivalTimestamp", x.approximateArrivalTimestamp())
      .add("data", x.data())
      .add("encryptionTypeAsString", x.encryptionTypeAsString())
      .add("partitionKey", x.partitionKey())
      .add("sequenceNumber", x.sequenceNumber())
      .build

  implicit val shardFilterShow: Show[kin.ShardFilter] = x =>
    ShowBuilder("ShardFilter")
      .add("shardId", x.shardId())
      .add("timestamp", x.timestamp())
      .add("type", x.typeAsString())
      .build

  implicit val streamSummaryShow: Show[kin.StreamSummary] = x =>
    ShowBuilder("StreamSummary")
      .add("streamARN", x.streamARN())
      .add("streamCreationTimestamp", x.streamCreationTimestamp())
      .add("streamModeDetails", x.streamModeDetails())
      .add("streamName", x.streamName())
      .add("streamStatus", x.streamStatusAsString())
      .build

  implicit val tagShow: Show[kin.Tag] = x =>
    ShowBuilder("Tag")
      .add("key", x.key())
      .add("value", x.value())
      .build

  implicit val putRecordsRequestEntryShow: Show[kin.PutRecordsRequestEntry] =
    x =>
      ShowBuilder("PutRecordsRequestEntry")
        .add("data", x.data())
        .add("explicitHashKey", x.explicitHashKey())
        .add("partitionKey", x.partitionKey())
        .build

  implicit val putRecordsResultEntryShow: Show[kin.PutRecordsResultEntry] = x =>
    ShowBuilder("PutRecordsResultEntry")
      .add("errorCode", x.errorCode())
      .add("errorMessage", x.errorMessage())
      .add("sequenceNumber", x.sequenceNumber())
      .add("shardId", x.shardId())
      .build

  implicit val startingPositionShow: Show[kin.StartingPosition] = x =>
    ShowBuilder("StartingPosition")
      .add("sequenceNumber", x.sequenceNumber())
      .add("timestamp", x.timestamp())
      .add("typeAsString", x.typeAsString())
      .build

  implicit val addTagsToStreamRequestShow: Show[kin.AddTagsToStreamRequest] =
    x =>
      ShowBuilder("AddTagsToStreamRequest")
        .add("streamName", x.streamName())
        .add("tags", x.tags())
        .add("streamARN", x.streamARN())
        .build

  implicit val addTagsToStreamResponseShow: Show[kin.AddTagsToStreamResponse] =
    x =>
      ShowBuilder("AddTagsToStreamResponse")
        .add("responseMetadata", x.responseMetadata())
        .build

  implicit val createStreamRequestShow: Show[kin.CreateStreamRequest] = x =>
    ShowBuilder("CreateStreamRequest")
      .add("streamName", x.streamName())
      .add("shardCount", x.shardCount())
      .add("streamModeDetails", x.streamModeDetails())
      .build

  implicit val createStreamResponseShow: Show[kin.CreateStreamResponse] = x =>
    ShowBuilder("CreateStreamResponse")
      .add("responseMetadata", x.responseMetadata())
      .build

  implicit val decreaseStreamRetentionPeriodRequestShow
      : Show[kin.DecreaseStreamRetentionPeriodRequest] = x =>
    ShowBuilder("DecreaseStreamRetentionPeriodRequest")
      .add("streamARN", x.streamARN())
      .add("streamName", x.streamName())
      .add("retentionPeriodHours", x.retentionPeriodHours())
      .build

  implicit val decreaseStreamRetentionPeriodResponseShow
      : Show[kin.DecreaseStreamRetentionPeriodResponse] = x =>
    ShowBuilder("DecreaseStreamRetentionPeriodResponse")
      .add("responseMetadata", x.responseMetadata())
      .build

  implicit val deleteStreamRequestShow: Show[kin.DeleteStreamRequest] = x =>
    ShowBuilder("DeleteStreamRequest")
      .add("streamARN", x.streamARN())
      .add("streamName", x.streamName())
      .add("enforceConsumerDeletion", x.enforceConsumerDeletion())
      .build

  implicit val deleteStreamResponseShow: Show[kin.DeleteStreamResponse] = x =>
    ShowBuilder("DeleteStreamResponse")
      .add("responseMetadata", x.responseMetadata())
      .build

  implicit val deregisterStreamConsumerRequestShow
      : Show[kin.DeregisterStreamConsumerRequest] = x =>
    ShowBuilder("DeregisterStreamConsumerRequest")
      .add("streamARN", x.streamARN())
      .add("consumerName", x.consumerName())
      .add("consumerARN", x.consumerARN())
      .build

  implicit val deregisterStreamConsumerResponseShow
      : Show[kin.DeregisterStreamConsumerResponse] = x =>
    ShowBuilder("DeregisterStreamConsumerRequest")
      .add("responseMetadata", x.responseMetadata())
      .build

  implicit val describeLimitsRequestShow: Show[kin.DescribeLimitsRequest] = _ =>
    ShowBuilder("DescribeLimitsRequest").build

  implicit val describeLimitsResponseShow: Show[kin.DescribeLimitsResponse] =
    x =>
      ShowBuilder("DescribeLimitsResponse")
        .add("onDemandStreamCount", x.onDemandStreamCount())
        .add("onDemandStreamCountLimit", x.onDemandStreamCountLimit())
        .add("openShardCount", x.openShardCount())
        .add("shardLimit", x.shardLimit())
        .add("responseMetadata", x.responseMetadata())
        .build

  implicit val describeStreamRequestShow: Show[kin.DescribeStreamRequest] = x =>
    ShowBuilder("DescribeStreamRequest")
      .add("exclusiveStartShardId", x.exclusiveStartShardId())
      .add("limit", x.limit())
      .add("streamARN", x.streamARN())
      .add("streamName", x.streamName())
      .build

  implicit val describeStreamResponseShow: Show[kin.DescribeStreamResponse] =
    x =>
      ShowBuilder("DescribeStreamResponse")
        .add("streamDescription", x.streamDescription())
        .add("responseMetadata", x.responseMetadata())
        .build

  implicit val describeStreamConsumerRequestShow
      : Show[kin.DescribeStreamConsumerRequest] = x =>
    ShowBuilder("DescribeStreamConsumerRequest")
      .add("streamARN", x.streamARN())
      .add("consumerARN", x.consumerARN())
      .add("consumerName", x.consumerName())
      .build

  implicit val describeStreamConsumerResponseShow
      : Show[kin.DescribeStreamConsumerResponse] = x =>
    ShowBuilder("DescribeStreamConsumerResponse")
      .add("consumerDescription", x.consumerDescription())
      .add("responseMetadata", x.responseMetadata())
      .build

  implicit val describeStreamSummaryRequestShow
      : Show[kin.DescribeStreamSummaryRequest] = x =>
    ShowBuilder("DescribeStreamSummaryRequest")
      .add("streamARN", x.streamARN())
      .add("streamName", x.streamName())
      .build

  implicit val describeStreamSummaryResponseShow
      : Show[kin.DescribeStreamSummaryResponse] = x =>
    ShowBuilder("DescribeStreamSummaryResponse")
      .add("streamDescriptionSummary", x.streamDescriptionSummary())
      .add("responseMetadata", x.responseMetadata())
      .build

  implicit val disableEnhancedMonitoringShow
      : Show[kin.DisableEnhancedMonitoringRequest] = x =>
    ShowBuilder("DisableEnhancedMonitoringRequest")
      .add("shardLevelMetrics", x.shardLevelMetricsAsStrings())
      .add("streamName", x.streamName())
      .add("streamARN", x.streamARN())
      .build

  implicit val disableEnhancedMonitoringResponseShow
      : Show[kin.DisableEnhancedMonitoringResponse] = x =>
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
      : Show[kin.EnableEnhancedMonitoringRequest] = x =>
    ShowBuilder("EnableEnhancedMonitoringRequest")
      .add("shardLevelMetrics", x.shardLevelMetricsAsStrings())
      .add("streamName", x.streamName())
      .add("streamARN", x.streamARN())
      .build

  implicit val enableEnhancedMonitoringResponseShow
      : Show[kin.EnableEnhancedMonitoringResponse] = x =>
    ShowBuilder("EnableEnhancedMonitoringResponse")
      .add("currentShardLevelMetrics", x.currentShardLevelMetricsAsStrings())
      .add("desiredShardLevelMetrics", x.desiredShardLevelMetricsAsStrings())
      .add("hasCurrentShardLevelMetrics", x.hasCurrentShardLevelMetrics())
      .add("hasDesiredShardLevelMetrics", x.hasDesiredShardLevelMetrics())
      .add("streamARN", x.streamARN())
      .add("streamName", x.streamName())
      .add("responseMetadata", x.responseMetadata())
      .build

  implicit val getRecordsRequestShow: Show[kin.GetRecordsRequest] = x =>
    ShowBuilder("GetRecordsRequest")
      .add("limit", x.limit())
      .add("shardIterator", x.shardIterator())
      .add("streamARN", x.streamARN())
      .build

  implicit val getRecordsResponseShow: Show[kin.GetRecordsResponse] = x =>
    ShowBuilder("GetRecordsResponse")
      .add("childShards", x.childShards())
      .add("hasChildShards", x.hasChildShards())
      .add("hasRecords", x.hasRecords())
      .add("millisBehindLatest", x.millisBehindLatest())
      .add("nextShardIterator", x.nextShardIterator())
      .add("records", x.records())
      .add("responseMetadata", x.responseMetadata())
      .build

  implicit val getShardIteratorRequestShow: Show[kin.GetShardIteratorRequest] =
    x =>
      ShowBuilder("GetShardIteratorRequest")
        .add("shardId", x.shardId())
        .add("shardIteratorType", x.shardIteratorTypeAsString())
        .add("startingSequenceNumber", x.startingSequenceNumber())
        .add("streamARN", x.streamARN())
        .add("streamName", x.streamName())
        .add("timestamp", x.timestamp())
        .build

  implicit val getShardIteratorResponseShow
      : Show[kin.GetShardIteratorResponse] =
    x =>
      ShowBuilder("GetShardIteratorResponse")
        .add("shardIterator", x.shardIterator())
        .build

  implicit val increaseStreamRetentionPeriodRequestShow
      : Show[kin.IncreaseStreamRetentionPeriodRequest] = x =>
    ShowBuilder("IncreaseStreamRetentionPeriodRequest")
      .add("streamARN", x.streamARN())
      .add("streamName", x.streamName())
      .add("retentionPeriodHours", x.retentionPeriodHours())
      .build

  implicit val increaseStreamRetentionPeriodResponseShow
      : Show[kin.IncreaseStreamRetentionPeriodResponse] = x =>
    ShowBuilder("IncreaseStreamRetentionPeriodResponse")
      .add("responseMetadata", x.responseMetadata())
      .build

  implicit val listShardsRequestShow: Show[kin.ListShardsRequest] = x =>
    ShowBuilder("ListShardsRequest")
      .add("maxResults", x.maxResults())
      .add("nextToken", x.nextToken())
      .add("shardFilter", x.shardFilter())
      .add("streamARN", x.streamARN())
      .add("streamCreationTimestamp", x.streamCreationTimestamp())
      .add("streamName", x.streamName())
      .build

  implicit val listShardsResponseShow: Show[kin.ListShardsResponse] = x =>
    ShowBuilder("ListShardsResponse")
      .add("hasShards", x.hasShards())
      .add("nextToken", x.nextToken())
      .add("shards", x.shards())
      .add("responseMetadata", x.responseMetadata())
      .build

  implicit val listStreamConsumersRequestShow
      : Show[kin.ListStreamConsumersRequest] = x =>
    ShowBuilder("ListStreamConsumersRequest")
      .add("maxResults", x.maxResults())
      .add("nextToken", x.nextToken())
      .add("streamARN", x.streamARN())
      .add("streamCreationTimestamp", x.streamCreationTimestamp())
      .build

  implicit val listStreamConsumersResponseShow
      : Show[kin.ListStreamConsumersResponse] = x =>
    ShowBuilder("ListStreamConsumersResponse")
      .add("consumers", x.consumers())
      .add("hasConsumers", x.hasConsumers())
      .add("nextToken", x.nextToken())
      .add("responseMetadata", x.responseMetadata())
      .build

  implicit val listStreamsRequestShow: Show[kin.ListStreamsRequest] = x =>
    ShowBuilder("ListStreamsRequest")
      .add("exclusiveStartStreamName", x.exclusiveStartStreamName())
      .add("limit", x.limit())
      .add("nextToken", x.nextToken())
      .build

  implicit val listStreamsResponseShow: Show[kin.ListStreamsResponse] = x =>
    ShowBuilder("ListStreamsResponse")
      .add("hasMoreStreams", x.hasMoreStreams())
      .add("hasStreamNames", x.hasStreamNames())
      .add("hasStreamSummaries", x.hasStreamSummaries())
      .add("nextToken", x.nextToken())
      .add("streamNames", x.streamNames())
      .add("streamSummaries", x.streamSummaries())
      .add("responseMetadata", x.responseMetadata())
      .build

  implicit val listTagsForStreamRequestShow
      : Show[kin.ListTagsForStreamRequest] =
    x =>
      ShowBuilder("ListTagsForStreamRequest")
        .add("exclusiveStartTagKey", x.exclusiveStartTagKey())
        .add("limit", x.limit())
        .add("streamARN", x.streamARN())
        .add("streamName", x.streamName())
        .build

  implicit val listTagsForStreamResponseShow
      : Show[kin.ListTagsForStreamResponse] =
    x =>
      ShowBuilder("ListTagsForStreamResponse")
        .add("hasMoreTags", x.hasMoreTags())
        .add("hasTags", x.hasTags())
        .add("tags", x.tags())
        .add("responseMetadata", x.responseMetadata())
        .build

  implicit val mergeShardsRequestShow: Show[kin.MergeShardsRequest] =
    x =>
      ShowBuilder("MergeShardsRequest")
        .add("adjacentShardToMerge", x.adjacentShardToMerge())
        .add("shardToMerge", x.shardToMerge())
        .add("streamARN", x.streamARN())
        .add("streamName", x.streamName())
        .build

  implicit val mergeShardsResponseShow: Show[kin.MergeShardsResponse] =
    x =>
      ShowBuilder("MergeShardsResponse")
        .add("responseMetadata", x.responseMetadata())
        .build

  implicit val putRecordRequestShow: Show[kin.PutRecordRequest] =
    x =>
      ShowBuilder("PutRecordRequest")
        .add("data", x.data())
        .add("explicitHashKey", x.explicitHashKey())
        .add("partitionKey", x.partitionKey())
        .add("sequenceNumberForOrdering", x.sequenceNumberForOrdering())
        .add("streamARN", x.streamARN())
        .add("streamName", x.streamName())
        .build

  implicit val putRecordResponseShow: Show[kin.PutRecordResponse] =
    x =>
      ShowBuilder("PutRecordResponse")
        .add("encryptionType", x.encryptionTypeAsString())
        .add("sequenceNumber", x.sequenceNumber())
        .add("shardId", x.shardId())
        .add("responseMetadata", x.responseMetadata())
        .build

  implicit val putRecordsRequestShow: Show[kin.PutRecordsRequest] =
    x =>
      ShowBuilder("PutRecordsRequest")
        .add("hasRecords", x.hasRecords())
        .add("records", x.records())
        .add("streamARN", x.streamARN())
        .add("streamName", x.streamName())
        .build

  implicit val putRecordsResponseShow: Show[kin.PutRecordsResponse] =
    x =>
      ShowBuilder("PutRecordsResponse")
        .add("encryptionType", x.encryptionTypeAsString())
        .add("failedRecordCount", x.failedRecordCount())
        .add("hasRecords", x.hasRecords())
        .add("records", x.records())
        .add("responseMetadata", x.responseMetadata())
        .build

  implicit val registerStreamConsumerRequestShow
      : Show[kin.RegisterStreamConsumerRequest] =
    x =>
      ShowBuilder("RegisterStreamConsumerRequest")
        .add("consumerName", x.consumerName())
        .add("streamARN", x.streamARN())
        .build

  implicit val registerStreamConsumerResponseShow
      : Show[kin.RegisterStreamConsumerResponse] =
    x =>
      ShowBuilder("RegisterStreamConsumerResponse")
        .add("consumer", x.consumer())
        .add("responseMetadata", x.responseMetadata())
        .build

  implicit val removeTagsFromStreamRequestShow
      : Show[kin.RemoveTagsFromStreamRequest] = x =>
    ShowBuilder("RemoveTagsFromStreamRequest")
      .add("hasTagKeys", x.hasTagKeys())
      .add("streamName", x.streamName())
      .add("streamARN", x.streamARN())
      .add("tagKeys", x.tagKeys())
      .build

  implicit val removeTagsFromStreamResponseShow
      : Show[kin.RemoveTagsFromStreamResponse] = x =>
    ShowBuilder("RemoveTagsFromStreamResponse")
      .add("responseMetadata", x.responseMetadata())
      .build

  implicit val splitShardRequestShow: Show[kin.SplitShardRequest] = x =>
    ShowBuilder("SplitShardRequest")
      .add("newStartingHashKey", x.newStartingHashKey())
      .add("shardToSplit", x.shardToSplit())
      .add("streamName", x.streamName())
      .add("streamARN", x.streamARN())
      .build

  implicit val splitShardResponseShow: Show[kin.SplitShardResponse] = x =>
    ShowBuilder("SplitShardResponse")
      .add("responseMetadata", x.responseMetadata())
      .build

  implicit val startStreamEncryptionRequestShow
      : Show[kin.StartStreamEncryptionRequest] = x =>
    ShowBuilder("StartStreamEncryptionRequest")
      .add("encryptionTypeAsString", x.encryptionTypeAsString())
      .add("keyId", x.keyId())
      .add("streamName", x.streamName())
      .add("streamARN", x.streamARN())
      .build

  implicit val startStreamEncryptionResponseShow
      : Show[kin.StartStreamEncryptionResponse] = x =>
    ShowBuilder("StartStreamEncryptionResponse")
      .add("responseMetadata", x.responseMetadata())
      .build

  implicit val stopStreamEncryptionRequestShow
      : Show[kin.StopStreamEncryptionRequest] = x =>
    ShowBuilder("StopStreamEncryptionRequest")
      .add("encryptionTypeAsString", x.encryptionTypeAsString())
      .add("keyId", x.keyId())
      .add("streamName", x.streamName())
      .add("streamARN", x.streamARN())
      .build

  implicit val stopStreamEncryptionResponseShow
      : Show[kin.StopStreamEncryptionResponse] = x =>
    ShowBuilder("StopStreamEncryptionResponse")
      .add("responseMetadata", x.responseMetadata())
      .build

  implicit val subscribeToShardRequestShow: Show[kin.SubscribeToShardRequest] =
    x =>
      ShowBuilder("SubscribeToShardRequest")
        .add("consumerARN", x.consumerARN())
        .add("shardId", x.shardId())
        .add("startingPosition", x.startingPosition())
        .build

  implicit val subscribeToShardResponseShow
      : Show[kin.SubscribeToShardResponse] =
    x =>
      ShowBuilder("SubscribeToShardResponse")
        .add("responseMetadata", x.responseMetadata())
        .build

  implicit val updateShardCountRequestShow: Show[kin.UpdateShardCountRequest] =
    x =>
      ShowBuilder("UpdateShardCountRequest")
        .add("scalingType", x.scalingTypeAsString())
        .add("streamARN", x.streamARN())
        .add("streamName", x.streamName())
        .add("targetShardCount", x.targetShardCount())
        .build

  implicit val updateShardCountResponseShow
      : Show[kin.UpdateShardCountResponse] =
    x =>
      ShowBuilder("UpdateShardCountResponse")
        .add("currentShardCount", x.currentShardCount())
        .add("streamARN", x.streamARN())
        .add("streamName", x.streamName())
        .add("targetShardCount", x.targetShardCount())
        .add("responseMetadata", x.responseMetadata())
        .build

  implicit val updateStreamModeRequestShow: Show[kin.UpdateStreamModeRequest] =
    x =>
      ShowBuilder("UpdateStreamModeRequest")
        .add("streamARN", x.streamARN())
        .add("streamModeDetails", x.streamModeDetails())
        .build

  implicit val updateStreamModeResponseShow
      : Show[kin.UpdateStreamModeResponse] =
    x =>
      ShowBuilder("UpdateStreamModeResponse")
        .add("responseMetadata", x.responseMetadata())
        .build

  implicit val kinesisClientLogEncoders: KinesisClient.LogEncoders =
    new KinesisClient.LogEncoders()

  implicit val attributeDefinitionShow: Show[ddb.AttributeDefinition] = x =>
    ShowBuilder("AttributeDefinition")
      .add("attributeName", x.attributeName())
      .add("attributeType", x.attributeTypeAsString())
      .build

  implicit val keySchemaElementShow: Show[ddb.KeySchemaElement] = x =>
    ShowBuilder("KeySchemaElement")
      .add("attributeName", x.attributeName())
      .add("keyType", x.keyTypeAsString())
      .build

  implicit val projectionShow: Show[ddb.Projection] = x =>
    ShowBuilder("Projection")
      .add("hasNonKeyAttributes", x.hasNonKeyAttributes())
      .add("nonKeyAttributes", x.nonKeyAttributes())
      .add("projectionType", x.projectionTypeAsString())
      .build

  implicit val provisionedThroughputShow: Show[ddb.ProvisionedThroughput] = x =>
    ShowBuilder("ProvisionedThroughput")
      .add("readCapacityUnits", x.readCapacityUnits())
      .add("writeCapacityUnits", x.writeCapacityUnits())
      .build

  implicit val globalSecondaryIndexShow: Show[ddb.GlobalSecondaryIndex] = x =>
    ShowBuilder("GlobalSecondaryIndex")
      .add("hasKeySchema", x.hasKeySchema())
      .add("indexName", x.indexName())
      .add("keySchema", x.keySchema())
      .add("projection", x.projection())
      .add("provisionedThroughput", x.provisionedThroughput())
      .build

  implicit val localSecondaryIndexShow: Show[ddb.LocalSecondaryIndex] = x =>
    ShowBuilder("LocalSecondaryIndex")
      .add("hasKeySchema", x.hasKeySchema())
      .add("indexName", x.indexName())
      .add("keySchema", x.keySchema())
      .add("projection", x.projection())
      .build

  implicit val sseSpecificationShow: Show[ddb.SSESpecification] = x =>
    ShowBuilder("SSESpecification")
      .add("enabled", x.enabled())
      .add("kmsMasterKeyId", x.kmsMasterKeyId())
      .add("sseType", x.sseTypeAsString())
      .build

  implicit val streamSpecificationShow: Show[ddb.StreamSpecification] = x =>
    ShowBuilder("StreamSpecification")
      .add("streamEnabled", x.streamEnabled())
      .add("streamViewType", x.streamViewTypeAsString())
      .build

  implicit val ddbTagShow: Show[ddb.Tag] = x =>
    ShowBuilder("Tag")
      .add("key", x.key())
      .add("value", x.value())
      .build

  implicit val archivalSummaryShow: Show[ddb.ArchivalSummary] = x =>
    ShowBuilder("ArchivalSummary")
      .add("archivalBackupArn", x.archivalBackupArn())
      .add("archivalDateTime", x.archivalDateTime())
      .add("archivalReason", x.archivalReason())
      .build

  implicit val billingModeSummaryShow: Show[ddb.BillingModeSummary] = x =>
    ShowBuilder("BillingModeSummary")
      .add("billingMode", x.billingModeAsString())
      .add(
        "lastUpdateToPayPerRequestDateTime",
        x.lastUpdateToPayPerRequestDateTime()
      )
      .build

  implicit val provisionedThroughputDescriptionShow
      : Show[ddb.ProvisionedThroughputDescription] = x =>
    ShowBuilder("ProvisionedThroughputDescription")
      .add("lastDecreaseDateTime", x.lastDecreaseDateTime())
      .add("lastIncreaseDateTime", x.lastIncreaseDateTime())
      .add("numberOfDecreasesToday", x.numberOfDecreasesToday())
      .add("readCapacityUnits", x.readCapacityUnits())
      .add("writeCapacityUnits", x.writeCapacityUnits())
      .build

  implicit val globalSecondaryIndexDescriptionShow
      : Show[ddb.GlobalSecondaryIndexDescription] = x =>
    ShowBuilder("GlobalSecondaryIndexDescription")
      .add("backfilling", x.backfilling())
      .add("hasKeySchema", x.hasKeySchema())
      .add("indexArn", x.indexArn())
      .add("indexName", x.indexName())
      .add("indexSizeBytes", x.indexSizeBytes())
      .add("indexStatus", x.indexStatusAsString())
      .add("itemCount", x.itemCount())
      .add("keySchema", x.keySchema())
      .add("projection", x.projection())
      .add("provisionedThroughput", x.provisionedThroughput())
      .build

  implicit val localSecondaryIndexDescriptionShow
      : Show[ddb.LocalSecondaryIndexDescription] = x =>
    ShowBuilder("LocalSecondaryIndexDescription")
      .add("hasKeySchema", x.hasKeySchema())
      .add("indexArn", x.indexArn())
      .add("indexName", x.indexName())
      .add("indexSizeBytes", x.indexSizeBytes())
      .add("itemCount", x.itemCount())
      .add("keySchema", x.keySchema())
      .add("projection", x.projection())
      .build

  implicit val provisionedThroughputOverrideShow
      : Show[ddb.ProvisionedThroughputOverride] = x =>
    ShowBuilder("ProvisionedThroughputOverride")
      .add("readCapacityUnits", x.readCapacityUnits())
      .build

  implicit val replicaGlobalSecondaryIndexDescriptionShow
      : Show[ddb.ReplicaGlobalSecondaryIndexDescription] = x =>
    ShowBuilder("ReplicaGlobalSecondaryIndexDescription")
      .add("indexName", x.indexName())
      .add("provisionedThroughputOverride", x.provisionedThroughputOverride())
      .build

  implicit val tableClassSummaryShow: Show[ddb.TableClassSummary] = x =>
    ShowBuilder("TableClassSummary")
      .add("lastUpdateDateTime", x.lastUpdateDateTime())
      .add("tableClass", x.tableClassAsString())
      .build

  implicit val replicaDescriptionShow: Show[ddb.ReplicaDescription] = x =>
    ShowBuilder("ReplicaDescription")
      .add("globalSecondaryIndexes", x.globalSecondaryIndexes())
      .add("hasGlobalSecondaryIndexes", x.hasGlobalSecondaryIndexes())
      .add("kmsMasterKeyId", x.kmsMasterKeyId())
      .add("provisionedThroughputOverride", x.provisionedThroughputOverride())
      .add("regionName", x.regionName())
      .add("replicaInaccessibleDateTime", x.replicaInaccessibleDateTime())
      .add("replicaStatus", x.replicaStatusAsString())
      .add("replicaStatusDescription", x.replicaStatusDescription())
      .add("replicaStatusPercentProgress", x.replicaStatusPercentProgress())
      .add("replicaTableClassSummary", x.replicaTableClassSummary())
      .build

  implicit val restoreSummaryShow: Show[ddb.RestoreSummary] = x =>
    ShowBuilder("RestoreSummary")
      .add("restoreDateTime", x.restoreDateTime())
      .add("restoreInProgress", x.restoreInProgress())
      .add("sourceBackupArn", x.sourceBackupArn())
      .add("sourceTableArn", x.sourceTableArn())
      .build

  implicit val sseDescriptionShow: Show[ddb.SSEDescription] = x =>
    ShowBuilder("SSEDescription")
      .add("inaccessibleEncryptionDateTime", x.inaccessibleEncryptionDateTime())
      .add("kmsMasterKeyArn", x.kmsMasterKeyArn())
      .add("sseType", x.sseTypeAsString())
      .add("status", x.statusAsString())
      .build

  implicit val tableDescriptionShow: Show[ddb.TableDescription] = x =>
    ShowBuilder("TableDescription")
      .add("archivalSummary", x.archivalSummary())
      .add("attributeDefinitions", x.attributeDefinitions())
      .add("billingModeSummary", x.billingModeSummary())
      .add("creationDateTime", x.creationDateTime())
      .add("globalSecondaryIndexes", x.globalSecondaryIndexes())
      .add("globalTableVersion", x.globalTableVersion())
      .add("hasAttributeDefinitions", x.hasAttributeDefinitions())
      .add("hasGlobalSecondaryIndexes", x.hasGlobalSecondaryIndexes())
      .add("hasKeySchema", x.hasKeySchema())
      .add("hasLocalSecondaryIndexes", x.hasLocalSecondaryIndexes())
      .add("hasReplicas", x.hasReplicas())
      .add("itemCount", x.itemCount())
      .add("keySchema", x.keySchema())
      .add("latestStreamArn", x.latestStreamArn())
      .add("latestStreamLabel", x.latestStreamLabel())
      .add("localSecondaryIndexes", x.localSecondaryIndexes())
      .add("provisionedThroughput", x.provisionedThroughput())
      .add("replicas", x.replicas())
      .add("restoreSummary", x.restoreSummary())
      .add("sseDescription", x.sseDescription())
      .add("streamSpecification", x.streamSpecification())
      .add("tableArn", x.tableArn())
      .add("tableClassSummary", x.tableClassSummary())
      .add("tableId", x.tableId())
      .add("tableName", x.tableName())
      .add("tableSizeBytes", x.tableSizeBytes())
      .add("tableStatus", x.tableStatusAsString())
      .build

  private def attributeValueMapShowImpl(
      x: List[(String, ddb.AttributeValue)],
      res: String = "Map(",
      empty: Boolean = true
  ): Eval[String] = Eval.defer(x match {
    case Nil => Eval.now(s"$res)")
    case (k, v) :: t =>
      val vStr = attributeValueShowImpl(v)
      val newRes = if (empty) s"$res$k=$vStr" else s"$res,$k=$vStr"
      attributeValueMapShowImpl(t, newRes, true)
  })

  private def attributeValueListShowImpl(
      x: List[ddb.AttributeValue],
      res: String = "List(",
      empty: Boolean = true
  ): Eval[String] = Eval.defer(x match {
    case Nil => Eval.now(s"$res)")
    case v :: t =>
      val vStr = attributeValueShowImpl(v)
      val newRes = if (empty) s"$res$vStr" else s"$res,$vStr"
      attributeValueListShowImpl(t, newRes, true)
  })

  private def attributeValueShowImpl(x: ddb.AttributeValue): Eval[String] =
    for {
      prefix <- Eval.now("AttributeValue(")
      t = Option(x.`type`)
      value <- t match {
        case Some(ddb.AttributeValue.Type.BOOL) =>
          Eval.now(s"bool=${Show[Boolean].show(x.bool())}")
        case Some(ddb.AttributeValue.Type.B) =>
          Eval.now(s"b=${sdkBytesShow.show(x.b())}")
        case Some(ddb.AttributeValue.Type.BS) =>
          Eval.now(s"bs=${Show[java.util.List[SdkBytes]].show(x.bs())}")
        case Some(ddb.AttributeValue.Type.S) => Eval.now(s"s=${x.s()}")
        case Some(ddb.AttributeValue.Type.SS) =>
          Eval.now(s"ss=${Show[java.util.List[String]].show(x.ss())}")
        case Some(ddb.AttributeValue.Type.N) => Eval.now(s"n=${x.n()}")
        case Some(ddb.AttributeValue.Type.NS) =>
          Eval.now(s"ns=${Show[java.util.List[String]].show(x.ns())}")
        case Some(ddb.AttributeValue.Type.NUL) =>
          Eval.now(s"nul=${Show[Boolean].show(x.nul())}")
        case Some(ddb.AttributeValue.Type.UNKNOWN_TO_SDK_VERSION) =>
          Eval.now("UNKNOWN_TO_SDK_VERSION")
        case Some(ddb.AttributeValue.Type.M) =>
          attributeValueMapShowImpl(x.m().asScala.toList)
        case Some(ddb.AttributeValue.Type.L) =>
          attributeValueListShowImpl(x.l().asScala.toList)
        case None => Eval.now("")
      }
      tStr = t.fold("")(y => s",type=${y.name()}")
    } yield s"$prefix$value$tStr)"

  implicit val attributeValueShow: Show[ddb.AttributeValue] = x =>
    attributeValueShowImpl(x).value

  implicit val attributeValueUpdateShow: Show[ddb.AttributeValueUpdate] = x =>
    ShowBuilder("AttributeValueUpdate")
      .add("action", x.actionAsString())
      .add("value", x.value())
      .build

  implicit val capacityShow: Show[ddb.Capacity] = x =>
    ShowBuilder("Capacity")
      .add("capacityUnits", x.capacityUnits())
      .add("readCapacityUnits", x.readCapacityUnits())
      .add("writeCapacityUnits", x.writeCapacityUnits())
      .build

  implicit val consumedCapacityShow: Show[ddb.ConsumedCapacity] = x =>
    ShowBuilder("ConsumedCapacity")
      .add("capacityUnits", x.capacityUnits())
      .add("globalSecondaryIndexes", x.globalSecondaryIndexes())
      .add("hasGlobalSecondaryIndexes", x.hasGlobalSecondaryIndexes())
      .add("hasLocalSecondaryIndexes", x.hasLocalSecondaryIndexes())
      .add("localSecondaryIndexes", x.localSecondaryIndexes())
      .add("readCapacityUnits", x.readCapacityUnits())
      .add("table", x.table())
      .add("tableName", x.tableName())
      .add("writeCapacityUnits", x.writeCapacityUnits())
      .build

  implicit val ddbConditionShow: Show[ddb.Condition] = x =>
    ShowBuilder("Condition")
      .add("attributeValueList", x.attributeValueList())
      .add("comparisonOperator", x.comparisonOperatorAsString())
      .add("hasAttributeValueList", x.hasAttributeValueList())
      .build

  implicit val expectedAttributeValueShow: Show[ddb.ExpectedAttributeValue] =
    x =>
      ShowBuilder("ExpectedAttributeValue")
        .add("attributeValueList", x.attributeValueList())
        .add("comparisonOperator", x.comparisonOperatorAsString())
        .add("exists", x.exists())
        .add("hasAttributeValueList", x.hasAttributeValueList())
        .add("value", x.value())
        .build

  implicit val itemCollectionMetricsShow: Show[ddb.ItemCollectionMetrics] = x =>
    ShowBuilder("ItemCollectionMetrics")
      .add("hasItemCollectionKey", x.hasItemCollectionKey())
      .add("hasSizeEstimateRangeGB", x.hasSizeEstimateRangeGB())
      .add("itemCollectionKey", x.itemCollectionKey())
      .add("sizeEstimateRangeGB", x.sizeEstimateRangeGB())
      .build

  implicit val createTableRequestShow: Show[ddb.CreateTableRequest] = x =>
    ShowBuilder("CreateTableRequest")
      .add("attributeDefinitions", x.attributeDefinitions())
      .add("billingMode", x.billingModeAsString())
      .add("globalSecondaryIndexes", x.globalSecondaryIndexes())
      .add("hasAttributeDefinitions", x.hasAttributeDefinitions())
      .add("hasGlobalSecondaryIndexes", x.hasGlobalSecondaryIndexes())
      .add("hasKeySchema", x.hasKeySchema())
      .add("hasLocalSecondaryIndexes", x.hasLocalSecondaryIndexes())
      .add("hasTags", x.hasTags())
      .add("keySchema", x.keySchema())
      .add("localSecondaryIndexes", x.localSecondaryIndexes())
      .add("provisionedThroughput", x.provisionedThroughput())
      .add("sseSpecification", x.sseSpecification())
      .add("streamSpecification", x.streamSpecification())
      .add("tableClass", x.tableClassAsString())
      .add("tableName", x.tableName())
      .add("tags", x.tags())
      .build

  implicit val createTableResponseShow: Show[ddb.CreateTableResponse] = x =>
    ShowBuilder("CreateTableResponse")
      .add("tableDescription", x.tableDescription())
      .build

  implicit val describeTableRequestShow: Show[ddb.DescribeTableRequest] = x =>
    ShowBuilder("DescribeTableRequest")
      .add("tableName", x.tableName())
      .build

  implicit val describeTableResponseShow: Show[ddb.DescribeTableResponse] = x =>
    ShowBuilder("DescribeTableResponse")
      .add("table", x.table())
      .build

  implicit val scanRequestShow: Show[ddb.ScanRequest] = x =>
    ShowBuilder("ScanRequest")
      .add("attributesToGet", x.attributesToGet())
      .add("conditionalOperator", x.conditionalOperatorAsString())
      .add("consistentRead", x.consistentRead())
      .add("exclusiveStartKey", x.exclusiveStartKey())
      .add("expressionAttributeNames", x.expressionAttributeNames())
      .add("expressionAttributeValues", x.expressionAttributeValues())
      .add("hasAttributesToGet", x.hasAttributesToGet())
      .add("hasExclusiveStartKey", x.hasExclusiveStartKey())
      .add("hasExpressionAttributeNames", x.hasExpressionAttributeNames())
      .add("hasExpressionAttributeValues", x.hasExpressionAttributeValues())
      .add("hasScanFilter", x.hasScanFilter())
      .add("indexName", x.indexName())
      .add("limit", x.limit())
      .add("projectionExpression", x.projectionExpression())
      .add("returnConsumedCapacity", x.returnConsumedCapacityAsString())
      .add("scanFilter", x.scanFilter())
      .add("segment", x.segment())
      .add("select", x.selectAsString())
      .add("tableName", x.tableName())
      .add("totalSegments", x.totalSegments())
      .build

  implicit val scanResponseShow: Show[ddb.ScanResponse] = x =>
    ShowBuilder("ScanResponse")
      .add("consumedCapacity", x.consumedCapacity())
      .add("count", x.count())
      .add("hasItems", x.hasItems())
      .add("hasLastEvaluatedKey", x.hasLastEvaluatedKey())
      .add("items", x.items())
      .add("lastEvaluatedKey", x.lastEvaluatedKey())
      .add("scannedCount", x.scannedCount())
      .build

  implicit val updateItemRequestShow: Show[ddb.UpdateItemRequest] = x =>
    ShowBuilder("UpdateItemRequest")
      .add("attributeUpdates", x.attributeUpdates())
      .add("conditionExpression", x.conditionExpression())
      .add("conditionalOperator", x.conditionalOperatorAsString())
      .add("expected", x.expected())
      .add("expressionAttributeNames", x.expressionAttributeNames())
      .add("expressionAttributeValues", x.expressionAttributeValues())
      .add("hasAttributeUpdates", x.hasAttributeUpdates())
      .add("hasExpected", x.hasExpected())
      .add("hasExpressionAttributeNames", x.hasExpressionAttributeNames())
      .add("hasExpressionAttributeValues", x.hasExpressionAttributeValues())
      .add("hasKey", x.hasKey())
      .add("key", x.key())
      .add("returnConsumedCapacity", x.returnConsumedCapacityAsString())
      .add(
        "returnItemCollectionMetrics",
        x.returnItemCollectionMetricsAsString()
      )
      .add("returnValues", x.returnValuesAsString())
      .add("tableName", x.tableName())
      .add("updateExpression", x.updateExpression())
      .build

  implicit val updateItemResponseShow: Show[ddb.UpdateItemResponse] = x =>
    ShowBuilder("UpdateItemResponse")
      .add("attributes", x.attributes())
      .add("consumedCapacity", x.consumedCapacity())
      .add("hasAttributes", x.hasAttributes())
      .add("itemCollectionMetrics", x.itemCollectionMetrics())
      .build

  implicit val putItemRequestShow: Show[ddb.PutItemRequest] = x =>
    ShowBuilder("PutItemRequest")
      .add("conditionExpression", x.conditionExpression())
      .add("conditionalOperator", x.conditionalOperatorAsString())
      .add("expected", x.expected())
      .add("expressionAttributeNames", x.expressionAttributeNames())
      .add("expressionAttributeValues", x.expressionAttributeValues())
      .add("hasExpected", x.hasExpected())
      .add("hasExpressionAttributeNames", x.hasExpressionAttributeNames())
      .add("hasExpressionAttributeValues", x.hasExpressionAttributeValues())
      .add("item", x.item())
      .add("returnConsumedCapacity", x.returnConsumedCapacityAsString())
      .add(
        "returnItemCollectionMetrics",
        x.returnItemCollectionMetricsAsString()
      )
      .add("returnValues", x.returnValuesAsString())
      .add("tableName", x.tableName())
      .build

  implicit val putItemResponseShow: Show[ddb.PutItemResponse] = x =>
    ShowBuilder("PutItemResponse")
      .add("attributes", x.attributes())
      .add("consumedCapacity", x.consumedCapacity())
      .add("hasAttributes", x.hasAttributes())
      .add("itemCollectionMetrics", x.itemCollectionMetrics())
      .build

  implicit val deleteTableRequestShow: Show[ddb.DeleteTableRequest] = x =>
    ShowBuilder("DeleteTableRequest")
      .add("tableName", x.tableName())
      .build

  implicit val deleteTableResponseShow: Show[ddb.DeleteTableResponse] = x =>
    ShowBuilder("DeleteTableResponse")
      .add("tableDescription", x.tableDescription())
      .build

  implicit val dynamoClientLogEncoders: DynamoClient.LogEncoders =
    new DynamoClient.LogEncoders()

  implicit val dimensionShow: Show[cw.Dimension] = x =>
    ShowBuilder("Dimension")
      .add("name", x.name())
      .add("value", x.value())
      .build

  implicit val statisticSetShow: Show[cw.StatisticSet] = x =>
    ShowBuilder("StatisticSet")
      .add("maximum", x.maximum())
      .add("minimum", x.minimum())
      .add("sampleCount", x.sampleCount())
      .add("sum", x.sum())
      .build

  implicit val metricDatumShow: Show[cw.MetricDatum] = x =>
    ShowBuilder("MetricDatum")
      .add("counts", x.counts())
      .add("dimensions", x.dimensions())
      .add("hasCounts", x.hasCounts())
      .add("hasDimensions", x.hasDimensions())
      .add("hasValues", x.hasValues())
      .add("metricName", x.metricName())
      .add("statisticValues", x.statisticValues())
      .add("storageResolution", x.storageResolution())
      .add("timestamp", x.timestamp())
      .add("unit", x.unitAsString())
      .add("value", x.value())
      .add("values", x.values())
      .build

  implicit val putMetricDataRequestShow: Show[cw.PutMetricDataRequest] = x =>
    ShowBuilder("PutMetricDataRequest")
      .add("hasMetricData", x.hasMetricData())
      .add("metricData", x.metricData())
      .add("namespace", x.namespace())
      .build

  implicit val putMetricDataResponseShow: Show[cw.PutMetricDataResponse] = _ =>
    ShowBuilder("PutMetricDataResponse").build

  implicit val cloudWatchClientLogEncoders: CloudWatchClient.LogEncoders =
    new CloudWatchClient.LogEncoders()

}
