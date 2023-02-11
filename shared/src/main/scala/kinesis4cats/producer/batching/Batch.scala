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

import cats.data.NonEmptyMap

import kinesis4cats.models.ShardId

final case class Batch(
    shardBatches: NonEmptyMap[ShardId, ShardBatch],
    count: Int,
    batchSize: Long
) {

  def add(record: Record.WithShard): Batch =
    copy(
      shardBatches = shardBatches.add(
        (record.predictedShard ->
          shardBatches(record.predictedShard)
            .fold(ShardBatch.create(record))(x => x.add(record.record)))
      ),
      count = count + 1,
      batchSize = batchSize + record.payloadSize
    )

  def canAdd(record: Record.WithShard): Boolean =
    count + 1 <= Constants.MaxRecordsPerRequest &&
      batchSize + record.payloadSize <= Constants.MaxPayloadSizePerRequest &&
      shardBatches(record.predictedShard)
        .map(_.canAdd(record.record))
        .getOrElse(true)

  def finalizeBatch: Batch =
    copy(shardBatches = shardBatches.map(_.finalizeBatch))
}

object Batch {
  def create(record: Record.WithShard): Batch = Batch(
    NonEmptyMap.of(record.predictedShard -> ShardBatch.create(record)),
    1,
    record.payloadSize
  )
}
