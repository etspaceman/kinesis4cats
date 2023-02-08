package kinesis4cats.producer

object Constants {
  val MaxChunkSize: Int = 1024 // Stream-internal max chunk size
  val MaxRecordsPerRequest = 500 // This is a Kinesis API limitation
  val MaxPayloadSizePerRequest = 5 * 1024 * 1024 // 5 MB
  val MaxPayloadSizePerRecord =
    1 * 1024 * 921 // 1 MB TODO actually 90%, to avoid underestimating and getting Kinesis errors
  val MaxIngestionPerShardPerSecond = 1 * 1024 * 1024 // 1 MB
  val MaxRecordsPerShardPerSecond = 1000
}
