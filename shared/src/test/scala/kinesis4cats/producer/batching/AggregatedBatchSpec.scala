package kinesis4cats.producer
package batching

import com.amazonaws.kinesis.agg.AggRecord

import kinesis4cats.models.ShardId

class AggregatedBatchSpec extends munit.CatsEffectSuite {
  val config = Batcher.Config.default
  test("It calculate the record sizes to be the same") {
    val data1 = Array.fill[Byte](500)(1)
    val partitionKey1 = "foo"
    val data2 = Array.fill[Byte](500)(1)
    val partitionKey2 = "wazzle"

    val batch = AggregatedBatch.create(
      Record.WithShard(Record(data1, partitionKey1), ShardId("1")),
      Batcher.Config.default
    )

    val testSize1 = batch.aggregatedMessageSize
    val batch2 = batch.add(Record.WithShard(Record(data2, partitionKey2), ShardId("1")))
    val testSize2 = batch2.aggregatedMessageSize

    val agRecord = new AggRecord()
    agRecord.addUserRecord(partitionKey1, null, data1)
    val expectedSize1 = agRecord.getSizeBytes()
    agRecord.addUserRecord(partitionKey2, null, data2)
    val expectedSize2 = agRecord.getSizeBytes()

    assertEquals(testSize1, expectedSize1)
    assertEquals(testSize2, expectedSize2)
  }
}
