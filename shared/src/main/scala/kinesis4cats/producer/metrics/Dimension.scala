package kinesis4cats.producer.metrics

import kinesis4cats.models.ShardId
import kinesis4cats.models.StreamArn

sealed abstract class Dimension(val name: String, val value: String)

object Dimension {
  final case class Stream(arn: StreamArn)
      extends Dimension("StreamARN", arn.streamArn)

  final case class Shard(id: ShardId) extends Dimension("ShardID", id.shardId)

  final case class ErrorCode(override val value: String)
      extends Dimension("ErrorCode", value)
}
