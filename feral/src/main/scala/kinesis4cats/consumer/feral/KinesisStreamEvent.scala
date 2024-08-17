/*
 * Copyright 2023-2023 etspaceman
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kinesis4cats.consumer
package feral

import scala.util.Try

import java.time.Instant

import _root_.feral.lambda.KernelSource
import io.circe.Decoder
import io.circe.scodec._
import scodec.bits.ByteVector

import kinesis4cats.models.EncryptionType
import kinesis4cats.models.instances.circe._

final case class KinesisStreamRecordPayload(
    approximateArrivalTimestamp: Instant,
    data: ByteVector,
    kinesisSchemaVersion: String,
    partitionKey: String,
    encryptionType: Option[EncryptionType],
    sequenceNumber: String
) {
  def asRecord = Record(
    sequenceNumber,
    approximateArrivalTimestamp,
    data,
    partitionKey,
    encryptionType,
    None,
    None
  )
}

object KinesisStreamRecordPayload {
  implicit private val instantCirceDecoder: Decoder[Instant] =
    Decoder.decodeBigDecimal.emapTry { secondsDecimal =>
      def round(x: BigDecimal) = x.setScale(0, BigDecimal.RoundingMode.DOWN)
      Try {
        val roundedSecondsDecimal = round(secondsDecimal)
        val seconds = roundedSecondsDecimal.toLongExact
        val nanos =
          round((secondsDecimal - roundedSecondsDecimal) * 1e9).toLongExact
        Instant.ofEpochSecond(seconds, nanos)
      }
    }

  implicit val kinesisStreamRecordPayloadCirceDecoder
      : Decoder[KinesisStreamRecordPayload] =
    Decoder.forProduct6(
      "approximateArrivalTimestamp",
      "data",
      "kinesisSchemaVersion",
      "partitionKey",
      "encryptionType",
      "sequenceNumber"
    )(KinesisStreamRecordPayload.apply)
}

final case class KinesisStreamRecord(
    awsRegion: String,
    eventID: String,
    eventName: String,
    eventSource: String,
    eventSourceArn: String,
    eventVersion: String,
    invokeIdentityArn: String,
    kinesis: KinesisStreamRecordPayload
)

object KinesisStreamRecord {
  implicit val kinesisStreamRecordCirceDecoder: Decoder[KinesisStreamRecord] =
    Decoder.forProduct8(
      "awsRegion",
      "eventID",
      "eventName",
      "eventSource",
      "eventSourceARN",
      "eventVersion",
      "invokeIdentityArn",
      "kinesis"
    )(KinesisStreamRecord.apply)
}

final case class KinesisStreamEvent(
    records: List[KinesisStreamRecord]
) {
  def deaggregate: Try[List[Record]] =
    Record.deaggregate(records.map(_.kinesis.asRecord))
}

object KinesisStreamEvent {
  implicit val kinesisStreamEventCirceDecoder: Decoder[KinesisStreamEvent] =
    Decoder.forProduct1("Records")(KinesisStreamEvent.apply)

  implicit def kinesisStreamEventKernelSource
      : KernelSource[KinesisStreamEvent] = KernelSource.emptyKernelSource
}
