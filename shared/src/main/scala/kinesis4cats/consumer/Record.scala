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
package consumer

import scala.util.Success
import scala.util.Try

import java.time.Instant

import cats.data.Chain
import cats.syntax.all._
import scodec.bits.ByteVector

import kinesis4cats.models.EncryptionType
import kinesis4cats.protobuf.kinesisMessages

final case class Record(
    sequenceNumber: String,
    approximateArrivalTimestamp: Instant,
    data: ByteVector,
    partitionKey: String,
    encryptionType: Option[EncryptionType],
    subSequenceNumber: Option[Long],
    explicitHashKey: Option[String]
) {
  val isAggregated: Boolean =
    if (data.length >= Aggregation.aggregatedByteSize) {
      data.startsWith(Aggregation.magicByteVector)
    } else false

  private val dataSize = data.length.toInt - Aggregation.aggregatedByteSize

  private val dataArray = {
    val arr = new Array[Byte](dataSize)
    data.copyToArray(arr, 0, Aggregation.magicBytes.length.toLong, dataSize)
    arr
  }
}

object Record {
  def deaggregate(records: List[Record]): Try[List[Record]] =
    records.flatTraverse {
      case record if !record.isAggregated => Success(List(record))
      case record =>
        Try(kinesisMessages.AggregatedRecord.parseFrom(record.dataArray))
          .flatMap { ar =>
            val pks = ar.partitionKeyTable.toList
            val ehks = ar.explicitHashKeyTable.toList

            Chain
              .traverseViaChain(ar.records.toIndexedSeq.zipWithIndex) {
                case (r, i) =>
                  Try {
                    val partitionKey = pks(r.partitionKeyIndex.toInt)
                    val explicitHashKey =
                      r.explicitHashKeyIndex.map(x => ehks(x.toInt))

                    Record(
                      record.sequenceNumber,
                      record.approximateArrivalTimestamp,
                      ByteVector(r.data.toByteArray()),
                      partitionKey,
                      record.encryptionType,
                      Some(i.toLong),
                      explicitHashKey
                    )
                  }
              }
              .map(_.toList)
          }
    }
}
