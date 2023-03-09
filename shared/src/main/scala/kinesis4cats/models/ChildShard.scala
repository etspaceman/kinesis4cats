package kinesis4cats.models

final case class ChildShard(
    hashKeyRange: HashKeyRange,
    parentShards: List[String],
    shardId: String
)
