package kinesis4cats
package producer
package logging.instances

import cats.Show
import kinesis4cats.logging.instances.show._
import kinesis4cats.models._

object show {
  implicit val shardIdShow: Show[ShardId] = _.shardId

  implicit val recordShow: Show[Record] = x =>
    ShowBuilder("Record")
      .add("data", x.data)
      .add("partitionKey", x.partitionKey)
      .add("explicitHashKey", x.explicitHashKey)
      .build

  implicit val hashKeyRangeShow: Show[HashKeyRange] = x =>
    ShowBuilder("HashKeyRange")
      .add("endingHashKey", x.endingHashKey)
      .add("startingHashKey", x.startingHashKey)
      .build

  implicit val shardMapRecordShow: Show[ShardMapRecord] = x =>
    ShowBuilder("ShardMapRecord")
      .add("shardId", x.shardId)
      .add("hashKeyRange", x.hashKeyRange)
      .build

  implicit val shardMapShow: Show[ShardMap] = x =>
    ShowBuilder("ShardMap")
      .add("lastUpdated", x.lastUpdated)
      .add("shards", x.shards)
      .build

  implicit val shardMapCacheLogEncoders: ShardMapCache.LogEncoders =
    new ShardMapCache.LogEncoders()
}
