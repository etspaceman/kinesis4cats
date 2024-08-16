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

import java.time.Instant

import io.circe.parser.decode
import scodec.bits.ByteVector

import kinesis4cats.consumer.feral.KinesisStreamRecord
import kinesis4cats.consumer.feral.KinesisStreamRecordPayload

class KinesisStreamRecordSpec extends munit.ScalaCheckSuite {
  test("It should parse the event correctly from JSON".only) {
    val recordJson = """
    {
        "kinesis": {
            "kinesisSchemaVersion": "1.0",
            "partitionKey": "1",
            "sequenceNumber": "49590338271490256608559692538361571095921575989136588898",
            "data": "SGVsbG8sIHRoaXMgaXMgYSB0ZXN0Lg==",
            "approximateArrivalTimestamp": 1723739794.987
        },
        "eventSource": "aws:kinesis",
        "eventVersion": "1.0",
        "eventID": "shardId-000000000006:49590338271490256608559692538361571095921575989136588898",
        "eventName": "aws:kinesis:record",
        "invokeIdentityArn": "arn:aws:iam::123456789012:role/lambda-role",
        "awsRegion": "us-east-2",
        "eventSourceARN": "arn:aws:kinesis:us-east-2:123456789012:stream/lambda-stream"
    }
    """

    val record = KinesisStreamRecord(
      awsRegion = "us-east-2",
      eventID =
        "shardId-000000000006:49590338271490256608559692538361571095921575989136588898",
      eventName = "aws:kinesis:record",
      eventSource = "aws:kinesis",
      eventSourceArn =
        "arn:aws:kinesis:us-east-2:123456789012:stream/lambda-stream",
      eventVersion = "1.0",
      invokeIdentityArn = "arn:aws:iam::123456789012:role/lambda-role",
      kinesis = KinesisStreamRecordPayload(
        approximateArrivalTimestamp =
          Instant.ofEpochSecond(1723739794L, 987000L),
        data = ByteVector.fromValidBase64("SGVsbG8sIHRoaXMgaXMgYSB0ZXN0Lg=="),
        kinesisSchemaVersion = "1.0",
        partitionKey = "1",
        encryptionType = None,
        sequenceNumber =
          "49590338271490256608559692538361571095921575989136588898"
      )
    )

    val parsedRecord = decode[KinesisStreamRecord](recordJson)
    assertEquals(parsedRecord, Right(record))
  }
}
