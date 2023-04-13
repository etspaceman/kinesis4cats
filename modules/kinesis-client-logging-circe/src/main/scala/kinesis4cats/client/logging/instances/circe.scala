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

package kinesis4cats.client
package logging.instances

import scala.jdk.CollectionConverters._

import java.nio.ByteBuffer

import cats.Eval
import io.circe.syntax._
import io.circe.{Encoder, Json}
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.cloudwatch.{model => cw}
import software.amazon.awssdk.services.dynamodb.{model => ddb}
import software.amazon.awssdk.services.kinesis.{model => kin}

import kinesis4cats.logging.instances.circe._
import kinesis4cats.logging.syntax.circe._

/** KinesisClient [[kinesis4cats.logging.LogEncoder LogEncoder]] instances for
  * string encoding of log structures using [[cats.Show Show]]
  */
object circe {

  implicit val kinesisResponseMetadataEncoder
      : Encoder[kin.KinesisResponseMetadata] = x => {
    val fields: Map[String, Json] =
      Map
        .empty[String, Json]
        .safeAdd("extendedRequestId", x.extendedRequestId())
        .safeAdd("requestId", x.requestId())

    Json.obj(fields.toSeq: _*)
  }

  implicit val streamModeDetailsEncoder: Encoder[kin.StreamModeDetails] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("streamMode", x.streamModeAsString())

