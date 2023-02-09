package kinesis4cats.producer

import java.nio.charset.StandardCharsets
import kinesis4cats.models.ShardId

final case class Record(
    data: Array[Byte],
    partitionKey: String,
    explicitHashKey: Option[String]
) {
  val payloadSize: Long =
    (partitionKey.getBytes(StandardCharsets.UTF_8).length + data.length).toLong
  val isWithinLimits =
    payloadSize <= Constants.MaxPayloadSizePerRecord
}

object Record {
  final case class WithShard(record: Record, predictedShard: ShardId) {
    val payloadSize: Long = record.payloadSize
    val isWithinLimits = record.isWithinLimits
  }

  object WithShard {
    def fromOption(record: Record, predictedShard: Option[ShardId]) =
      WithShard(record, predictedShard.getOrElse(ShardId("DEFAULT")))
  }
}
