package kinesis4cats.producer

import java.nio.charset.StandardCharsets

final case class Record(
    data: Array[Byte],
    partitionKey: String,
    explicitHashKey: Option[String]
) {
  val payloadSize =
    partitionKey.getBytes(StandardCharsets.UTF_8).length + data.length
}