    Json.obj(fields.toSeq: _*)
  }

  implicit val enhancedMonitoringEncoder: Encoder[kin.EnhancedMetrics] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("hasShardLevelMetrics", x.hasShardLevelMetrics())
      .safeAdd("shardLevelMetrics", x.shardLevelMetricsAsStrings())

    Json.obj(fields.toSeq: _*)
  }

  implicit val hashKeyRangeEncoder: Encoder[kin.HashKeyRange] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("endingHashKey", x.endingHashKey())
      .safeAdd("startingHashKey", x.startingHashKey())

    Json.obj(fields.toSeq: _*)
  }

  implicit val sequenceNumberRangeEncoder: Encoder[kin.SequenceNumberRange] =
    x => {
      val fields: Map[String, Json] = Map
        .empty[String, Json]
        .safeAdd("endingSequenceNumber", x.endingSequenceNumber())
        .safeAdd("startingSequenceNumber", x.startingSequenceNumber())

      Json.obj(fields.toSeq: _*)
    }

  implicit val shardEncoder: Encoder[kin.Shard] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("adjacentParentShardId", x.adjacentParentShardId())
      .safeAdd("hashKeyRange", x.hashKeyRange())
      .safeAdd("parentShardId", x.parentShardId())
      .safeAdd("sequenceNumberRange", x.sequenceNumberRange())
      .safeAdd("shardId", x.shardId())

    Json.obj(fields.toSeq: _*)
  }

  implicit val streamDescriptionEncoder: Encoder[kin.StreamDescription] = x => {
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
      : Encoder[kin.StreamDescriptionSummary] =
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

  implicit val consumerDescriptionEncoder: Encoder[kin.ConsumerDescription] =
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

  implicit val childShardEncoder: Encoder[kin.ChildShard] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("hasParentShards", x.hasParentShards())
      .safeAdd("hashKeyRange", x.hashKeyRange())
      .safeAdd("parentShards", x.parentShards())
      .safeAdd("shardId", x.shardId())

    Json.obj(fields.toSeq: _*)
  }

  implicit val consumerEncoder: Encoder[kin.Consumer] = x => {
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

  implicit val recordEncoder: Encoder[kin.Record] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd(
        "approximateArrivalTimestamp",
        x.approximateArrivalTimestamp()
      )
      .safeAdd("data", x.data())
      .safeAdd("encryptionTypeAsString", x.encryptionTypeAsString())
      .safeAdd("partitionKey", x.partitionKey())
      .safeAdd("sequenceNumber", x.sequenceNumber())

    Json.obj(fields.toSeq: _*)
  }

  implicit val shardFilterEncoder: Encoder[kin.ShardFilter] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("shardId", x.shardId())
      .safeAdd("timestamp", x.timestamp())
      .safeAdd("type", x.typeAsString())

    Json.obj(fields.toSeq: _*)
  }

  implicit val streamSummaryEncoder: Encoder[kin.StreamSummary] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("streamARN", x.streamARN())
      .safeAdd("streamCreationTimestamp", x.streamCreationTimestamp())
      .safeAdd("streamModeDetails", x.streamModeDetails())
      .safeAdd("streamName", x.streamName())
      .safeAdd("streamStatus", x.streamStatusAsString())

    Json.obj(fields.toSeq: _*)
  }

  implicit val tagEncoder: Encoder[kin.Tag] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("key", x.key())
      .safeAdd("value", x.value())

    Json.obj(fields.toSeq: _*)
  }

  implicit val putRecordsRequestEntryEncoder
      : Encoder[kin.PutRecordsRequestEntry] =
    x => {
      val fields: Map[String, Json] = Map
        .empty[String, Json]
        .safeAdd("data", x.data())
        .safeAdd("explicitHashKey", x.explicitHashKey())
        .safeAdd("partitionKey", x.partitionKey())

      Json.obj(fields.toSeq: _*)
    }

  implicit val putRecordsResultEntryEncoder
      : Encoder[kin.PutRecordsResultEntry] =
    x => {
      val fields: Map[String, Json] = Map
        .empty[String, Json]
        .safeAdd("errorCode", x.errorCode())
        .safeAdd("errorMessage", x.errorMessage())
        .safeAdd("sequenceNumber", x.sequenceNumber())
        .safeAdd("shardId", x.shardId())

      Json.obj(fields.toSeq: _*)
    }

  implicit val startingPositionEncoder: Encoder[kin.StartingPosition] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("sequenceNumber", x.sequenceNumber())
      .safeAdd("timestamp", x.timestamp())
      .safeAdd("typeAsString", x.typeAsString())

    Json.obj(fields.toSeq: _*)
  }

  implicit val addTagsToStreamRequestEncoder
      : Encoder[kin.AddTagsToStreamRequest] =
    x => {
      val fields: Map[String, Json] = Map
        .empty[String, Json]
        .safeAdd("streamName", x.streamName())
        .safeAdd("tags", x.tags())
        .safeAdd("streamARN", x.streamARN())

      Json.obj(fields.toSeq: _*)
    }

  implicit val addTagsToStreamResponseEncoder
      : Encoder[kin.AddTagsToStreamResponse] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("responseMetadata", x.responseMetadata())

    Json.obj(fields.toSeq: _*)
  }

  implicit val createStreamRequestEncoder: Encoder[kin.CreateStreamRequest] =
    x => {
      val fields: Map[String, Json] = Map
        .empty[String, Json]
        .safeAdd("streamName", x.streamName())
        .safeAdd("shardCount", x.shardCount())
        .safeAdd("streamARN", x.streamModeDetails())

      Json.obj(fields.toSeq: _*)
    }

  implicit val createStreamResponseEncoder: Encoder[kin.CreateStreamResponse] =
    x => {
      val fields: Map[String, Json] = Map
        .empty[String, Json]
        .safeAdd("responseMetadata", x.responseMetadata())

      Json.obj(fields.toSeq: _*)
    }

  implicit val decreaseStreamRetentionPeriodRequestEncoder
      : Encoder[kin.DecreaseStreamRetentionPeriodRequest] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("streamARN", x.streamARN())
      .safeAdd("streamName", x.streamName())
      .safeAdd("retentionPeriodHours", x.retentionPeriodHours())

    Json.obj(fields.toSeq: _*)
  }

  implicit val decreaseStreamRetentionPeriodResponseEncoder
      : Encoder[kin.DecreaseStreamRetentionPeriodResponse] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("responseMetadata", x.responseMetadata())

    Json.obj(fields.toSeq: _*)
  }

  implicit val deleteStreamRequestEncoder: Encoder[kin.DeleteStreamRequest] =
    x => {
      val fields: Map[String, Json] = Map
        .empty[String, Json]
        .safeAdd("streamARN", x.streamARN())
        .safeAdd("streamName", x.streamName())
        .safeAdd("enforceConsumerDeletion", x.enforceConsumerDeletion())

      Json.obj(fields.toSeq: _*)
    }

  implicit val deleteStreamResponseEncoder: Encoder[kin.DeleteStreamResponse] =
    x => {
      val fields: Map[String, Json] = Map
        .empty[String, Json]
        .safeAdd("responseMetadata", x.responseMetadata())

      Json.obj(fields.toSeq: _*)
    }

  implicit val deregisterStreamConsumerRequestEncoder
      : Encoder[kin.DeregisterStreamConsumerRequest] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("streamARN", x.streamARN())
      .safeAdd("consumerName", x.consumerName())
      .safeAdd("consumerARN", x.consumerARN())

    Json.obj(fields.toSeq: _*)
  }

  implicit val deregisterStreamConsumerResponseEncoder
      : Encoder[kin.DeregisterStreamConsumerResponse] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("responseMetadata", x.responseMetadata())

    Json.obj(fields.toSeq: _*)
  }

  implicit val describeLimitsRequestEncoder
      : Encoder[kin.DescribeLimitsRequest] =
    _ => Json.obj()

  implicit val describeLimitsResponseEncoder
      : Encoder[kin.DescribeLimitsResponse] =
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

  implicit val describeStreamRequestEncoder
      : Encoder[kin.DescribeStreamRequest] =
    x => {
      val fields: Map[String, Json] = Map
        .empty[String, Json]
        .safeAdd("exclusiveStartShardId", x.exclusiveStartShardId())
        .safeAdd("limit", x.limit())
        .safeAdd("streamARN", x.streamARN())
        .safeAdd("streamName", x.streamName())

      Json.obj(fields.toSeq: _*)
    }

  implicit val describeStreamResponseEncoder
      : Encoder[kin.DescribeStreamResponse] =
    x => {
      val fields: Map[String, Json] = Map
        .empty[String, Json]
        .safeAdd("streamDescription", x.streamDescription())
        .safeAdd("responseMetadata", x.responseMetadata())

      Json.obj(fields.toSeq: _*)
    }

  implicit val describeStreamConsumerRequestEncoder
      : Encoder[kin.DescribeStreamConsumerRequest] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("streamARN", x.streamARN())
      .safeAdd("consumerARN", x.consumerARN())
      .safeAdd("consumerName", x.consumerName())

    Json.obj(fields.toSeq: _*)
  }

  implicit val describeStreamConsumerResponseEncoder
      : Encoder[kin.DescribeStreamConsumerResponse] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("consumerDescription", x.consumerDescription())
      .safeAdd("responseMetadata", x.responseMetadata())

    Json.obj(fields.toSeq: _*)
  }

  implicit val describeStreamSummaryRequestEncoder
      : Encoder[kin.DescribeStreamSummaryRequest] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("streamARN", x.streamARN())
      .safeAdd("streamName", x.streamName())

    Json.obj(fields.toSeq: _*)
  }

  implicit val describeStreamSummaryResponseEncoder
      : Encoder[kin.DescribeStreamSummaryResponse] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("streamDescriptionSummary", x.streamDescriptionSummary())
      .safeAdd("responseMetadata", x.responseMetadata())

    Json.obj(fields.toSeq: _*)
  }

  implicit val disableEnhancedMonitoringEncoder
      : Encoder[kin.DisableEnhancedMonitoringRequest] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("shardLevelMetrics", x.shardLevelMetricsAsStrings())
      .safeAdd("streamName", x.streamName())
      .safeAdd("streamARN", x.streamARN())

    Json.obj(fields.toSeq: _*)
  }

  implicit val disableEnhancedMonitoringResponseEncoder
      : Encoder[kin.DisableEnhancedMonitoringResponse] = x => {
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
      .safeAdd(
        "hasCurrentShardLevelMetrics",
        x.hasCurrentShardLevelMetrics()
      )
      .safeAdd(
        "hasDesiredShardLevelMetrics",
        x.hasDesiredShardLevelMetrics()
      )
      .safeAdd("streamARN", x.streamARN())
      .safeAdd("streamName", x.streamName())
      .safeAdd("responseMetadata", x.responseMetadata())

    Json.obj(fields.toSeq: _*)
  }

  implicit val enableEnhancedMonitoringEncoder
      : Encoder[kin.EnableEnhancedMonitoringRequest] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("shardLevelMetrics", x.shardLevelMetricsAsStrings())
      .safeAdd("streamName", x.streamName())
      .safeAdd("streamARN", x.streamARN())

    Json.obj(fields.toSeq: _*)
  }

  implicit val enableEnhancedMonitoringResponseEncoder
      : Encoder[kin.EnableEnhancedMonitoringResponse] = x => {
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
      .safeAdd(
        "hasCurrentShardLevelMetrics",
        x.hasCurrentShardLevelMetrics()
      )
      .safeAdd(
        "hasDesiredShardLevelMetrics",
        x.hasDesiredShardLevelMetrics()
      )
      .safeAdd("streamARN", x.streamARN())
      .safeAdd("streamName", x.streamName())
      .safeAdd("responseMetadata", x.responseMetadata())

    Json.obj(fields.toSeq: _*)
  }

  implicit val getRecordsRequestEncoder: Encoder[kin.GetRecordsRequest] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("limit", x.limit())
      .safeAdd("shardIterator", x.shardIterator())
      .safeAdd("streamARN", x.streamARN())

    Json.obj(fields.toSeq: _*)
  }

  implicit val getRecordsResponseEncoder: Encoder[kin.GetRecordsResponse] = x =>
    {
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
      : Encoder[kin.GetShardIteratorRequest] = x => {
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
      : Encoder[kin.GetShardIteratorResponse] =
    x => {
      val fields: Map[String, Json] = Map
        .empty[String, Json]
        .safeAdd("shardIterator", x.shardIterator())

      Json.obj(fields.toSeq: _*)
    }

  implicit val increaseStreamRetentionPeriodRequestEncoder
      : Encoder[kin.IncreaseStreamRetentionPeriodRequest] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("streamARN", x.streamARN())
      .safeAdd("streamName", x.streamName())
      .safeAdd("retentionPeriodHours", x.retentionPeriodHours())

    Json.obj(fields.toSeq: _*)
  }

  implicit val increaseStreamRetentionPeriodResponseEncoder
      : Encoder[kin.IncreaseStreamRetentionPeriodResponse] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("responseMetadata", x.responseMetadata())

    Json.obj(fields.toSeq: _*)
  }

  implicit val listShardsRequestEncoder: Encoder[kin.ListShardsRequest] = x => {
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

  implicit val listShardsResponseEncoder: Encoder[kin.ListShardsResponse] = x =>
    {
      val fields: Map[String, Json] = Map
        .empty[String, Json]
        .safeAdd("hasShards", x.hasShards())
        .safeAdd("nextToken", x.nextToken())
        .safeAdd("shards", x.shards())
        .safeAdd("responseMetadata", x.responseMetadata())

      Json.obj(fields.toSeq: _*)
    }

  implicit val listStreamConsumersRequestEncoder
      : Encoder[kin.ListStreamConsumersRequest] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("maxResults", x.maxResults())
      .safeAdd("nextToken", x.nextToken())
      .safeAdd("streamARN", x.streamARN())
      .safeAdd("streamCreationTimestamp", x.streamCreationTimestamp())

    Json.obj(fields.toSeq: _*)
  }

  implicit val listStreamConsumersResponseEncoder
      : Encoder[kin.ListStreamConsumersResponse] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("consumers", x.consumers())
      .safeAdd("hasConsumers", x.hasConsumers())
      .safeAdd("nextToken", x.nextToken())
      .safeAdd("responseMetadata", x.responseMetadata())

    Json.obj(fields.toSeq: _*)
  }

  implicit val listStreamsRequestEncoder: Encoder[kin.ListStreamsRequest] = x =>
    {
      val fields: Map[String, Json] = Map
        .empty[String, Json]
        .safeAdd("exclusiveStartStreamName", x.exclusiveStartStreamName())
        .safeAdd("limit", x.limit())
        .safeAdd("nextToken", x.nextToken())

      Json.obj(fields.toSeq: _*)
    }

  implicit val listStreamsResponseEncoder: Encoder[kin.ListStreamsResponse] =
    x => {
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
      : Encoder[kin.ListTagsForStreamRequest] =
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
      : Encoder[kin.ListTagsForStreamResponse] =
    x => {
      val fields: Map[String, Json] = Map
        .empty[String, Json]
        .safeAdd("hasMoreTags", x.hasMoreTags())
        .safeAdd("hasTags", x.hasTags())
        .safeAdd("tags", x.tags())
        .safeAdd("responseMetadata", x.responseMetadata())

      Json.obj(fields.toSeq: _*)
    }

  implicit val mergeShardsRequestEncoder: Encoder[kin.MergeShardsRequest] =
    x => {
      val fields: Map[String, Json] = Map
        .empty[String, Json]
        .safeAdd("adjacentShardToMerge", x.adjacentShardToMerge())
        .safeAdd("shardToMerge", x.shardToMerge())
        .safeAdd("streamARN", x.streamARN())
        .safeAdd("streamName", x.streamName())

      Json.obj(fields.toSeq: _*)
    }

  implicit val mergeShardsResponseEncoder: Encoder[kin.MergeShardsResponse] =
    x => {
      val fields: Map[String, Json] = Map
        .empty[String, Json]
        .safeAdd("responseMetadata", x.responseMetadata())

      Json.obj(fields.toSeq: _*)
    }

  implicit val putRecordRequestEncoder: Encoder[kin.PutRecordRequest] =
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

  implicit val putRecordResponseEncoder: Encoder[kin.PutRecordResponse] =
    x => {
      val fields: Map[String, Json] = Map
        .empty[String, Json]
        .safeAdd("encryptionType", x.encryptionTypeAsString())
        .safeAdd("sequenceNumber", x.sequenceNumber())
        .safeAdd("shardId", x.shardId())
        .safeAdd("responseMetadata", x.responseMetadata())

      Json.obj(fields.toSeq: _*)
    }

  implicit val putRecordsRequestEncoder: Encoder[kin.PutRecordsRequest] =
    x => {
      val fields: Map[String, Json] = Map
        .empty[String, Json]
        .safeAdd("hasRecords", x.hasRecords())
        .safeAdd("records", x.records())
        .safeAdd("streamARN", x.streamARN())
        .safeAdd("streamName", x.streamName())

      Json.obj(fields.toSeq: _*)
    }

  implicit val putRecordsResponseEncoder: Encoder[kin.PutRecordsResponse] =
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
      : Encoder[kin.RegisterStreamConsumerRequest] =
    x => {
      val fields: Map[String, Json] = Map
        .empty[String, Json]
        .safeAdd("consumerName", x.consumerName())
        .safeAdd("streamARN", x.streamARN())

      Json.obj(fields.toSeq: _*)
    }

  implicit val registerStreamConsumerResponseEncoder
      : Encoder[kin.RegisterStreamConsumerResponse] =
    x => {
      val fields: Map[String, Json] = Map
        .empty[String, Json]
        .safeAdd("consumer", x.consumer())
        .safeAdd("responseMetadata", x.responseMetadata())

      Json.obj(fields.toSeq: _*)
    }

  implicit val removeTagsFromStreamRequestEncoder
      : Encoder[kin.RemoveTagsFromStreamRequest] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("hasTagKeys", x.hasTagKeys())
      .safeAdd("streamName", x.streamName())
      .safeAdd("streamARN", x.streamARN())
      .safeAdd("tagKeys", x.tagKeys())

    Json.obj(fields.toSeq: _*)
  }

  implicit val removeTagsFromStreamResponseEncoder
      : Encoder[kin.RemoveTagsFromStreamResponse] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("responseMetadata", x.responseMetadata())

    Json.obj(fields.toSeq: _*)
  }

  implicit val splitShardRequestEncoder: Encoder[kin.SplitShardRequest] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("newStartingHashKey", x.newStartingHashKey())
      .safeAdd("shardToSplit", x.shardToSplit())
      .safeAdd("streamName", x.streamName())
      .safeAdd("streamARN", x.streamARN())

    Json.obj(fields.toSeq: _*)
  }

  implicit val splitShardResponseEncoder: Encoder[kin.SplitShardResponse] = x =>
    {
      val fields: Map[String, Json] = Map
        .empty[String, Json]
        .safeAdd("responseMetadata", x.responseMetadata())

      Json.obj(fields.toSeq: _*)
    }

  implicit val startStreamEncryptionRequestEncoder
      : Encoder[kin.StartStreamEncryptionRequest] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("encryptionTypeAsString", x.encryptionTypeAsString())
      .safeAdd("keyId", x.keyId())
      .safeAdd("streamName", x.streamName())
      .safeAdd("streamARN", x.streamARN())

    Json.obj(fields.toSeq: _*)
  }

  implicit val startStreamEncryptionResponseEncoder
      : Encoder[kin.StartStreamEncryptionResponse] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("responseMetadata", x.responseMetadata())

    Json.obj(fields.toSeq: _*)
  }

  implicit val stopStreamEncryptionRequestEncoder
      : Encoder[kin.StopStreamEncryptionRequest] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("encryptionTypeAsString", x.encryptionTypeAsString())
      .safeAdd("keyId", x.keyId())
      .safeAdd("streamName", x.streamName())
      .safeAdd("streamARN", x.streamARN())

    Json.obj(fields.toSeq: _*)
  }

  implicit val stopStreamEncryptionResponseEncoder
      : Encoder[kin.StopStreamEncryptionResponse] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("responseMetadata", x.responseMetadata())

    Json.obj(fields.toSeq: _*)
  }

  implicit val subscribeToShardRequestEncoder
      : Encoder[kin.SubscribeToShardRequest] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("consumerARN", x.consumerARN())
      .safeAdd("shardId", x.shardId())
      .safeAdd("startingPosition", x.startingPosition())

    Json.obj(fields.toSeq: _*)
  }

  implicit val subscribeToShardResponseEncoder
      : Encoder[kin.SubscribeToShardResponse] =
    x => {
      val fields: Map[String, Json] = Map
        .empty[String, Json]
        .safeAdd("responseMetadata", x.responseMetadata())

      Json.obj(fields.toSeq: _*)
    }

  implicit val updateShardCountRequestEncoder
      : Encoder[kin.UpdateShardCountRequest] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("scalingType", x.scalingTypeAsString())
      .safeAdd("streamARN", x.streamARN())
      .safeAdd("streamName", x.streamName())
      .safeAdd("targetShardCount", x.targetShardCount())

    Json.obj(fields.toSeq: _*)
  }

  implicit val updateShardCountResponseEncoder
      : Encoder[kin.UpdateShardCountResponse] =
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
      : Encoder[kin.UpdateStreamModeRequest] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("streamARN", x.streamARN())
      .safeAdd("streamModeDetails", x.streamModeDetails())

    Json.obj(fields.toSeq: _*)
  }

  implicit val updateStreamModeResponseEncoder
      : Encoder[kin.UpdateStreamModeResponse] =
    x => {
      val fields: Map[String, Json] = Map
        .empty[String, Json]
        .safeAdd("responseMetadata", x.responseMetadata())

      Json.obj(fields.toSeq: _*)
    }

  implicit val sdkEventTypeEncoder
      : Encoder[kin.SubscribeToShardEventStream.EventType] =
    Encoder[String].contramap(_.toString())

  implicit val subscribeToShardEventEncoder
      : Encoder[kin.SubscribeToShardEvent] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("childShards", x.childShards())
      .safeAdd("continuationSequenceNumber", x.continuationSequenceNumber())
      .safeAdd("hasChildShards", x.hasChildShards())
      .safeAdd("hasRecords", x.hasRecords())
      .safeAdd("millisBehindLatest", x.millisBehindLatest())
      .safeAdd("records", x.records())
      .safeAdd("sdkEventType", x.sdkEventType())

    Json.obj(fields.toSeq: _*)
  }

  implicit val kinesisClientLogEncoders: KinesisClient.LogEncoders =
    new KinesisClient.LogEncoders()

  implicit val attributeDefinitionEncoder: Encoder[ddb.AttributeDefinition] =
    x => {
      val fields: Map[String, Json] = Map
        .empty[String, Json]
        .safeAdd("attributeName", x.attributeName())
        .safeAdd("attributeType", x.attributeTypeAsString())

      Json.obj(fields.toSeq: _*)
    }

  implicit val keySchemaElementEncoder: Encoder[ddb.KeySchemaElement] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("attributeName", x.attributeName())
      .safeAdd("keyType", x.keyTypeAsString())

    Json.obj(fields.toSeq: _*)
  }

  implicit val projectionEncoder: Encoder[ddb.Projection] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("hasNonKeyAttributes", x.hasNonKeyAttributes())
      .safeAdd("nonKeyAttributes", x.nonKeyAttributes())
      .safeAdd("projectionType", x.projectionTypeAsString())

    Json.obj(fields.toSeq: _*)
  }

  implicit val provisionedThroughputEncoder
      : Encoder[ddb.ProvisionedThroughput] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("readCapacityUnits", x.readCapacityUnits())
      .safeAdd("writeCapacityUnits", x.writeCapacityUnits())

    Json.obj(fields.toSeq: _*)
  }

  implicit val globalSecondaryIndexEncoder: Encoder[ddb.GlobalSecondaryIndex] =
    x => {
      val fields: Map[String, Json] = Map
        .empty[String, Json]
        .safeAdd("hasKeySchema", x.hasKeySchema())
        .safeAdd("indexName", x.indexName())
        .safeAdd("keySchema", x.keySchema())
        .safeAdd("projection", x.projection())
        .safeAdd("provisionedThroughput", x.provisionedThroughput())

      Json.obj(fields.toSeq: _*)
    }

  implicit val localSecondaryIndexEncoder: Encoder[ddb.LocalSecondaryIndex] =
    x => {
      val fields: Map[String, Json] = Map
        .empty[String, Json]
        .safeAdd("hasKeySchema", x.hasKeySchema())
        .safeAdd("indexName", x.indexName())
        .safeAdd("keySchema", x.keySchema())
        .safeAdd("projection", x.projection())

      Json.obj(fields.toSeq: _*)
    }

  implicit val sseSpecificationEncoder: Encoder[ddb.SSESpecification] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("enabled", x.enabled())
      .safeAdd("kmsMasterKeyId", x.kmsMasterKeyId())
      .safeAdd("sseType", x.sseTypeAsString())

    Json.obj(fields.toSeq: _*)
  }

  implicit val streamSpecificationEncoder: Encoder[ddb.StreamSpecification] =
    x => {
      val fields: Map[String, Json] = Map
        .empty[String, Json]
        .safeAdd("streamEnabled", x.streamEnabled())
        .safeAdd("streamViewType", x.streamViewTypeAsString())

      Json.obj(fields.toSeq: _*)
    }

  implicit val ddbTagEncoder: Encoder[ddb.Tag] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("key", x.key())
      .safeAdd("value", x.value())

    Json.obj(fields.toSeq: _*)
  }

  implicit val archivalSummaryEncoder: Encoder[ddb.ArchivalSummary] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("archivalBackupArn", x.archivalBackupArn())
      .safeAdd("archivalDateTime", x.archivalDateTime())
      .safeAdd("archivalReason", x.archivalReason())

    Json.obj(fields.toSeq: _*)
  }

  implicit val billingModeSummaryEncoder: Encoder[ddb.BillingModeSummary] = x =>
    {
      val fields: Map[String, Json] = Map
        .empty[String, Json]
        .safeAdd("billingMode", x.billingModeAsString())
        .safeAdd(
          "lastUpdateToPayPerRequestDateTime",
          x.lastUpdateToPayPerRequestDateTime()
        )

      Json.obj(fields.toSeq: _*)
    }

  implicit val provisionedThroughputDescriptionEncoder
      : Encoder[ddb.ProvisionedThroughputDescription] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("lastDecreaseDateTime", x.lastDecreaseDateTime())
      .safeAdd("lastIncreaseDateTime", x.lastIncreaseDateTime())
      .safeAdd("numberOfDecreasesToday", x.numberOfDecreasesToday())
      .safeAdd("readCapacityUnits", x.readCapacityUnits())
      .safeAdd("writeCapacityUnits", x.writeCapacityUnits())

    Json.obj(fields.toSeq: _*)
  }

  implicit val globalSecondaryIndexDescriptionEncoder
      : Encoder[ddb.GlobalSecondaryIndexDescription] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("backfilling", x.backfilling())
      .safeAdd("hasKeySchema", x.hasKeySchema())
      .safeAdd("indexArn", x.indexArn())
      .safeAdd("indexName", x.indexName())
      .safeAdd("indexSizeBytes", x.indexSizeBytes())
      .safeAdd("indexStatus", x.indexStatusAsString())
      .safeAdd("itemCount", x.itemCount())
      .safeAdd("keySchema", x.keySchema())
      .safeAdd("projection", x.projection())
      .safeAdd("provisionedThroughput", x.provisionedThroughput())

    Json.obj(fields.toSeq: _*)
  }

  implicit val localSecondaryIndexDescriptionEncoder
      : Encoder[ddb.LocalSecondaryIndexDescription] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("hasKeySchema", x.hasKeySchema())
      .safeAdd("indexArn", x.indexArn())
      .safeAdd("indexName", x.indexName())
      .safeAdd("indexSizeBytes", x.indexSizeBytes())
      .safeAdd("itemCount", x.itemCount())
      .safeAdd("keySchema", x.keySchema())
      .safeAdd("projection", x.projection())

    Json.obj(fields.toSeq: _*)
  }

  implicit val provisionedThroughputOverrideEncoder
      : Encoder[ddb.ProvisionedThroughputOverride] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("readCapacityUnits", x.readCapacityUnits())

    Json.obj(fields.toSeq: _*)
  }

  implicit val replicaGlobalSecondaryIndexDescriptionEncoder
      : Encoder[ddb.ReplicaGlobalSecondaryIndexDescription] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("indexName", x.indexName())
      .safeAdd(
        "provisionedThroughputOverride",
        x.provisionedThroughputOverride()
      )

    Json.obj(fields.toSeq: _*)
  }

  implicit val tableClassSummaryEncoder: Encoder[ddb.TableClassSummary] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("lastUpdateDateTime", x.lastUpdateDateTime())
      .safeAdd("tableClass", x.tableClassAsString())

    Json.obj(fields.toSeq: _*)
  }

  implicit val replicaDescriptionEncoder: Encoder[ddb.ReplicaDescription] = x =>
    {
      val fields: Map[String, Json] = Map
        .empty[String, Json]
        .safeAdd("globalSecondaryIndexes", x.globalSecondaryIndexes())
        .safeAdd("hasGlobalSecondaryIndexes", x.hasGlobalSecondaryIndexes())
        .safeAdd("kmsMasterKeyId", x.kmsMasterKeyId())
        .safeAdd(
          "provisionedThroughputOverride",
          x.provisionedThroughputOverride()
        )
        .safeAdd("regionName", x.regionName())
        .safeAdd("replicaInaccessibleDateTime", x.replicaInaccessibleDateTime())
        .safeAdd("replicaStatus", x.replicaStatusAsString())
        .safeAdd("replicaStatusDescription", x.replicaStatusDescription())
        .safeAdd(
          "replicaStatusPercentProgress",
          x.replicaStatusPercentProgress()
        )
        .safeAdd("replicaTableClassSummary", x.replicaTableClassSummary())

      Json.obj(fields.toSeq: _*)
    }

  implicit val restoreSummaryEncoder: Encoder[ddb.RestoreSummary] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("restoreDateTime", x.restoreDateTime())
      .safeAdd("restoreInProgress", x.restoreInProgress())
      .safeAdd("sourceBackupArn", x.sourceBackupArn())
      .safeAdd("sourceTableArn", x.sourceTableArn())

    Json.obj(fields.toSeq: _*)
  }

  implicit val sseDescriptionEncoder: Encoder[ddb.SSEDescription] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd(
        "inaccessibleEncryptionDateTime",
        x.inaccessibleEncryptionDateTime()
      )
      .safeAdd("kmsMasterKeyArn", x.kmsMasterKeyArn())
      .safeAdd("sseType", x.sseTypeAsString())
      .safeAdd("status", x.statusAsString())

    Json.obj(fields.toSeq: _*)
  }

  implicit val tableDescriptionEncoder: Encoder[ddb.TableDescription] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("archivalSummary", x.archivalSummary())
      .safeAdd("attributeDefinitions", x.attributeDefinitions())
      .safeAdd("billingModeSummary", x.billingModeSummary())
      .safeAdd("creationDateTime", x.creationDateTime())
      .safeAdd("globalSecondaryIndexes", x.globalSecondaryIndexes())
      .safeAdd("globalTableVersion", x.globalTableVersion())
      .safeAdd("hasAttributeDefinitions", x.hasAttributeDefinitions())
      .safeAdd("hasGlobalSecondaryIndexes", x.hasGlobalSecondaryIndexes())
      .safeAdd("hasKeySchema", x.hasKeySchema())
      .safeAdd("hasLocalSecondaryIndexes", x.hasLocalSecondaryIndexes())
      .safeAdd("hasReplicas", x.hasReplicas())
      .safeAdd("itemCount", x.itemCount())
      .safeAdd("keySchema", x.keySchema())
      .safeAdd("latestStreamArn", x.latestStreamArn())
      .safeAdd("latestStreamLabel", x.latestStreamLabel())
      .safeAdd("localSecondaryIndexes", x.localSecondaryIndexes())
      .safeAdd("provisionedThroughput", x.provisionedThroughput())
      .safeAdd("replicas", x.replicas())
      .safeAdd("restoreSummary", x.restoreSummary())
      .safeAdd("sseDescription", x.sseDescription())
      .safeAdd("streamSpecification", x.streamSpecification())
      .safeAdd("tableArn", x.tableArn())
      .safeAdd("tableClassSummary", x.tableClassSummary())
      .safeAdd("tableId", x.tableId())
      .safeAdd("tableName", x.tableName())
      .safeAdd("tableSizeBytes", x.tableSizeBytes())
      .safeAdd("tableStatus", x.tableStatusAsString())

    Json.obj(fields.toSeq: _*)
  }

  private def attributeValueMapJsonImpl(
      x: List[(String, ddb.AttributeValue)],
      res: Json = Json.obj()
  ): Eval[Json] = Eval.defer(x match {
    case Nil => Eval.now(res.asJson)
    case (k, v) :: t =>
      attributeValueJsonImpl(v).flatMap(x =>
        attributeValueMapJsonImpl(t, res.withObject(_.add(k, x).asJson))
      )
  })

  private def attributeValueListJsonImpl(
      x: List[ddb.AttributeValue],
      res: List[Json] = Nil
  ): Eval[Json] = Eval.defer(x match {
    case Nil => Eval.now(x.reverse.asJson)
    case v :: t =>
      attributeValueJsonImpl(v).flatMap(x =>
        attributeValueListJsonImpl(t, res :+ x)
      )
  })

  private def attributeValueJsonImpl(x: ddb.AttributeValue): Eval[Json] = {
    val t = Option(x.`type`)

    val value: Eval[Json] = t match {
      case Some(ddb.AttributeValue.Type.BOOL) =>
        Eval.now(Json.obj("bool" -> x.bool().asJson))
      case Some(ddb.AttributeValue.Type.B) =>
        Eval.now(Json.obj("b" -> x.b().asJson))
      case Some(ddb.AttributeValue.Type.BS) =>
        Eval.now(Json.obj("bs" -> x.bs().asJson))
      case Some(ddb.AttributeValue.Type.S) =>
        Eval.now(Json.obj("s" -> x.s().asJson))
      case Some(ddb.AttributeValue.Type.SS) =>
        Eval.now(Json.obj("ss" -> x.ss().asJson))
      case Some(ddb.AttributeValue.Type.N) =>
        Eval.now(Json.obj("n" -> x.n().asJson))
      case Some(ddb.AttributeValue.Type.NS) =>
        Eval.now(Json.obj("ns" -> x.ns().asJson))
      case Some(ddb.AttributeValue.Type.NUL) =>
        Eval.now(Json.obj("nul" -> x.nul().asJson))
      case Some(ddb.AttributeValue.Type.UNKNOWN_TO_SDK_VERSION) =>
        Eval.now(Json.obj("UNKNOWN_TO_SDK_VERSION" -> "".asJson))
      case Some(ddb.AttributeValue.Type.M) =>
        attributeValueMapJsonImpl(x.m().asScala.toList)
      case Some(ddb.AttributeValue.Type.L) =>
        attributeValueListJsonImpl(x.l().asScala.toList)
      case None => Eval.now(Json.obj())
    }
    t.fold(value)(y =>
      value.map(v => v.withObject(_.add("type", y.name().asJson).asJson))
    )
  }

  implicit val attributeValueEncoder: Encoder[ddb.AttributeValue] = x =>
    attributeValueJsonImpl(x).value

  implicit val attributeValueUpdateEncoder: Encoder[ddb.AttributeValueUpdate] =
    x => {
      val fields: Map[String, Json] = Map
        .empty[String, Json]
        .safeAdd("action", x.actionAsString())
        .safeAdd("value", x.value())

      Json.obj(fields.toSeq: _*)
    }

  implicit val capacityEncoder: Encoder[ddb.Capacity] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("capacityUnits", x.capacityUnits())
      .safeAdd("readCapacityUnits", x.readCapacityUnits())
      .safeAdd("writeCapacityUnits", x.writeCapacityUnits())

    Json.obj(fields.toSeq: _*)
  }

  implicit val consumedCapacityEncoder: Encoder[ddb.ConsumedCapacity] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("capacityUnits", x.capacityUnits())
      .safeAdd("globalSecondaryIndexes", x.globalSecondaryIndexes())
      .safeAdd("hasGlobalSecondaryIndexes", x.hasGlobalSecondaryIndexes())
      .safeAdd("hasLocalSecondaryIndexes", x.hasLocalSecondaryIndexes())
      .safeAdd("localSecondaryIndexes", x.localSecondaryIndexes())
      .safeAdd("readCapacityUnits", x.readCapacityUnits())
      .safeAdd("table", x.table())
      .safeAdd("tableName", x.tableName())
      .safeAdd("writeCapacityUnits", x.writeCapacityUnits())

    Json.obj(fields.toSeq: _*)
  }

  implicit val ddbConditionEncoder: Encoder[ddb.Condition] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("attributeValueList", x.attributeValueList())
      .safeAdd("comparisonOperator", x.comparisonOperatorAsString())
      .safeAdd("hasAttributeValueList", x.hasAttributeValueList())

    Json.obj(fields.toSeq: _*)
  }

  implicit val expectedAttributeValueEncoder
      : Encoder[ddb.ExpectedAttributeValue] =
    x => {
      val fields: Map[String, Json] = Map
        .empty[String, Json]
        .safeAdd("attributeValueList", x.attributeValueList())
        .safeAdd("comparisonOperator", x.comparisonOperatorAsString())
        .safeAdd("exists", x.exists())
        .safeAdd("hasAttributeValueList", x.hasAttributeValueList())
        .safeAdd("value", x.value())

      Json.obj(fields.toSeq: _*)
    }

  implicit val itemCollectionMetricsEncoder
      : Encoder[ddb.ItemCollectionMetrics] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("hasItemCollectionKey", x.hasItemCollectionKey())
      .safeAdd("hasSizeEstimateRangeGB", x.hasSizeEstimateRangeGB())
      .safeAdd("itemCollectionKey", x.itemCollectionKey())
      .safeAdd("sizeEstimateRangeGB", x.sizeEstimateRangeGB())

    Json.obj(fields.toSeq: _*)
  }

  implicit val createTableRequestEncoder: Encoder[ddb.CreateTableRequest] = x =>
    {
      val fields: Map[String, Json] = Map
        .empty[String, Json]
        .safeAdd("attributeDefinitions", x.attributeDefinitions())
        .safeAdd("billingMode", x.billingModeAsString())
        .safeAdd("globalSecondaryIndexes", x.globalSecondaryIndexes())
        .safeAdd("hasAttributeDefinitions", x.hasAttributeDefinitions())
        .safeAdd("hasGlobalSecondaryIndexes", x.hasGlobalSecondaryIndexes())
        .safeAdd("hasKeySchema", x.hasKeySchema())
        .safeAdd("hasLocalSecondaryIndexes", x.hasLocalSecondaryIndexes())
        .safeAdd("hasTags", x.hasTags())
        .safeAdd("keySchema", x.keySchema())
        .safeAdd("localSecondaryIndexes", x.localSecondaryIndexes())
        .safeAdd("provisionedThroughput", x.provisionedThroughput())
        .safeAdd("sseSpecification", x.sseSpecification())
        .safeAdd("streamSpecification", x.streamSpecification())
        .safeAdd("tableClass", x.tableClassAsString())
        .safeAdd("tableName", x.tableName())
        .safeAdd("tags", x.tags())

      Json.obj(fields.toSeq: _*)
    }

  implicit val createTableResponseEncoder: Encoder[ddb.CreateTableResponse] =
    x => {
      val fields: Map[String, Json] = Map
        .empty[String, Json]
        .safeAdd("tableDescription", x.tableDescription())

      Json.obj(fields.toSeq: _*)
    }

  implicit val describeTableRequestEncoder: Encoder[ddb.DescribeTableRequest] =
    x => {
      val fields: Map[String, Json] = Map
        .empty[String, Json]
        .safeAdd("tableName", x.tableName())

      Json.obj(fields.toSeq: _*)
    }

  implicit val describeTableResponseEncoder
      : Encoder[ddb.DescribeTableResponse] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("table", x.table())

    Json.obj(fields.toSeq: _*)
  }

  implicit val scanRequestEncoder: Encoder[ddb.ScanRequest] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("attributesToGet", x.attributesToGet())
      .safeAdd("conditionalOperator", x.conditionalOperatorAsString())
      .safeAdd("consistentRead", x.consistentRead())
      .safeAdd("exclusiveStartKey", x.exclusiveStartKey())
      .safeAdd("expressionAttributeNames", x.expressionAttributeNames())
      .safeAdd("expressionAttributeValues", x.expressionAttributeValues())
      .safeAdd("hasAttributesToGet", x.hasAttributesToGet())
      .safeAdd("hasExclusiveStartKey", x.hasExclusiveStartKey())
      .safeAdd("hasExpressionAttributeNames", x.hasExpressionAttributeNames())
      .safeAdd("hasExpressionAttributeValues", x.hasExpressionAttributeValues())
      .safeAdd("hasScanFilter", x.hasScanFilter())
      .safeAdd("indexName", x.indexName())
      .safeAdd("limit", x.limit())
      .safeAdd("projectionExpression", x.projectionExpression())
      .safeAdd("returnConsumedCapacity", x.returnConsumedCapacityAsString())
      .safeAdd("scanFilter", x.scanFilter())
      .safeAdd("segment", x.segment())
      .safeAdd("select", x.selectAsString())
      .safeAdd("tableName", x.tableName())
      .safeAdd("totalSegments", x.totalSegments())

    Json.obj(fields.toSeq: _*)
  }

  implicit val scanResponseEncoder: Encoder[ddb.ScanResponse] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("consumedCapacity", x.consumedCapacity())
      .safeAdd("count", x.count())
      .safeAdd("hasItems", x.hasItems())
      .safeAdd("hasLastEvaluatedKey", x.hasLastEvaluatedKey())
      .safeAdd("items", x.items())
      .safeAdd("lastEvaluatedKey", x.lastEvaluatedKey())
      .safeAdd("scannedCount", x.scannedCount())

    Json.obj(fields.toSeq: _*)
  }

  implicit val updateItemRequestEncoder: Encoder[ddb.UpdateItemRequest] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("attributeUpdates", x.attributeUpdates())
      .safeAdd("conditionExpression", x.conditionExpression())
      .safeAdd("conditionalOperator", x.conditionalOperatorAsString())
      .safeAdd("expected", x.expected())
      .safeAdd("expressionAttributeNames", x.expressionAttributeNames())
      .safeAdd("expressionAttributeValues", x.expressionAttributeValues())
      .safeAdd("hasAttributeUpdates", x.hasAttributeUpdates())
      .safeAdd("hasExpected", x.hasExpected())
      .safeAdd("hasExpressionAttributeNames", x.hasExpressionAttributeNames())
      .safeAdd("hasExpressionAttributeValues", x.hasExpressionAttributeValues())
      .safeAdd("hasKey", x.hasKey())
      .safeAdd("key", x.key())
      .safeAdd("returnConsumedCapacity", x.returnConsumedCapacityAsString())
      .safeAdd(
        "returnItemCollectionMetrics",
        x.returnItemCollectionMetricsAsString()
      )
      .safeAdd("returnValues", x.returnValuesAsString())
      .safeAdd("tableName", x.tableName())
      .safeAdd("updateExpression", x.updateExpression())

    Json.obj(fields.toSeq: _*)
  }

  implicit val updateItemResponseEncoder: Encoder[ddb.UpdateItemResponse] = x =>
    {
      val fields: Map[String, Json] = Map
        .empty[String, Json]
        .safeAdd("attributes", x.attributes())
        .safeAdd("consumedCapacity", x.consumedCapacity())
        .safeAdd("hasAttributes", x.hasAttributes())
        .safeAdd("itemCollectionMetrics", x.itemCollectionMetrics())

      Json.obj(fields.toSeq: _*)
    }

  implicit val putItemRequestEncoder: Encoder[ddb.PutItemRequest] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("conditionExpression", x.conditionExpression())
      .safeAdd("conditionalOperator", x.conditionalOperatorAsString())
      .safeAdd("expected", x.expected())
      .safeAdd("expressionAttributeNames", x.expressionAttributeNames())
      .safeAdd("expressionAttributeValues", x.expressionAttributeValues())
      .safeAdd("hasExpected", x.hasExpected())
      .safeAdd("hasExpressionAttributeNames", x.hasExpressionAttributeNames())
      .safeAdd("hasExpressionAttributeValues", x.hasExpressionAttributeValues())
      .safeAdd("item", x.item())
      .safeAdd("returnConsumedCapacity", x.returnConsumedCapacityAsString())
      .safeAdd(
        "returnItemCollectionMetrics",
        x.returnItemCollectionMetricsAsString()
      )
      .safeAdd("returnValues", x.returnValuesAsString())
      .safeAdd("tableName", x.tableName())

    Json.obj(fields.toSeq: _*)
  }

  implicit val putItemResponseEncoder: Encoder[ddb.PutItemResponse] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("attributes", x.attributes())
      .safeAdd("consumedCapacity", x.consumedCapacity())
      .safeAdd("hasAttributes", x.hasAttributes())
      .safeAdd("itemCollectionMetrics", x.itemCollectionMetrics())

    Json.obj(fields.toSeq: _*)
  }

  implicit val deleteTableRequestEncoder: Encoder[ddb.DeleteTableRequest] = x =>
    {
      val fields: Map[String, Json] = Map
        .empty[String, Json]
        .safeAdd("tableName", x.tableName())

      Json.obj(fields.toSeq: _*)
    }

  implicit val deleteTableResponseEncoder: Encoder[ddb.DeleteTableResponse] =
    x => {
      val fields: Map[String, Json] = Map
        .empty[String, Json]
        .safeAdd("tableDescription", x.tableDescription())

      Json.obj(fields.toSeq: _*)
    }

  implicit val dynamoClientLogEncoders: DynamoClient.LogEncoders =
    new DynamoClient.LogEncoders()

  implicit val dimensionEncoder: Encoder[cw.Dimension] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("name", x.name())
      .safeAdd("value", x.value())

    Json.obj(fields.toSeq: _*)
  }

  implicit val statisticSetEncoder: Encoder[cw.StatisticSet] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("maximum", x.maximum())
      .safeAdd("minimum", x.minimum())
      .safeAdd("sampleCount", x.sampleCount())
      .safeAdd("sum", x.sum())

    Json.obj(fields.toSeq: _*)
  }

  implicit val metricDatumEncoder: Encoder[cw.MetricDatum] = x => {
    val fields: Map[String, Json] = Map
      .empty[String, Json]
      .safeAdd("counts", x.counts())
      .safeAdd("dimensions", x.dimensions())
      .safeAdd("hasCounts", x.hasCounts())
      .safeAdd("hasDimensions", x.hasDimensions())
      .safeAdd("hasValues", x.hasValues())
      .safeAdd("metricName", x.metricName())
      .safeAdd("statisticValues", x.statisticValues())
      .safeAdd("storageResolution", x.storageResolution())
      .safeAdd("timestamp", x.timestamp())
      .safeAdd("unit", x.unitAsString())
      .safeAdd("value", x.value())
      .safeAdd("values", x.values())

    Json.obj(fields.toSeq: _*)
  }

  implicit val putMetricDataRequestEncoder: Encoder[cw.PutMetricDataRequest] =
    x => {
      val fields: Map[String, Json] = Map
        .empty[String, Json]
        .safeAdd("hasMetricData", x.hasMetricData())
        .safeAdd("metricData", x.metricData())
        .safeAdd("namespace", x.namespace())

      Json.obj(fields.toSeq: _*)
    }

  implicit val putMetricDataResponseEncoder: Encoder[cw.PutMetricDataResponse] =
    _ => {
      val fields: Map[String, Json] = Map
        .empty[String, Json]

      Json.obj(fields.toSeq: _*)
    }

  implicit val cloudWatchClientLogEncoders: CloudWatchClient.LogEncoders =
    new CloudWatchClient.LogEncoders()

}
