package kinesis4cats.producer
package batching

import cats.data.NonEmptyMap

import kinesis4cats.models.ShardId
import cats.data.NonEmptyList

final case class Batch(
    _shardBatches: NonEmptyMap[ShardId, ShardBatch],
    count: Int,
    batchSize: Long
) {
  private def getOrCreateShardBatch(record: Record.WithShard): ShardBatch =
    _shardBatches(record.predictedShard)
      .getOrElse(ShardBatch.create(record))

  def add(record: Record.WithShard): Batch =
    copy(
      _shardBatches = _shardBatches.add(
        (record.predictedShard ->
          _shardBatches(record.predictedShard)
            .fold(ShardBatch.create(record))(x => x.add(record.record)))
      ),
      count = count + 1,
      batchSize = batchSize + record.payloadSize
    )

  def canAdd(record: Record.WithShard): Boolean =
    count + 1 <= Constants.MaxRecordsPerRequest &&
      batchSize + record.payloadSize <= Constants.MaxPayloadSizePerRecord &&
      getOrCreateShardBatch(record).canAdd(record.record)

  def shardBatches: NonEmptyList[ShardBatch] =
    _shardBatches.toNel.map(_._2)
}

object Batch {
  def create(record: Record.WithShard): Batch = Batch(
    NonEmptyMap.of(record.predictedShard -> ShardBatch.create(record)),
    1,
    record.payloadSize
  )
}
