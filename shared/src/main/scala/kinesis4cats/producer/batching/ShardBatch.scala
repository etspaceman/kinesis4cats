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

import cats.data.NonEmptyList

import kinesis4cats.models.ShardId

// The records in a specific shard
final case class ShardBatch private (
    shardId: ShardId,
    _records: NonEmptyList[Record],
    count: Int,
    batchSize: Long
) {
  def add(record: Record): ShardBatch =
    copy(
      _records = _records.prepend(record),
      count = count + 1,
      batchSize = batchSize + record.payloadSize
    )

  def canAdd(record: Record): Boolean =
    count + 1 <= Constants.MaxRecordsPerShardPerSecond &&
      batchSize + record.payloadSize <= Constants.MaxIngestionPerShardPerSecond

  def records = _records.reverse
}

object ShardBatch {
  def create(record: Record.WithShard) =
    ShardBatch(
      record.predictedShard,
      NonEmptyList.one(record.record),
      1,
      record.payloadSize
    )
}
