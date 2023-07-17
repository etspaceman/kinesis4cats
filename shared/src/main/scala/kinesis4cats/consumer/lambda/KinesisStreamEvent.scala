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
package lambda

import scala.util.Try

import java.time.Instant

import scodec.bits.ByteVector

import kinesis4cats.models.EncryptionType

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

final case class KinesisStreamEvent(
    records: List[KinesisStreamRecord]
) {
  def deaggregate: Try[List[Record]] =
    Record.deaggregate(records.map(_.kinesis.asRecord))
}
