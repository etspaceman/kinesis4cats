package kinesis4cats.producer

final case class Batch(entries: List[Record], count: Int, payloadSize: Long) {
  def add(entry: Record): Batch =
    copy(
      entries = entry +: entries,
      count = count + 1,
      payloadSize = payloadSize + entry.payloadSize
    )

  def isWithinLimits =
    count <= Constants.MaxRecordsPerRequest &&
      payloadSize <= Constants.MaxPayloadSizePerRequest

  def getEntries = entries.reverse
}

object Batch {
  val empty = Batch(List.empty, 0, 0)
}
