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

package kinesis4cats.producer
package batching

import cats.syntax.all._
import com.amazonaws.kinesis.agg.AggRecord

import kinesis4cats.instances.eq._
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

    val testSize1 = batch.getSizeBytes
    val batch2 =
      batch.add(Record.WithShard(Record(data2, partitionKey2), ShardId("1")))
    val testSize2 = batch2.getSizeBytes

    val agRecord = new AggRecord()
    agRecord.addUserRecord(partitionKey1, null, data1)
    val expectedSize1 = agRecord.getSizeBytes()
    agRecord.addUserRecord(partitionKey2, null, data2)
    val expectedSize2 = agRecord.getSizeBytes()

    assertEquals(testSize1, expectedSize1)
    assertEquals(testSize2, expectedSize2)
  }

  test("It should serialize to bytes correctly") {
    val data1 = Array.fill[Byte](500)(1)
    val partitionKey1 = "foo"
    val data2 = Array.fill[Byte](500)(1)
    val partitionKey2 = "wazzle"

    val batch = AggregatedBatch
      .create(
        Record.WithShard(Record(data1, partitionKey1), ShardId("1")),
        Batcher.Config.default
      )
      .add(Record.WithShard(Record(data2, partitionKey2), ShardId("1")))

    val agRecord = new AggRecord()
    agRecord.addUserRecord(partitionKey1, null, data1)
    agRecord.addUserRecord(partitionKey2, null, data2)

    val testBytes = batch.asBytes
    val expectedBytes = agRecord.toRecordBytes()

    assert(testBytes === expectedBytes)
  }

  test("It should signal that a record cannot be added") {
    def oneThird =
      Array.fill[Byte]((config.maxPayloadSizePerRecord / 3) - 1)(1)
    val shardId = ShardId("1")
    val data1 = oneThird
    val partitionKey1 = "foo"
    val data2 = oneThird
    val partitionKey2 = "wazzle"
    val data3 = oneThird
    val partitionKey3 = "wizzle"

    val batch = AggregatedBatch.create(
      Record.WithShard(Record(data1, partitionKey1), shardId),
      Batcher.Config.default
    )

    val record2 = Record.WithShard(Record(data2, partitionKey2), shardId)

    assert(batch.canAdd(record2))

    val batch2 = batch.add(record2)

    val record3 = Record.WithShard(Record(data3, partitionKey3), shardId)
    assert(!batch2.canAdd(record3))

    val agRecord = new AggRecord()
    assert(agRecord.addUserRecord(partitionKey1, null, data1))
    assert(agRecord.addUserRecord(partitionKey2, null, data2))
    assert(!agRecord.addUserRecord(partitionKey3, null, data3))
  }
}
