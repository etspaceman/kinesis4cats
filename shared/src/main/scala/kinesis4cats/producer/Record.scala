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

import java.nio.charset.StandardCharsets

import kinesis4cats.models.ShardId

final case class Record(
    data: Array[Byte],
    partitionKey: String,
    explicitHashKey: Option[String] = None,
    sequenceNumberForOrdering: Option[String] = None
) {
  val payloadSize: Long =
    (partitionKey.getBytes(StandardCharsets.UTF_8).length + data.length).toLong
  def isWithinLimits(payloadSizeLimit: Int) =
    payloadSize <= payloadSizeLimit
}

object Record {
  final case class WithShard(record: Record, predictedShard: ShardId) {
    val payloadSize: Long = record.payloadSize
    def isWithinLimits(payloadSizeLimit: Int) =
      record.isWithinLimits(payloadSizeLimit)
  }

  object WithShard {
    def fromOption(record: Record, predictedShard: Option[ShardId]) =
      WithShard(record, predictedShard.getOrElse(ShardId("DEFAULT")))
  }
}
