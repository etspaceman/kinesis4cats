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

import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

import com.google.protobuf.ByteString

import kinesis4cats.models.ShardId
import kinesis4cats.protobuf.Messages

final case class Record(
    data: Array[Byte],
    partitionKey: String,
    explicitHashKey: Option[String] = None,
    sequenceNumberForOrdering: Option[String] = None
) {
  private val partitionKeyBytes = partitionKey.getBytes(StandardCharsets.UTF_8)
  private val partitionKeyLength = partitionKeyBytes.length

  val payloadSize: Int =
    partitionKeyLength + data.length

  def isValidPayloadSize(payloadSizeLimit: Int) =
    payloadSize <= payloadSizeLimit

  def isValidPartitionKey(partitionKeyMin: Int, partitionKeyMax: Int) =
    partitionKeyLength <= partitionKeyMax && partitionKeyMin <= partitionKeyLength

  def isValidExplicitHashKey = explicitHashKey.forall { case ehs =>
    val b = BigInt(ehs)
    b.compareTo(Record.unit128Max) <= 0 &&
    b.compareTo(BigInt(BigInteger.ZERO)) >= 0
  }

  def isValid(
      payloadSizeLimit: Int,
      partitionKeyMin: Int,
      partitionKeyMax: Int
  ) = isValidPayloadSize(payloadSizeLimit) && isValidPartitionKey(
    partitionKeyMin,
    partitionKeyMax
  ) && isValidExplicitHashKey
}

object Record {
  private val unit128Max = BigInt(List.fill(16)("FF").mkString, 16)

  // See https://github.com/awslabs/kinesis-aggregation/blob/master/java/KinesisAggregatorV2/src/main/java/com/amazonaws/kinesis/agg/AggRecord.java
  // shift the value right one bit at a time until
  // there are no more '1' bits left...this counts
  // how many bits we need to represent the number
  @annotation.tailrec
  private def getBitsNeeded(value: Int, bitsNeeded: Int = 0): Int =
    if (value <= 0) bitsNeeded
    else getBitsNeeded(value >> 1, bitsNeeded + 1)

  private def calculateVarIntSize(value: Int): Int = {
    val bitsNeeded = if (value == 0) 1 else getBitsNeeded(value)
    val varintBytes = bitsNeeded / 7

    if (bitsNeeded % 7 > 0) varintBytes + 1
    else varintBytes
  }

  final case class WithShard(record: Record, predictedShard: ShardId) {
    def getExplicitHashKey(
        digest: MessageDigest
    ): String = record.explicitHashKey.getOrElse {
      var hashKey = BigInt(BigInteger.ZERO)
      val pkDigest = digest.digest(record.partitionKeyBytes)

      for (i <- 0 until digest.getDigestLength()) {
        val p = BigInt(String.valueOf(pkDigest(i).toInt & 0xff))
        val shifted = p << ((16 - i - 1) * 8)
        hashKey = hashKey + shifted
      }

      digest.reset()
      hashKey.toString(10)
    }

    def asAggregationEntry(
        currentPartitionKeys: Map[String, Int],
        currentExplicitHashKeys: Map[String, Int],
        digest: MessageDigest
    ): AggregationEntry = {
      val ehk = getExplicitHashKey(digest)

      val partitionKeyIndex = currentPartitionKeys.getOrElse(
        record.partitionKey,
        currentPartitionKeys.size
      )
      val explicitHashKeyIndex = currentExplicitHashKeys.getOrElse(
        ehk,
        currentExplicitHashKeys.size
      )
      AggregationEntry(record, ehk, partitionKeyIndex, explicitHashKeyIndex)
    }

    def aggregatedPayloadSize(
        currentPartitionKeys: Map[String, Int],
        currentExplicitHashKeys: Map[String, Int],
        digest: MessageDigest
    ): Int =
      asAggregationEntry(currentPartitionKeys, currentExplicitHashKeys, digest)
        .aggregatedPayloadSize(currentPartitionKeys, currentExplicitHashKeys)
  }

  object WithShard {
    def fromOption(record: Record, predictedShard: Option[ShardId]) =
      WithShard(record, predictedShard.getOrElse(ShardId("DEFAULT")))
  }

  final case class AggregationEntry(
      record: Record,
      explicitHashKey: String,
      partitionKeyTableIndex: Int,
      explicitHashKeyTableIndex: Int
  ) {
    def aggregatedPayloadSize(
        currentPartitionKeys: Map[String, Int],
        currentExplicitHashKeys: Map[String, Int]
    ): Int = {
      val pkSize = if (!currentPartitionKeys.contains(record.partitionKey)) {
        val pkLength = record.partitionKeyLength
        1 + Record.calculateVarIntSize(pkLength) + pkLength
      } else 0

      val explicitHashKeySize =
        if (!currentExplicitHashKeys.contains(explicitHashKey)) {
          val ehkLength =
            explicitHashKey.getBytes(StandardCharsets.UTF_8).length
          1 + Record.calculateVarIntSize(ehkLength) + ehkLength
        } else 0

      val innerRecordSize = {

        val pkIndexSize = Record.calculateVarIntSize(
          currentPartitionKeys.getOrElse(
            record.partitionKey,
            currentPartitionKeys.size
          )
        ) + 1

        val ehkIndexSize = Record.calculateVarIntSize(
          currentExplicitHashKeys.getOrElse(
            explicitHashKey,
            currentExplicitHashKeys.size
          )
        ) + 1

        val dataSize =
          Record.calculateVarIntSize(record.data.length) +
            record.data.length + 1

        val combined = pkIndexSize + ehkIndexSize + dataSize

        1 + Record.calculateVarIntSize(combined) + combined
      }
      pkSize + explicitHashKeySize + innerRecordSize
    }

    def asEntry: Messages.Record = Messages.Record
      .newBuilder()
      .setData(ByteString.copyFrom(record.data))
      .setPartitionKeyIndex(partitionKeyTableIndex.toLong)
      .setExplicitHashKeyIndex(explicitHashKeyTableIndex.toLong)
      .build()
  }
}
