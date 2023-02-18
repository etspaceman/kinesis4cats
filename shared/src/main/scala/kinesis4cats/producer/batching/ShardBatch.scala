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

/** Represents records that can be produced in a single request for a single
  * shard
  *
  * @param shardId
  *   [[kinesis4cats.models.ShardId ShardId]] for the shard
  * @param records
  *   [[cats.data.NonEmptyList NonEmptyList]] of
  *   [[kinesis4cats.producer.Record records]] in the batch
  * @param count
  *   Total records in batch
  * @param batchSize
  *   Total payload size in batch
  * @param config
  *   [[kinesis4cats.producer.batching.Batcher.Config Batcher.Config]]
  */
final case class ShardBatch private (
    shardId: ShardId,
    records: NonEmptyList[Record],
    count: Int,
    batchSize: Int,
    config: Batcher.Config
) {

  /** Add a record to the batch
    *
    * @param record
    *   [[kinesis4cats.producer.Record Record]]
    * @return
    *   [[kinesis4cats.producer.batching.ShardBatch ShardBatch]]
    */
  def add(record: Record): ShardBatch =
    copy(
      records = records.prepend(record),
      count = count + 1,
      batchSize = batchSize + record.payloadSize
    )

  /** Determines if a record can be added to the batch
    *
    * @param record
    *   [[kinesis4cats.producer.Record Record]]
    * @return
    *   Boolean
    */
  def canAdd(record: Record): Boolean =
    count + 1 <= config.maxRecordsPerShardPerSecond &&
      batchSize + record.payloadSize <= config.maxPayloadSizePerShardPerSecond

  /** Return a finalized
    * [[kinesis4cats.producer.batching.ShardBatch ShardBatch]]
    *
    * @return
    *   [[kinesis4cats.producer.batching.ShardBatch ShardBatch]]
    */
  def finalizeBatch: ShardBatch = copy(records = records.reverse)
}

object ShardBatch {

  /** Create a fresh batch with a new record
    *
    * @param record
    *   [[kinesis4cats.producer.Record.WithShard Record.WithShard]]
    * @param config
    *   [[kinesis4cats.producer.batching.Batcher.Config Batcher.Config]]
    * @return
    *   [[kinesis4cats.producer.batching.ShardBatch ShardBatch]]
    */
  def create(record: Record.WithShard, config: Batcher.Config) =
    ShardBatch(
      record.predictedShard,
      NonEmptyList.one(record.record),
      1,
      record.payloadSize,
      config
    )
}
