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

import io.circe.{Encoder, Json}
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.kinesis.model._

import kinesis4cats.client.KinesisClientLogEncoders
import kinesis4cats.logging.instances.circe._
import kinesis4cats.logging.syntax.circe._

object circe {

  implicit val kinesisResponseMetadataEncoder
      : Encoder[KinesisResponseMetadata] = x => {
    val fields: Map[String, Json] =
      Map
        .empty[String, Json]
        .safeAdd("extendedRequestId", x.extendedRequestId())
        .safeAdd("requestId", x.requestId())

    Json.obj(fields.toSeq: _*)
  }

  implicit val streamModeDetailsEncoder: Encoder[StreamModeDetails] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("streamMode", x.streamModeAsString())

    Json.obj(fields.toSeq: _*)
  }

  implicit val enhancedMonitoringEncoder: Encoder[EnhancedMetrics] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("hasShardLevelMetrics", x.hasShardLevelMetrics())
      .safeAdd("shardLevelMetrics", x.shardLevelMetricsAsStrings())

    Json.obj(fields.toSeq: _*)
  }

  implicit val hashKeyRangeEncoder: Encoder[HashKeyRange] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("endingHashKey", x.endingHashKey())
      .safeAdd("startingHashKey", x.startingHashKey())

    Json.obj(fields.toSeq: _*)
  }

  implicit val sequenceNumberRangeEncoder: Encoder[SequenceNumberRange] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("endingSequenceNumber", x.endingSequenceNumber())
      .safeAdd("startingSequenceNumber", x.startingSequenceNumber())

    Json.obj(fields.toSeq: _*)
  }

  implicit val shardEncoder: Encoder[Shard] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("adjacentParentShardId", x.adjacentParentShardId())
      .safeAdd("hashKeyRange", x.hashKeyRange())
      .safeAdd("parentShardId", x.parentShardId())
      .safeAdd("sequenceNumberRange", x.sequenceNumberRange())
      .safeAdd("shardId", x.shardId())

    Json.obj(fields.toSeq: _*)
  }

  implicit val streamDescriptionEncoder: Encoder[StreamDescription] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("encryptionType", x.encryptionTypeAsString())
      .safeAdd("enhancedMonitoring", x.enhancedMonitoring())
      .safeAdd("hasEnhancedMonitoring", x.hasEnhancedMonitoring())
      .safeAdd("hasMoreShards", x.hasMoreShards())
      .safeAdd("hasShards", x.hasShards())
      .safeAdd("keyId", x.keyId())
      .safeAdd("retentionPeriodHours", x.retentionPeriodHours())
      .safeAdd("shards", x.shards())
      .safeAdd("streamARN", x.streamARN())
      .safeAdd("streamCreationTimestamp", x.streamCreationTimestamp())
      .safeAdd("streamModeDetails", x.streamModeDetails())
      .safeAdd("streamName", x.streamName())
      .safeAdd("streamStatus", x.streamStatusAsString())

    Json.obj(fields.toSeq: _*)
  }

  implicit val streamDescriptionSummaryEncoder
      : Encoder[StreamDescriptionSummary] =
    x => {
      val fields: Map[String, Json] = Map
        .empty[String, Json]
        .safeAdd("consumerCount", x.consumerCount())
        .safeAdd("encryptionType", x.encryptionTypeAsString())
        .safeAdd("enhancedMonitoring", x.enhancedMonitoring())
        .safeAdd("hasEnhancedMonitoring", x.hasEnhancedMonitoring())
        .safeAdd("keyId", x.keyId())
        .safeAdd("openShardCount", x.openShardCount())
        .safeAdd("retentionPeriodHours", x.retentionPeriodHours())
        .safeAdd("streamARN", x.streamARN())
        .safeAdd("streamCreationTimestamp", x.streamCreationTimestamp())
        .safeAdd("streamModeDetails", x.streamModeDetails())
        .safeAdd("streamName", x.streamName())
        .safeAdd("streamStatus", x.streamStatusAsString())

      Json.obj(fields.toSeq: _*)
    }

  implicit val consumerDescriptionEncoder: Encoder[ConsumerDescription] =
    x => {
      val fields: Map[String, Json] = Map
        .empty[String, Json]
        .safeAdd("consumerARN", x.consumerARN())
        .safeAdd("consumerCreationTimestamp", x.consumerCreationTimestamp())
        .safeAdd("consumerName", x.consumerName())
        .safeAdd("consumerStatus", x.consumerStatusAsString())
        .safeAdd("streamARN", x.streamARN())

      Json.obj(fields.toSeq: _*)
    }

  implicit val childShardEncoder: Encoder[ChildShard] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("hasParentShards", x.hasParentShards())
      .safeAdd("hashKeyRange", x.hashKeyRange())
      .safeAdd("parentShards", x.parentShards())
      .safeAdd("shardId", x.shardId())

    Json.obj(fields.toSeq: _*)
  }

  implicit val consumerEncoder: Encoder[Consumer] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("consumerARN", x.consumerARN())
      .safeAdd("consumerCreationTimestamp", x.consumerCreationTimestamp())
      .safeAdd("consumerName", x.consumerName())
      .safeAdd("consumerStatus", x.consumerStatusAsString())

    Json.obj(fields.toSeq: _*)
  }

  implicit val sdkBytesEncoder: Encoder[SdkBytes] =
    Encoder[ByteBuffer].contramap(_.asByteBuffer())

  implicit val recordEncoder: Encoder[Record] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("approximateArrivalTimestamp", x.approximateArrivalTimestamp())
      .safeAdd("data", x.data())
      .safeAdd("encryptionTypeAsString", x.encryptionTypeAsString())
      .safeAdd("partitionKey", x.partitionKey())
      .safeAdd("sequenceNumber", x.sequenceNumber())

    Json.obj(fields.toSeq: _*)
  }

  implicit val shardFilterEncoder: Encoder[ShardFilter] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("shardId", x.shardId())
      .safeAdd("timestamp", x.timestamp())
      .safeAdd("type", x.typeAsString())

    Json.obj(fields.toSeq: _*)
  }

  implicit val streamSummaryEncoder: Encoder[StreamSummary] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("streamARN", x.streamARN())
      .safeAdd("streamCreationTimestamp", x.streamCreationTimestamp())
      .safeAdd("streamModeDetails", x.streamModeDetails())
      .safeAdd("streamName", x.streamName())
      .safeAdd("streamStatus", x.streamStatusAsString())

    Json.obj(fields.toSeq: _*)
  }

  implicit val tagEncoder: Encoder[Tag] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("key", x.key())
      .safeAdd("value", x.value())

    Json.obj(fields.toSeq: _*)
  }

  implicit val putRecordsRequestEntryEncoder: Encoder[PutRecordsRequestEntry] =
    x => {
      val fields: Map[String, Json] = Map
        .empty[String, Json]
        .safeAdd("data", x.data())
        .safeAdd("explicitHashKey", x.explicitHashKey())
        .safeAdd("partitionKey", x.partitionKey())

      Json.obj(fields.toSeq: _*)
    }

  implicit val putRecordsResultEntryEncoder: Encoder[PutRecordsResultEntry] =
    x => {
      val fields: Map[String, Json] = Map
        .empty[String, Json]
        .safeAdd("errorCode", x.errorCode())
        .safeAdd("errorMessage", x.errorMessage())
        .safeAdd("sequenceNumber", x.sequenceNumber())
        .safeAdd("shardId", x.shardId())

      Json.obj(fields.toSeq: _*)
    }

  implicit val startingPositionEncoder: Encoder[StartingPosition] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("sequenceNumber", x.sequenceNumber())
      .safeAdd("timestamp", x.timestamp())
      .safeAdd("typeAsString", x.typeAsString())

    Json.obj(fields.toSeq: _*)
  }

  implicit val addTagsToStreamRequestEncoder: Encoder[AddTagsToStreamRequest] =
    x => {
      val fields: Map[String, Json] = Map
        .empty[String, Json]
        .safeAdd("streamName", x.streamName())
        .safeAdd("tags", x.tags())
        .safeAdd("streamARN", x.streamARN())

      Json.obj(fields.toSeq: _*)
    }

  implicit val addTagsToStreamResponseEncoder
      : Encoder[AddTagsToStreamResponse] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("responseMetadata", x.responseMetadata())

    Json.obj(fields.toSeq: _*)
  }

  implicit val createStreamRequestEncoder: Encoder[CreateStreamRequest] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("streamName", x.streamName())
      .safeAdd("shardCount", x.shardCount())
      .safeAdd("streamARN", x.streamModeDetails())

    Json.obj(fields.toSeq: _*)
  }

  implicit val createStreamResponseEncoder: Encoder[CreateStreamResponse] = x =>
    {
      val fields: Map[String, Json] = Map
        .empty[String, Json]
        .safeAdd("responseMetadata", x.responseMetadata())

      Json.obj(fields.toSeq: _*)
    }

  implicit val decreaseStreamRetentionPeriodRequestEncoder
      : Encoder[DecreaseStreamRetentionPeriodRequest] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("streamARN", x.streamARN())
      .safeAdd("streamName", x.streamName())
      .safeAdd("retentionPeriodHours", x.retentionPeriodHours())

    Json.obj(fields.toSeq: _*)
  }

  implicit val decreaseStreamRetentionPeriodResponseEncoder
      : Encoder[DecreaseStreamRetentionPeriodResponse] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("responseMetadata", x.responseMetadata())

    Json.obj(fields.toSeq: _*)
  }

  implicit val deleteStreamRequestEncoder: Encoder[DeleteStreamRequest] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("streamARN", x.streamARN())
      .safeAdd("streamName", x.streamName())
      .safeAdd("enforceConsumerDeletion", x.enforceConsumerDeletion())

    Json.obj(fields.toSeq: _*)
  }

  implicit val deleteStreamResponseEncoder: Encoder[DeleteStreamResponse] = x =>
    {
      val fields: Map[String, Json] = Map
        .empty[String, Json]
        .safeAdd("responseMetadata", x.responseMetadata())

      Json.obj(fields.toSeq: _*)
    }

  implicit val deregisterStreamConsumerRequestEncoder
      : Encoder[DeregisterStreamConsumerRequest] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("streamARN", x.streamARN())
      .safeAdd("consumerName", x.consumerName())
      .safeAdd("consumerARN", x.consumerARN())

    Json.obj(fields.toSeq: _*)
  }

  implicit val deregisterStreamConsumerResponseEncoder
      : Encoder[DeregisterStreamConsumerResponse] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("responseMetadata", x.responseMetadata())

    Json.obj(fields.toSeq: _*)
  }

  implicit val describeLimitsRequestEncoder: Encoder[DescribeLimitsRequest] =
    _ => Json.obj()

  implicit val describeLimitsResponseEncoder: Encoder[DescribeLimitsResponse] =
    x => {
      val fields: Map[String, Json] = Map
        .empty[String, Json]
        .safeAdd("onDemandStreamCount", x.onDemandStreamCount())
        .safeAdd("onDemandStreamCountLimit", x.onDemandStreamCountLimit())
        .safeAdd("openShardCount", x.openShardCount())
        .safeAdd("shardLimit", x.shardLimit())
        .safeAdd("responseMetadata", x.responseMetadata())

      Json.obj(fields.toSeq: _*)
    }

  implicit val describeStreamRequestEncoder: Encoder[DescribeStreamRequest] =
    x => {
      val fields: Map[String, Json] = Map
        .empty[String, Json]
        .safeAdd("exclusiveStartShardId", x.exclusiveStartShardId())
        .safeAdd("limit", x.limit())
        .safeAdd("streamARN", x.streamARN())
        .safeAdd("streamName", x.streamName())

      Json.obj(fields.toSeq: _*)
    }

  implicit val describeStreamResponseEncoder: Encoder[DescribeStreamResponse] =
    x => {
      val fields: Map[String, Json] = Map
        .empty[String, Json]
        .safeAdd("streamDescription", x.streamDescription())
        .safeAdd("responseMetadata", x.responseMetadata())

      Json.obj(fields.toSeq: _*)
    }

  implicit val describeStreamConsumerRequestEncoder
      : Encoder[DescribeStreamConsumerRequest] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("streamARN", x.streamARN())
      .safeAdd("consumerARN", x.consumerARN())
      .safeAdd("consumerName", x.consumerName())

    Json.obj(fields.toSeq: _*)
  }

  implicit val describeStreamConsumerResponseEncoder
      : Encoder[DescribeStreamConsumerResponse] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("consumerDescription", x.consumerDescription())
      .safeAdd("responseMetadata", x.responseMetadata())

    Json.obj(fields.toSeq: _*)
  }

  implicit val describeStreamSummaryRequestEncoder
      : Encoder[DescribeStreamSummaryRequest] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("streamARN", x.streamARN())
      .safeAdd("streamName", x.streamName())

    Json.obj(fields.toSeq: _*)
  }

  implicit val describeStreamSummaryResponseEncoder
      : Encoder[DescribeStreamSummaryResponse] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("streamDescriptionSummary", x.streamDescriptionSummary())
      .safeAdd("responseMetadata", x.responseMetadata())

    Json.obj(fields.toSeq: _*)
  }

  implicit val disableEnhancedMonitoringEncoder
      : Encoder[DisableEnhancedMonitoringRequest] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("shardLevelMetrics", x.shardLevelMetricsAsStrings())
      .safeAdd("streamName", x.streamName())
      .safeAdd("streamARN", x.streamARN())

    Json.obj(fields.toSeq: _*)
  }

  implicit val disableEnhancedMonitoringResponseEncoder
      : Encoder[DisableEnhancedMonitoringResponse] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd(
        "currentShardLevelMetrics",
        x.currentShardLevelMetricsAsStrings()
      )
      .safeAdd(
        "desiredShardLevelMetrics",
        x.desiredShardLevelMetricsAsStrings()
      )
      .safeAdd("hasCurrentShardLevelMetrics", x.hasCurrentShardLevelMetrics())
      .safeAdd("hasDesiredShardLevelMetrics", x.hasDesiredShardLevelMetrics())
      .safeAdd("streamARN", x.streamARN())
      .safeAdd("streamName", x.streamName())
      .safeAdd("responseMetadata", x.responseMetadata())

    Json.obj(fields.toSeq: _*)
  }

  implicit val enableEnhancedMonitoringEncoder
      : Encoder[EnableEnhancedMonitoringRequest] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("shardLevelMetrics", x.shardLevelMetricsAsStrings())
      .safeAdd("streamName", x.streamName())
      .safeAdd("streamARN", x.streamARN())

    Json.obj(fields.toSeq: _*)
  }

  implicit val enableEnhancedMonitoringResponseEncoder
      : Encoder[EnableEnhancedMonitoringResponse] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd(
        "currentShardLevelMetrics",
        x.currentShardLevelMetricsAsStrings()
      )
      .safeAdd(
        "desiredShardLevelMetrics",
        x.desiredShardLevelMetricsAsStrings()
      )
      .safeAdd("hasCurrentShardLevelMetrics", x.hasCurrentShardLevelMetrics())
      .safeAdd("hasDesiredShardLevelMetrics", x.hasDesiredShardLevelMetrics())
      .safeAdd("streamARN", x.streamARN())
      .safeAdd("streamName", x.streamName())
      .safeAdd("responseMetadata", x.responseMetadata())

    Json.obj(fields.toSeq: _*)
  }

  implicit val getRecordsRequestEncoder: Encoder[GetRecordsRequest] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("limit", x.limit())
      .safeAdd("shardIterator", x.shardIterator())
      .safeAdd("streamARN", x.streamARN())

    Json.obj(fields.toSeq: _*)
  }

  implicit val getRecordsResponseEncoder: Encoder[GetRecordsResponse] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("childShards", x.childShards())
      .safeAdd("hasChildShards", x.hasChildShards())
      .safeAdd("hasRecords", x.hasRecords())
      .safeAdd("millisBehindLatest", x.millisBehindLatest())
      .safeAdd("nextShardIterator", x.nextShardIterator())
      .safeAdd("records", x.records())
      .safeAdd("responseMetadata", x.responseMetadata())

    Json.obj(fields.toSeq: _*)
  }

  implicit val getShardIteratorRequestEncoder
      : Encoder[GetShardIteratorRequest] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("shardId", x.shardId())
      .safeAdd("shardIteratorType", x.shardIteratorTypeAsString())
      .safeAdd("startingSequenceNumber", x.startingSequenceNumber())
      .safeAdd("streamARN", x.streamARN())
      .safeAdd("streamName", x.streamName())
      .safeAdd("timestamp", x.timestamp())

    Json.obj(fields.toSeq: _*)
  }

  implicit val getShardIteratorResponseEncoder
      : Encoder[GetShardIteratorResponse] =
    x => {
      val fields: Map[String, Json] = Map
        .empty[String, Json]
        .safeAdd("shardIterator", x.shardIterator())

      Json.obj(fields.toSeq: _*)
    }

  implicit val increaseStreamRetentionPeriodRequestEncoder
      : Encoder[IncreaseStreamRetentionPeriodRequest] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("streamARN", x.streamARN())
      .safeAdd("streamName", x.streamName())
      .safeAdd("retentionPeriodHours", x.retentionPeriodHours())

    Json.obj(fields.toSeq: _*)
  }

  implicit val increaseStreamRetentionPeriodResponseEncoder
      : Encoder[IncreaseStreamRetentionPeriodResponse] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("responseMetadata", x.responseMetadata())

    Json.obj(fields.toSeq: _*)
  }

  implicit val listShardsRequestEncoder: Encoder[ListShardsRequest] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("maxResults", x.maxResults())
      .safeAdd("nextToken", x.nextToken())
      .safeAdd("shardFilter", x.shardFilter())
      .safeAdd("streamARN", x.streamARN())
      .safeAdd("streamCreationTimestamp", x.streamCreationTimestamp())
      .safeAdd("streamName", x.streamName())

    Json.obj(fields.toSeq: _*)
  }

  implicit val listShardsResponseEncoder: Encoder[ListShardsResponse] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("hasShards", x.hasShards())
      .safeAdd("nextToken", x.nextToken())
      .safeAdd("shards", x.shards())
      .safeAdd("responseMetadata", x.responseMetadata())

    Json.obj(fields.toSeq: _*)
  }

  implicit val listStreamConsumersRequestEncoder
      : Encoder[ListStreamConsumersRequest] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("maxResults", x.maxResults())
      .safeAdd("nextToken", x.nextToken())
      .safeAdd("streamARN", x.streamARN())
      .safeAdd("streamCreationTimestamp", x.streamCreationTimestamp())

    Json.obj(fields.toSeq: _*)
  }

  implicit val listStreamConsumersResponseEncoder
      : Encoder[ListStreamConsumersResponse] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("consumers", x.consumers())
      .safeAdd("hasConsumers", x.hasConsumers())
      .safeAdd("nextToken", x.nextToken())
      .safeAdd("responseMetadata", x.responseMetadata())

    Json.obj(fields.toSeq: _*)
  }

  implicit val listStreamsRequestEncoder: Encoder[ListStreamsRequest] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("exclusiveStartStreamName", x.exclusiveStartStreamName())
      .safeAdd("limit", x.limit())
      .safeAdd("nextToken", x.nextToken())

    Json.obj(fields.toSeq: _*)
  }

  implicit val listStreamsResponseEncoder: Encoder[ListStreamsResponse] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("hasMoreStreams", x.hasMoreStreams())
      .safeAdd("hasStreamNames", x.hasStreamNames())
      .safeAdd("hasStreamSummaries", x.hasStreamSummaries())
      .safeAdd("nextToken", x.nextToken())
      .safeAdd("streamNames", x.streamNames())
      .safeAdd("streamSummaries", x.streamSummaries())
      .safeAdd("responseMetadata", x.responseMetadata())

    Json.obj(fields.toSeq: _*)
  }

  implicit val listTagsForStreamRequestEncoder
      : Encoder[ListTagsForStreamRequest] =
    x => {
      val fields: Map[String, Json] = Map
        .empty[String, Json]
        .safeAdd("exclusiveStartTagKey", x.exclusiveStartTagKey())
        .safeAdd("limit", x.limit())
        .safeAdd("streamARN", x.streamARN())
        .safeAdd("streamName", x.streamName())

      Json.obj(fields.toSeq: _*)
    }

  implicit val listTagsForStreamResponseEncoder
      : Encoder[ListTagsForStreamResponse] =
    x => {
      val fields: Map[String, Json] = Map
        .empty[String, Json]
        .safeAdd("hasMoreTags", x.hasMoreTags())
        .safeAdd("hasTags", x.hasTags())
        .safeAdd("tags", x.tags())
        .safeAdd("responseMetadata", x.responseMetadata())

      Json.obj(fields.toSeq: _*)
    }

  implicit val mergeShardsRequestEncoder: Encoder[MergeShardsRequest] =
    x => {
      val fields: Map[String, Json] = Map
        .empty[String, Json]
        .safeAdd("adjacentShardToMerge", x.adjacentShardToMerge())
        .safeAdd("shardToMerge", x.shardToMerge())
        .safeAdd("streamARN", x.streamARN())
        .safeAdd("streamName", x.streamName())

      Json.obj(fields.toSeq: _*)
    }

  implicit val mergeShardsResponseEncoder: Encoder[MergeShardsResponse] =
    x => {
      val fields: Map[String, Json] = Map
        .empty[String, Json]
        .safeAdd("responseMetadata", x.responseMetadata())

      Json.obj(fields.toSeq: _*)
    }

  implicit val putRecordRequestEncoder: Encoder[PutRecordRequest] =
    x => {
      val fields: Map[String, Json] = Map
        .empty[String, Json]
        .safeAdd("data", x.data())
        .safeAdd("explicitHashKey", x.explicitHashKey())
        .safeAdd("partitionKey", x.partitionKey())
        .safeAdd("sequenceNumberForOrdering", x.sequenceNumberForOrdering())
        .safeAdd("streamARN", x.streamARN())
        .safeAdd("streamName", x.streamName())

      Json.obj(fields.toSeq: _*)
    }

  implicit val putRecordResponseEncoder: Encoder[PutRecordResponse] =
    x => {
      val fields: Map[String, Json] = Map
        .empty[String, Json]
        .safeAdd("encryptionType", x.encryptionTypeAsString())
        .safeAdd("sequenceNumber", x.sequenceNumber())
        .safeAdd("shardId", x.shardId())
        .safeAdd("responseMetadata", x.responseMetadata())

      Json.obj(fields.toSeq: _*)
    }

  implicit val putRecordsRequestEncoder: Encoder[PutRecordsRequest] =
    x => {
      val fields: Map[String, Json] = Map
        .empty[String, Json]
        .safeAdd("hasRecords", x.hasRecords())
        .safeAdd("records", x.records())
        .safeAdd("streamARN", x.streamARN())
        .safeAdd("streamName", x.streamName())

      Json.obj(fields.toSeq: _*)
    }

  implicit val putRecordsResponseEncoder: Encoder[PutRecordsResponse] =
    x => {
      val fields: Map[String, Json] = Map
        .empty[String, Json]
        .safeAdd("encryptionType", x.encryptionTypeAsString())
        .safeAdd("failedRecordCount", x.failedRecordCount())
        .safeAdd("hasRecords", x.hasRecords())
        .safeAdd("records", x.records())
        .safeAdd("responseMetadata", x.responseMetadata())

      Json.obj(fields.toSeq: _*)
    }

  implicit val registerStreamConsumerRequestEncoder
      : Encoder[RegisterStreamConsumerRequest] =
    x => {
      val fields: Map[String, Json] = Map
        .empty[String, Json]
        .safeAdd("consumerName", x.consumerName())
        .safeAdd("streamARN", x.streamARN())

      Json.obj(fields.toSeq: _*)
    }

  implicit val registerStreamConsumerResponseEncoder
      : Encoder[RegisterStreamConsumerResponse] =
    x => {
      val fields: Map[String, Json] = Map
        .empty[String, Json]
        .safeAdd("consumer", x.consumer())
        .safeAdd("responseMetadata", x.responseMetadata())

      Json.obj(fields.toSeq: _*)
    }

  implicit val removeTagsFromStreamRequestEncoder
      : Encoder[RemoveTagsFromStreamRequest] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("hasTagKeys", x.hasTagKeys())
      .safeAdd("streamName", x.streamName())
      .safeAdd("streamARN", x.streamARN())
      .safeAdd("tagKeys", x.tagKeys())

    Json.obj(fields.toSeq: _*)
  }

  implicit val removeTagsFromStreamResponseEncoder
      : Encoder[RemoveTagsFromStreamResponse] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("responseMetadata", x.responseMetadata())

    Json.obj(fields.toSeq: _*)
  }

  implicit val splitShardRequestEncoder: Encoder[SplitShardRequest] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("newStartingHashKey", x.newStartingHashKey())
      .safeAdd("shardToSplit", x.shardToSplit())
      .safeAdd("streamName", x.streamName())
      .safeAdd("streamARN", x.streamARN())

    Json.obj(fields.toSeq: _*)
  }

  implicit val splitShardResponseEncoder: Encoder[SplitShardResponse] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("responseMetadata", x.responseMetadata())

    Json.obj(fields.toSeq: _*)
  }

  implicit val startStreamEncryptionRequestEncoder
      : Encoder[StartStreamEncryptionRequest] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("encryptionTypeAsString", x.encryptionTypeAsString())
      .safeAdd("keyId", x.keyId())
      .safeAdd("streamName", x.streamName())
      .safeAdd("streamARN", x.streamARN())

    Json.obj(fields.toSeq: _*)
  }

  implicit val startStreamEncryptionResponseEncoder
      : Encoder[StartStreamEncryptionResponse] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("responseMetadata", x.responseMetadata())

    Json.obj(fields.toSeq: _*)
  }

  implicit val stopStreamEncryptionRequestEncoder
      : Encoder[StopStreamEncryptionRequest] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("encryptionTypeAsString", x.encryptionTypeAsString())
      .safeAdd("keyId", x.keyId())
      .safeAdd("streamName", x.streamName())
      .safeAdd("streamARN", x.streamARN())

    Json.obj(fields.toSeq: _*)
  }

  implicit val stopStreamEncryptionResponseEncoder
      : Encoder[StopStreamEncryptionResponse] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("responseMetadata", x.responseMetadata())

    Json.obj(fields.toSeq: _*)
  }

  implicit val subscribeToShardRequestEncoder
      : Encoder[SubscribeToShardRequest] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("consumerARN", x.consumerARN())
      .safeAdd("shardId", x.shardId())
      .safeAdd("startingPosition", x.startingPosition())

    Json.obj(fields.toSeq: _*)
  }

  implicit val subscribeToShardResponseEncoder
      : Encoder[SubscribeToShardResponse] =
    x => {
      val fields: Map[String, Json] = Map
        .empty[String, Json]
        .safeAdd("responseMetadata", x.responseMetadata())

      Json.obj(fields.toSeq: _*)
    }

  implicit val updateShardCountRequestEncoder
      : Encoder[UpdateShardCountRequest] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("scalingType", x.scalingTypeAsString())
      .safeAdd("streamARN", x.streamARN())
      .safeAdd("streamName", x.streamName())
      .safeAdd("targetShardCount", x.targetShardCount())

    Json.obj(fields.toSeq: _*)
  }

  implicit val updateShardCountResponseEncoder
      : Encoder[UpdateShardCountResponse] =
    x => {
      val fields: Map[String, Json] = Map
        .empty[String, Json]
        .safeAdd("currentShardCount", x.currentShardCount())
        .safeAdd("streamARN", x.streamARN())
        .safeAdd("streamName", x.streamName())
        .safeAdd("targetShardCount", x.targetShardCount())
        .safeAdd("responseMetadata", x.responseMetadata())

      Json.obj(fields.toSeq: _*)
    }

  implicit val updateStreamModeRequestEncoder
      : Encoder[UpdateStreamModeRequest] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("streamARN", x.streamARN())
      .safeAdd("streamModeDetails", x.streamModeDetails())

    Json.obj(fields.toSeq: _*)
  }

  implicit val updateStreamModeResponseEncoder
      : Encoder[UpdateStreamModeResponse] =
    x => {
      val fields: Map[String, Json] = Map
        .empty[String, Json]
        .safeAdd("responseMetadata", x.responseMetadata())

      Json.obj(fields.toSeq: _*)
    }

  implicit val kinesisClientLogEncoders: KinesisClientLogEncoders =
    new KinesisClientLogEncoders()

}
