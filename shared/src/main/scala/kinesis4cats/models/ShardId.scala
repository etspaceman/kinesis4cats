package kinesis4cats.models

import cats.Order

final case class ShardId(shardId: String)

object ShardId {
  implicit val shardIdOrder: Order[ShardId] = Order.by(_.shardId)
}
