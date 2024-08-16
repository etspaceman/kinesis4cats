package kinesis4cats.consumer

import java.time.Instant

import scodec.bits.ByteVector

import kinesis4cats.consumer.feral.KinesisStreamRecord
import kinesis4cats.consumer.feral.KinesisStreamRecordPayload
import io.circe.parser.decode

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
