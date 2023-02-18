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

/** Represents records that can be produced in a single request across shards
  *
  * @param shardBatches
  *   [[cats.data.NonEmptyMap NonEmptyMap]] of
  *   [[kinesis4cats.producer.batching.ShardBatch]] by shard-id
  * @param count
  *   Total records in batch
  * @param batchSize
  *   Total payload size in batch
  * @param config
  *   [[kinesis4cats.producer.batching.Batcher.Config Batcher.Config]]
  */
final case class Batch(
    shardBatches: NonEmptyMap[ShardId, ShardBatch],
    count: Int,
    batchSize: Int,
    config: Batcher.Config
) {

  /** Add a record to the batch
    *
    * @param record
    *   [[kinesis4cats.producer.Record.WithShard Record.WithShard]]
    * @return
    *   [[kinesis4cats.producer.batching.Batch Batch]]
    */
  def add(record: Record.WithShard): Batch =
    copy(
      shardBatches = shardBatches.add(
        record.predictedShard ->
          shardBatches(record.predictedShard)
            .fold(ShardBatch.create(record, config))(x => x.add(record.record))
      ),
      count = count + 1,
      batchSize = batchSize + record.payloadSize
    )

  /** Determines if a record can be added to the batch
    *
    * @param record
    *   [[kinesis4cats.producer.Record.WithShard Record.WithShard]]
    * @return
    *   Boolean
    */
  def canAdd(record: Record.WithShard): Boolean =
    count + 1 <= config.maxRecordsPerRequest &&
      batchSize + record.payloadSize <= config.maxPayloadSizePerRequest &&
      shardBatches(record.predictedShard)
        .map(_.canAdd(record.record))
        .getOrElse(true)

  /** Return a finalized [[kinesis4cats.producer.batching.Batch Batch]]
    *
    * @return
    *   [[kinesis4cats.producer.batching.Batch Batch]]
    */
  def finalizeBatch: Batch =
    copy(shardBatches = shardBatches.map(_.finalizeBatch))
}

object Batch {

  /** Create a fresh batch with a new record
    *
    * @param record
    *   [[kinesis4cats.producer.Record.WithShard Record.WithShard]]
    * @param config
    *   [[kinesis4cats.producer.batching.Batcher.Config Batcher.Config]]
    * @return
    *   [[kinesis4cats.producer.batching.Batch Batch]]
    */
  def create(record: Record.WithShard, config: Batcher.Config): Batch = Batch(
    NonEmptyMap.of(record.predictedShard -> ShardBatch.create(record, config)),
    1,
    record.payloadSize,
    config
  )

}
