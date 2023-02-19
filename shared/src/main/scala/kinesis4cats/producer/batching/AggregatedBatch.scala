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

package kinesis4cats.producer
package batching

import scala.jdk.CollectionConverters._

import java.io.ByteArrayOutputStream
import java.security.MessageDigest

import cats.data.NonEmptyList
import cats.syntax.all._

import kinesis4cats.models.ShardId
import kinesis4cats.producer.Producer
import kinesis4cats.protobuf.Messages.AggregatedRecord

final case class AggregatedBatch private (
    shardId: ShardId,
    records: NonEmptyList[Record.AggregationEntry],
    aggregatedMessageSize: Int,
    explicitHashKeys: Map[String, Int],
    partitionKeys: Map[String, Int],
    digest: MessageDigest,
    aggPartitionKey: String,
    aggExplicitHashKey: Option[String],
    config: Batcher.Config
) {
  def getSizeBytes: Int =
    AggregatedBatch.magicBytes.length +
      aggregatedMessageSize +
      digest.getDigestLength()

  def canAdd(record: Record.WithShard): Boolean =
    (record.aggregatedPayloadSize(
      partitionKeys,
      explicitHashKeys,
      digest
    ) + getSizeBytes) <= config.maxPayloadSizePerRecord

  def add(record: Record.WithShard): AggregatedBatch = {
    val aggregationEntry =
      record.asAggregationEntry(partitionKeys, explicitHashKeys, digest)

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

  def asAggregatedRecord: AggregatedRecord = AggregatedRecord
    .newBuilder()
    .addAllRecords(records.reverse.map(_.asEntry).toList.asJava)
    .addAllPartitionKeyTable(partitionKeys.keys.asJava)
    .addAllExplicitHashKeyTable(explicitHashKeys.keys.asJava)
    .build()

  def asBytes: Array[Byte] = {
    val messageBody: Array[Byte] = asAggregatedRecord.toByteArray()
    val messageDigest: Array[Byte] = digest.digest(messageBody)

    val baos: ByteArrayOutputStream = new ByteArrayOutputStream(getSizeBytes)
    baos.write(
      AggregatedBatch.magicBytes,
      0,
      AggregatedBatch.magicBytes.length
    )
    baos.write(messageBody, 0, messageBody.length)
    baos.write(messageDigest, 0, messageDigest.length)

    val res = baos.toByteArray()

    digest.reset()

    res
  }

  def asRecord: Record.WithShard = Record.WithShard(
    Record(asBytes, aggPartitionKey, aggExplicitHashKey, None),
    shardId
  )

}

object AggregatedBatch {
  def create(
      record: Record.WithShard,
      config: Batcher.Config
  ): AggregatedBatch = {
    val digest = Producer.md5Digest
    val aggregationEntry =
      record.asAggregationEntry(Map.empty, Map.empty, digest)
    AggregatedBatch(
      record.predictedShard,
      NonEmptyList.one(aggregationEntry),
      aggregationEntry.aggregatedPayloadSize(Map.empty, Map.empty),
      Map(aggregationEntry.explicitHashKey -> 0),
      Map(record.record.partitionKey -> 0),
      digest,
      record.record.partitionKey,
      None,
      config
    )
  }

  // From https://github.com/awslabs/amazon-kinesis-producer/blob/master/aggregation-format.md
  val magicBytes: Array[Byte] =
    Array(0xf3, 0x89, 0x9a, 0xc2).map(_.toByte)
}
