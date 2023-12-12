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

import cats.data._
import cats.syntax.all._

import kinesis4cats.models.ShardId

class BatcherSpec extends munit.CatsEffectSuite {
  val config = Batcher.Config.default.copy(aggregate = false)
  val batcher = new Batcher(config)

  test("It should batch records against MaxIngestionPerShard") {
    def oneThird =
      Array.fill[Byte]((config.maxPayloadSizePerShardPerSecond / 3) - 1)(1)
    val shardId = ShardId("1")
    val records = NonEmptyList.fromListUnsafe(
      List.tabulate(7)(index =>
        Record.WithShard(Record(oneThird, s"$index", None, None), shardId)
      )
    )

    val expected: Batcher.Result = Batcher.Result(
      Nil,
      records
        .grouped(3)
        .toList
        .map(x =>
          Batch(
            NonEmptyMap.of(
              shardId -> ShardBatch(
                shardId,
                x.map(_.record),
                x.length,
                x.map(_.record.payloadSize).sumAll,
                config
              )
            ),
            x.length,
            x.map(_.record.payloadSize).sumAll,
            config
          )
        )
    )

    val res = batcher.batch(records, false)
    assert(res.isSuccessful)
    assert(res === expected, s"res: $res\nexp: $expected")
  }

  test("It should batch records against maxPayloadSizePerRequest") {
    def oneSixth =
      Array.fill[Byte]((config.maxPayloadSizePerRequest / 6) - 1)(1)
    val records = NonEmptyList.fromListUnsafe(
      List.tabulate(14) { index =>
        val shardId = ShardId(index.toString)
        shardId -> Record.WithShard(
          Record(oneSixth, s"$index", None, None),
          shardId
        )
      }
    )

    val expected = Batcher.Result(
      Nil,
      records
        .map { case (shardId, record) =>
          shardId -> ShardBatch(
            shardId,
            NonEmptyList.one(record.record),
            1,
            record.record.payloadSize,
            config
          )
        }
        .grouped(6)
        .toList
        .map(x =>
          Batch(
            NonEmptyMap.of(x.head, x.tail: _*),
            x.length,
            x.map(_._2.batchSize).sumAll,
            config
          )
        )
    )

    val res = batcher.batch(records.map(_._2), false)
    assert(res.isSuccessful)
    assert(res === expected, s"res: $res\nexp: $expected")
  }

  test("It should batch records against maxRecordsPerRequest") {
    val shardId = ShardId("1")
    val records = NonEmptyList.fromListUnsafe(
      List.tabulate((config.maxRecordsPerRequest * 2) + 5) { index =>
        Record.WithShard(Record(Array[Byte](1), s"$index", None, None), shardId)
      }
    )

    val expected = Batcher.Result(
      Nil,
      records
        .grouped(config.maxRecordsPerRequest)
        .toList
        .map(x =>
          Batch(
            NonEmptyMap.of(
              shardId -> ShardBatch(
                shardId,
                x.map(_.record),
                x.length,
                x.map(_.record.payloadSize).sumAll,
                config
              )
            ),
            x.length,
            x.map(_.record.payloadSize).sumAll,
            config
          )
        )
    )

    val res = batcher.batch(records, false)
    assert(res.isSuccessful)
    assert(res === expected, s"res: $res\nexp: $expected")
  }

  test(
    "It should batch records against maxRecordsPerRequest with differing shards"
  ) {
    val records = NonEmptyList.fromListUnsafe(
      List.tabulate((config.maxRecordsPerRequest * 2) + 5) { index =>
        val shardId = ShardId(index.toString)
        shardId -> Record.WithShard(
          Record(Array[Byte](1), s"$index", None, None),
          shardId
        )
      }
    )

    val expected = Batcher.Result(
      Nil,
      records
        .map { case (shardId, record) =>
          shardId -> ShardBatch(
            shardId,
            NonEmptyList.one(record.record),
            1,
            record.record.payloadSize,
            config
          )
        }
        .grouped(config.maxRecordsPerRequest)
        .toList
        .map(x =>
          Batch(
            NonEmptyMap.of(x.head, x.tail: _*),
            x.length,
            x.map(_._2.batchSize).sumAll,
            config
          )
        )
    )

    val res = batcher.batch(records.map(_._2), false)
    assert(res.isSuccessful)
    assert(res === expected, s"res: $res\nexp: $expected")
  }

  test("It should reject records that are too large") {
    def tooBig =
      Array.fill[Byte]((config.maxPayloadSizePerRecord) + 1)(1)
    val shardId = ShardId("1")
    val records = NonEmptyList.fromListUnsafe(
      List.tabulate(3) { index =>
        Record.WithShard(Record(tooBig, s"$index", None, None), shardId)
      }
    )
    val expected = Batcher.Result(
      records.map(x => Producer.InvalidRecord.RecordTooLarge(x.record)).toList,
      Nil
    )
    val res = batcher.batch(records, false)
    assert(res.hasInvalid)
    assert(res === expected, s"res: $res\nexp: $expected")
  }

  test("It should reject records that are too large and batch valid records") {
    def tooBig =
      Array.fill[Byte]((config.maxPayloadSizePerRecord) + 1)(1)
    val shardId = ShardId("1")
    val badRecords = NonEmptyList.fromListUnsafe(
      List.tabulate(3) { index =>
        Record.WithShard(Record(tooBig, s"$index", None, None), shardId)
      }
    )
    val goodRecords = NonEmptyList.fromListUnsafe(
      List.tabulate(3) { index =>
        shardId -> Record.WithShard(
          Record(Array[Byte](1), s"$index", None, None),
          shardId
        )
      }
    )
    val expected = Batcher.Result(
      badRecords
        .map(x => Producer.InvalidRecord.RecordTooLarge(x.record))
        .toList, {
        val shardBatch = ShardBatch(
          shardId,
          goodRecords.map(_._2.record),
          goodRecords.length,
          goodRecords.map(_._2.record.payloadSize).sumAll,
          config
        )

        List(
          Batch(
            NonEmptyMap.one(shardId, shardBatch),
            shardBatch.count,
            shardBatch.batchSize,
            config
          )
        )
      }
    )

    val res = batcher.batch(badRecords ::: goodRecords.map(_._2), false)
    assert(res.isPartiallySuccessful)
    assert(res === expected, s"res: $res\nexp: $expected")
  }
}
