package kinesis4cats.models

final case class HashKeyRange(endingHashKey: BigInt, startingHashKey: BigInt) {
  def isBetween(hashKey: BigInt): Boolean =
    hashKey >= startingHashKey && hashKey <= endingHashKey
}
