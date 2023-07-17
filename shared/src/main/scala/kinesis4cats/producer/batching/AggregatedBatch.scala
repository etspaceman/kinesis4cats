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
package producer
package batching

import java.io.ByteArrayOutputStream

import cats.data.NonEmptyList

import kinesis4cats.Utils
import kinesis4cats.models.ShardId
import kinesis4cats.protobuf.messages.AggregatedRecord

/** Represents records that can be aggregated into a single record using the
  * [[https://docs.aws.amazon.com/streams/latest/dev/kinesis-kpl-concepts.html#kinesis-kpl-concepts-aggretation KPL Aggregation Format]].
  * We can aggregate records that share a single partition key. If the shard-map
  * never changed, you could potentially aggregate records for an entire shard,
  * however if a user scales the stream, then that shard map would be out of
  * date.
  *
  * @param shardId
  *   Shard ID for the batch
  * @param records
  *   [[cats.data.NonEmptyList NonEmptyList]] of
  *   [[kinesis4cats.producer.Record.AggregationEntry Record.AggregationEntry]]
  * @param aggregatedMessageSize
  *   Accumulated size of messages in the aggregated data
  * @param explicitHashKeys
  *   Map of explicit hash keys to their known index in the records
  * @param partitionKeys
  *   Map of partition kyes to their known index in the records
  * @param partitionKey
  *   Partition Key for the aggregated record
  * @param config
  *   [[kinesis4cats.producer.batching.Batcher.Config Batcher.Config]]
  */
private[kinesis4cats] final case class AggregatedBatch private (
    shardId: ShardId,
    records: NonEmptyList[Record.AggregationEntry],
    aggregatedMessageSize: Int,
    explicitHashKeys: Map[String, Int],
    partitionKeys: Map[String, Int],
    partitionKey: String,
    config: Batcher.Config
) {

  // See https://github.com/awslabs/kinesis-aggregation/blob/2.0.3/java/KinesisAggregatorV2/src/main/java/com/amazonaws/kinesis/agg/AggRecord.java#L127
  def getSizeBytes: Int =
    Aggregation.magicBytes.length +
      aggregatedMessageSize +
      Aggregation.digestSize

  // See https://github.com/awslabs/kinesis-aggregation/blob/2.0.3/java/KinesisAggregatorV2/src/main/java/com/amazonaws/kinesis/agg/AggRecord.java#L330
  def canAdd(record: Record.WithShard): Boolean =
    (record.aggregatedPayloadSize(
      partitionKeys,
      explicitHashKeys
    ) + getSizeBytes) <= config.maxPayloadSizePerRecord

  def add(record: Record.WithShard): AggregatedBatch = {
    val aggregationEntry =
      record.asAggregationEntry(partitionKeys, explicitHashKeys)

    val newHashKeys = explicitHashKeys
      .get(aggregationEntry.explicitHashKey)
      .fold(
        explicitHashKeys + (aggregationEntry.explicitHashKey -> aggregationEntry.explicitHashKeyTableIndex)
      )(_ => explicitHashKeys)

    val newPartitionKeys = partitionKeys
      .get(aggregationEntry.record.partitionKey)
      .fold(
        partitionKeys + (aggregationEntry.record.partitionKey -> partitionKeys.size)
      )(_ => partitionKeys)

    copy(
      records = records.prepend(aggregationEntry),
      aggregatedMessageSize =
        aggregatedMessageSize + (aggregationEntry.aggregatedPayloadSize(
          partitionKeys,
          explicitHashKeys
        )),
      explicitHashKeys = newHashKeys,
      partitionKeys = newPartitionKeys
    )
  }

  def asAggregatedRecord: AggregatedRecord = AggregatedRecord(
    partitionKeys.keys.toSeq,
    explicitHashKeys.keys.toSeq,
    records.reverse.map(_.asEntry).toList
  )

  // See https://github.com/awslabs/kinesis-aggregation/blob/2.0.3/java/KinesisAggregatorV2/src/main/java/com/amazonaws/kinesis/agg/AggRecord.java#L142
  def asBytes: Array[Byte] = {
    val messageBody: Array[Byte] = asAggregatedRecord.toByteArray
    val messageDigest: Array[Byte] = Utils.md5(messageBody)

    val baos: ByteArrayOutputStream = new ByteArrayOutputStream(getSizeBytes)
    baos.write(
      Aggregation.magicBytes,
      0,
      Aggregation.magicBytes.length
    )
    baos.write(messageBody, 0, messageBody.length)
    baos.write(messageDigest, 0, messageDigest.length)

    val res = baos.toByteArray()

    res
  }

  def asRecord: Record.WithShard = Record.WithShard(
    Record(asBytes, partitionKey, None, None),
    shardId
  )
}

private[kinesis4cats] object AggregatedBatch {
  def create(
      record: Record.WithShard,
      config: Batcher.Config
  ): AggregatedBatch = {
    val aggregationEntry =
      record.asAggregationEntry(Map.empty, Map.empty)
    AggregatedBatch(
      record.predictedShard,
      NonEmptyList.one(aggregationEntry),
      aggregationEntry.aggregatedPayloadSize(Map.empty, Map.empty),
      Map(aggregationEntry.explicitHashKey -> 0),
      Map(record.record.partitionKey -> 0),
      record.record.partitionKey,
      config
    )
  }
}
