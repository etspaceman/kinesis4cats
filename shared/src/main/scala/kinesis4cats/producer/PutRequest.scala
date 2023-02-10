package kinesis4cats.producer

import kinesis4cats.models.StreamArn
import cats.data.NonEmptyList

sealed trait PutRequest extends Product with Serializable {
  def record: Record
}
object PutRequest {
  final case class Name(streamName: String, record: Record) extends PutRequest
  final case class Arn(streamArn: StreamArn, record: Record) extends PutRequest
}

sealed trait PutNRequest extends Product with Serializable { self =>
  def records: NonEmptyList[Record]
  def withRecords(records: NonEmptyList[Record]): PutNRequest =
    self match {
      case x: PutNRequest.Name => x.copy(records = records)
      case x: PutNRequest.Arn => x.copy(records = records)
    }
}
object PutNRequest {
  final case class Name(streamName: String, records: NonEmptyList[Record])
      extends PutNRequest
  final case class Arn(streamArn: StreamArn, records: NonEmptyList[Record])
      extends PutNRequest
}
