package kinesis4cats.models

final case class ExtendedSequenceNumber(
    sequenceNumber: String,
    subSequenceNumber: Option[Long]
) {
  val sequenceNumberBigInt: BigInt =
    if (this == ExtendedSequenceNumber.TRIM_HORIZON) BigInt(-2)
    else if (this == ExtendedSequenceNumber.LATEST) BigInt(-1)
    else if (this == ExtendedSequenceNumber.AT_TIMESTAMP) BigInt(-3)
    else if (this == ExtendedSequenceNumber.SHARD_END) BigInt(Int.MaxValue)
    else BigInt(sequenceNumber)
}

object ExtendedSequenceNumber {
  val TRIM_HORIZON = ExtendedSequenceNumber("TRIM_HORIZON", None)
  val LATEST = ExtendedSequenceNumber("LATEST", None)
  val AT_TIMESTAMP = ExtendedSequenceNumber("AT_TIMESTAMP", None)
  val SHARD_END = ExtendedSequenceNumber("SHARD_END", None)
}
