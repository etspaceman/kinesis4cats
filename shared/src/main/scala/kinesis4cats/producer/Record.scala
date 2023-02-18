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

import com.google.protobuf.ByteString

import kinesis4cats.models.ShardId
import kinesis4cats.protobuf.Messages
import kinesis4cats.syntax.id._

final case class Record(
    data: Array[Byte],
    partitionKey: String,
    explicitHashKey: Option[String] = None,
    sequenceNumberForOrdering: Option[String] = None
) {
  private val partitionKeyLength =
    partitionKey.getBytes(StandardCharsets.UTF_8).length

  val payloadSize: Int =
    partitionKeyLength + data.length

  def aggregatedPayloadSize(
      currentPartitionKeys: Map[String, Int],
      currentExplicitHashKeys: Option[Map[String, Int]]
  ): Int = {
    val pkSize = if (!currentPartitionKeys.contains(partitionKey)) {
      val pkLength = partitionKey.getBytes().length
      1 + Record.calculateVarIntSize(pkLength) + pkLength
    } else 0

    val explicitHashKeySize = explicitHashKey match {
      case None => 2
      case Some(ehk) if !currentExplicitHashKeys.contains(ehk) =>
        val ehkLength = ehk.getBytes().length
        1 + Record.calculateVarIntSize(ehkLength) + ehkLength
      case _ => 0
    }

    val innerRecordSize = {

      val pkIndexSize = Record.calculateVarIntSize(
        currentPartitionKeys.getOrElse(
          partitionKey,
          currentPartitionKeys.size
        ) + 1
      )

      val ehkIndexSize = explicitHashKey.fold(0)(ehk =>
        Record.calculateVarIntSize(
          currentExplicitHashKeys.fold(0)(ehks =>
            ehks.getOrElse(ehk, ehks.size)
          )
        ) + 1
      )

      val dataSize = Record.calculateVarIntSize(data.length) + data.length + 1

      val combined = pkIndexSize + ehkIndexSize + dataSize

      1 + Record.calculateVarIntSize(combined) + combined
    }

    pkSize + explicitHashKeySize + innerRecordSize
  }

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
  val unit128Max = BigInt(List.fill(16)("FF").mkString, 16)

  // See https://github.com/awslabs/kinesis-aggregation/blob/master/java/KinesisAggregatorV2/src/main/java/com/amazonaws/kinesis/agg/AggRecord.java
  // shift the value right one bit at a time until
  // there are no more '1' bits left...this counts
  // how many bits we need to represent the number
  @annotation.tailrec
  def getBitsNeeded(value: Int, bitsNeeded: Int = 0): Int =
    if (value <= 0) bitsNeeded
    else getBitsNeeded(value >> 1, bitsNeeded + 1)

  def calculateVarIntSize(value: Int): Int = {
    val bitsNeeded = if (value == 0) 1 else getBitsNeeded(value)
    val varintBytes = bitsNeeded / 7

    if (varintBytes % 7 > 0) varintBytes + 1
    else varintBytes
  }

  final case class WithShard(record: Record, predictedShard: ShardId) {
    val payloadSize: Int = record.payloadSize
    def isValidPayloadSize(payloadSizeLimit: Int) =
      record.isValidPayloadSize(payloadSizeLimit)

    def isValidPartitionKey(partitionKeyMin: Int, partitionKeyMax: Int) =
      record.isValidPartitionKey(partitionKeyMin, partitionKeyMax)

    def isValidExplicitHashKey = record.isValidExplicitHashKey

    def isValid(
        payloadSizeLimit: Int,
        partitionKeyMin: Int,
        partitionKeyMax: Int
    ) = record.isValid(payloadSizeLimit, partitionKeyMin, partitionKeyMax)

    def aggregatedPayloadSize(
        currentPartitionKeys: Map[String, Int],
        currentExplicitHashKeys: Option[Map[String, Int]]
    ): Int = record.aggregatedPayloadSize(
      currentPartitionKeys,
      currentExplicitHashKeys
    )
  }

  object WithShard {
    def fromOption(record: Record, predictedShard: Option[ShardId]) =
      WithShard(record, predictedShard.getOrElse(ShardId("DEFAULT")))
  }

  final case class AggregationEntry(
      record: Record,
      partitionKeyTableIndex: Int,
      explicitHashKeyTableIndex: Option[Int]
  ) {
    val payloadSize: Int = record.payloadSize
    def isValidPayloadSize(payloadSizeLimit: Int) =
      record.isValidPayloadSize(payloadSizeLimit)

    def isValidPartitionKey(partitionKeyMin: Int, partitionKeyMax: Int) =
      record.isValidPartitionKey(partitionKeyMin, partitionKeyMax)

    def isValidExplicitHashKey = record.isValidExplicitHashKey

    def isValid(
        payloadSizeLimit: Int,
        partitionKeyMin: Int,
        partitionKeyMax: Int
    ) = record.isValid(payloadSizeLimit, partitionKeyMin, partitionKeyMax)

    def aggregatedPayloadSize(
        currentPartitionKeys: Map[String, Int],
        currentExplicitHashKeys: Option[Map[String, Int]]
    ): Int = record.aggregatedPayloadSize(
      currentPartitionKeys,
      currentExplicitHashKeys
    )

    def getEntry: Messages.Record = Messages.Record
      .newBuilder()
      .setData(ByteString.copyFrom(record.data))
      .setPartitionKeyIndex(partitionKeyTableIndex.toLong)
      .maybeTransform(explicitHashKeyTableIndex) { case (x, y) =>
        x.setExplicitHashKeyIndex(y.toLong)
      }
      .build()
  }
}
