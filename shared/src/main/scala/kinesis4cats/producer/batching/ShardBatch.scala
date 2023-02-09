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
