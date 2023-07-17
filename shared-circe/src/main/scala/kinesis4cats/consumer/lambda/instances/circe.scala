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

package kinesis4cats.consumer.lambda
package instances

import scala.util.Try

import java.time.Instant

import io.circe.Decoder
import io.circe.scodec._

import kinesis4cats.models.instances.circe._

object circe {
  implicit private val instantCirceDecoder: Decoder[Instant] =
    Decoder.decodeBigDecimal.emapTry { millis =>
      def round(x: BigDecimal) = x.setScale(0, BigDecimal.RoundingMode.DOWN)
      Try {
        val seconds = round(millis / 1000).toLongExact
        val nanos = round((millis % 1000) * 1e6).toLongExact
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

  implicit val kinesisStreamEventCirceDecoder: Decoder[KinesisStreamEvent] =
    Decoder.forProduct1("Records")(KinesisStreamEvent.apply)
}
