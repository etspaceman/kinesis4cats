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

import software.amazon.awssdk.services.kinesis.model._

import kinesis4cats.logging.LogEncoder

final class KinesisClientLogEncoders(implicit
    val addTagsToStreamRequestLogEncoder: LogEncoder[AddTagsToStreamRequest],
    val addTagsToStreamResponseLogEncoder: LogEncoder[AddTagsToStreamResponse],
    val createStreamRequestLogEncoder: LogEncoder[CreateStreamRequest],
    val createStreamResponseLogEncoder: LogEncoder[CreateStreamResponse],
    val decreaseStreamRetentionPeriodRequestLogEncoder: LogEncoder[
      DecreaseStreamRetentionPeriodRequest
    ],
    val decreaseStreamRetentionPeriodResponseLogEncoder: LogEncoder[
      DecreaseStreamRetentionPeriodResponse
    ],
    val deleteStreamRequestLogEncoder: LogEncoder[DeleteStreamRequest],
    val deleteStreamResponseLogEncoder: LogEncoder[DeleteStreamResponse],
    val deregisterStreamConsumerRequestLogEncoder: LogEncoder[
      DeregisterStreamConsumerRequest
    ],
    val deregisterStreamConsumerResponseLogEncoder: LogEncoder[
      DeregisterStreamConsumerResponse
    ],
    val describeLimitsRequestLogEncoder: LogEncoder[DescribeLimitsRequest],
    val describeLimitsResponseLogEncoder: LogEncoder[DescribeLimitsResponse],
    val describeStreamRequestLogEncoder: LogEncoder[DescribeStreamRequest],
    val describeStreamResponseLogEncoder: LogEncoder[DescribeStreamResponse],
    val describeStreamConsumerRequestLogEncoder: LogEncoder[
      DescribeStreamConsumerRequest
    ],
    val describeStreamConsumerResponseLogEncoder: LogEncoder[
      DescribeStreamConsumerResponse
    ],
    val describeStreamSummaryRequestLogEncoder: LogEncoder[
      DescribeStreamSummaryRequest
    ],
    val describeStreamSummaryResponseLogEncoder: LogEncoder[
      DescribeStreamSummaryResponse
    ],
    val disableEnhancedMonitoringRequestLogEncoder: LogEncoder[
      DisableEnhancedMonitoringRequest
    ],
    val disableEnhancedMonitoringResponseLogEncoder: LogEncoder[
      DisableEnhancedMonitoringResponse
    ],
    val enableEnhancedMonitoringRequestLogEncoder: LogEncoder[
      EnableEnhancedMonitoringRequest
    ],
    val enableEnhancedMonitoringResponseLogEncoder: LogEncoder[
      EnableEnhancedMonitoringResponse
    ],
    val getRecordsRequestLogEncoder: LogEncoder[GetRecordsRequest],
    val getRecordsResponseLogEncoder: LogEncoder[GetRecordsResponse],
    val getShardIteratorRequestLogEncoder: LogEncoder[GetShardIteratorRequest],
    val getShardIteratorResponseLogEncoder: LogEncoder[
      GetShardIteratorResponse
    ],
    val increaseStreamRetentionPeriodRequestLogEncoder: LogEncoder[
      IncreaseStreamRetentionPeriodRequest
    ],
    val increaseStreamRetentionPeriodResponseLogEncoder: LogEncoder[
      IncreaseStreamRetentionPeriodResponse
    ],
    val listShardsRequestLogEncoder: LogEncoder[ListShardsRequest],
    val listShardsResponseLogEncoder: LogEncoder[ListShardsResponse],
    val listStreamConsumersRequestLogEncoder: LogEncoder[
      ListStreamConsumersRequest
    ],
    val listStreamConsumersResponseLogEncoder: LogEncoder[
      ListStreamConsumersResponse
    ],
    val listStreamsRequestLogEncoder: LogEncoder[ListStreamsRequest],
    val listStreamsResponseLogEncoder: LogEncoder[ListStreamsResponse],
    val listTagsForStreamRequestLogEncoder: LogEncoder[
      ListTagsForStreamRequest
    ],
    val listTagsForStreamResponseLogEncoder: LogEncoder[
      ListTagsForStreamResponse
    ],
    val mergeShardsRequestLogEncoder: LogEncoder[MergeShardsRequest],
    val mergeShardsResponseLogEncoder: LogEncoder[MergeShardsResponse],
    val putRecordRequestLogEncoder: LogEncoder[PutRecordRequest],
    val putRecordResponseLogEncoder: LogEncoder[PutRecordResponse],
    val putRecordsRequestLogEncoder: LogEncoder[PutRecordsRequest],
    val putRecordsResponseLogEncoder: LogEncoder[PutRecordsResponse],
    val registerStreamConsumerRequestLogEncoder: LogEncoder[
      RegisterStreamConsumerRequest
    ],
    val registerStreamConsumerResponseLogEncoder: LogEncoder[
      RegisterStreamConsumerResponse
    ],
    val removeTagsFromStreamRequestLogEncoder: LogEncoder[
      RemoveTagsFromStreamRequest
    ],
    val removeTagsFromStreamResponseLogEncoder: LogEncoder[
      RemoveTagsFromStreamResponse
    ],
    val splitShardRequestLogEncoder: LogEncoder[SplitShardRequest],
    val splitShardResponseLogEncoder: LogEncoder[SplitShardResponse],
    val startStreamEncryptionRequestLogEncoder: LogEncoder[
      StartStreamEncryptionRequest
    ],
    val startStreamEncryptionResponseLogEncoder: LogEncoder[
      StartStreamEncryptionResponse
    ],
    val stopStreamEncryptionRequestLogEncoder: LogEncoder[
      StopStreamEncryptionRequest
    ],
    val stopStreamEncryptionResponseLogEncoder: LogEncoder[
      StopStreamEncryptionResponse
    ],
    val subscribeToShardRequestLogEncoder: LogEncoder[SubscribeToShardRequest],
    val subscribeToShardResponseLogEncoder: LogEncoder[
      SubscribeToShardResponse
    ],
    val updateShardCountRequestLogEncoder: LogEncoder[UpdateShardCountRequest],
    val updateShardCountResponseLogEncoder: LogEncoder[
      UpdateShardCountResponse
    ],
    val updateStreamModeRequestLogEncoder: LogEncoder[UpdateStreamModeRequest],
    val updateStreamModeResponseLogEncoder: LogEncoder[UpdateStreamModeResponse]
)
