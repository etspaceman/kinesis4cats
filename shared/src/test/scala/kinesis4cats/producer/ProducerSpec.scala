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

import java.time.Instant

import cats.Eq
import cats.data.NonEmptyList
import cats.effect.IO
import cats.effect.Resource
import cats.effect.SyncIO
import cats.syntax.all._
import org.typelevel.log4cats.StructuredLogger
import org.typelevel.log4cats.noop.NoOpLogger

import kinesis4cats.compat.retry._
import kinesis4cats.models.HashKeyRange
import kinesis4cats.models.ShardId
import kinesis4cats.models.StreamNameOrArn

class ProducerSpec extends munit.CatsEffectSuite {
  def fixture(aggregate: Boolean): SyncIO[FunFixture[MockProducer]] =
    ResourceFunFixture(
      MockProducer(aggregate)
    )

  fixture(false).test("It should retry methods and eventually produce") {
    producer =>
      val record1 = Record(Array.fill(50)(1), "1")
      val record2 = Record(Array.fill(50)(1), "2")
      val record3 = Record(Array.fill(50)(1), "3")
      val record4 = Record(Array.fill(50)(1), "4")
      val record5 = Record(Array.fill(50)(1), "5")

      val data = NonEmptyList.of(
        record1,
        record2,
        record3,
        record4,
        record5
      )

      val response1 = MockPutResponse(
        NonEmptyList.one(record1),
        List(record2, record3, record4, record5)
      )

      val response2 = MockPutResponse(
        NonEmptyList.one(record2),
        List(record3, record4, record5)
      )

      val response3 = MockPutResponse(
        NonEmptyList.one(record3),
        List(record4, record5)
      )

      val response4 = MockPutResponse(
        NonEmptyList.one(record4),
        List(record5)
      )

      val response5 = MockPutResponse(
        NonEmptyList.one(record5),
        List.empty
      )

      val expected: Producer.Result[MockPutResponse] = Producer.Result(
        List(response1, response2, response3, response4, response5),
        Nil,
        Nil
      )

      producer.put(data).map { res =>
        assert(res === expected, s"res: $res\nexp: $expected")
      }
  }

  fixture(true).test(
    "It should retry methods and eventually produce when aggregated"
  ) { producer =>
    val record1 = Record("record 1".getBytes(), "1")
    val record2 = Record("record 2".getBytes(), "2")

    val data = NonEmptyList.of(
      record1,
      record2
    )

    val aggregatedRecord1 = producer.batcher
      ._aggregateAndBatch(
        NonEmptyList.one(Record.WithShard(record1, ShardId("1")))
      )
      .getOrElse(fail("Could not get aggregated batch"))
      .head
      .shardBatches
      .head
      ._2
      .records
    val aggregatedRecord2 = producer.batcher
      ._aggregateAndBatch(
        NonEmptyList.one(Record.WithShard(record2, ShardId("1")))
      )
      .getOrElse(fail("Could not get aggregated batch"))
      .head
      .shardBatches
      .head
      ._2
      .records

    val response1 = MockPutResponse(
      aggregatedRecord1,
      aggregatedRecord2.toList
    )

    val response2 = MockPutResponse(
      aggregatedRecord2,
      List.empty
    )

    val expected: Producer.Result[MockPutResponse] = Producer.Result(
      List(response1, response2),
      Nil,
      Nil
    )

    producer.put(data).map { res =>
      assert(res === expected, s"res: $res\nexp: $expected")
    }
  }

  fixture(false).test("It should not retry when all Results are invalid") {
    producer =>
      val tooSmallPartitionKey = ""
      val record = Record(Array.fill(50)(1), tooSmallPartitionKey)

      val data = NonEmptyList.of(record, record, record)

      val expected: Producer.Result[MockPutResponse] = Producer.Result(
        successful = Nil,
        invalid = List(
          Producer.InvalidRecord.InvalidPartitionKey(tooSmallPartitionKey),
          Producer.InvalidRecord.InvalidPartitionKey(tooSmallPartitionKey),
          Producer.InvalidRecord.InvalidPartitionKey(tooSmallPartitionKey)
        ),
        failed = Nil
      )

      producer.put(data).map { res =>
        assert(res === expected, s"res: $res\nexp: $expected")
      }
  }
}

class MockProducer(
    val logger: StructuredLogger[IO],
    val shardMapCache: ShardMapCache[IO],
    val config: Producer.Config[IO],
    encoders: Producer.LogEncoders
) extends Producer[IO, MockPutRequest, MockPutResponse](encoders) {

  var requests: Int = 0 // scalafix:ok

  override protected def putImpl(req: MockPutRequest): IO[MockPutResponse] =
    for {
      _ <- IO(this.requests = this.requests + 1)
    } yield MockPutResponse(
      NonEmptyList.one(req.records.head),
      req.records.tail
    )

  override protected def asPutRequest(
      records: NonEmptyList[Record]
  ): MockPutRequest = MockPutRequest(records)

  override protected def failedRecords(
      records: NonEmptyList[Record],
      resp: MockPutResponse
  ): Option[NonEmptyList[Producer.FailedRecord]] = resp.failedRecords match {
    case Nil => None
    case x =>
      Some(
        NonEmptyList
          .fromListUnsafe(x)
          .zipWithIndex
          .map { case (r, i) =>
            Producer.FailedRecord(
              r,
              "ProvisionedThroughputExceededException",
              "Throughput was exceeded",
              i
            )
          }
      )
  }

}

object MockProducer {
  def apply(aggregate: Boolean): Resource[IO, MockProducer] = for {
    logger <- Resource.pure(NoOpLogger[IO])
    shardMapCache <- ShardMapCache.Builder
      .default[IO](
        IO.pure(
          Right(
            ShardMap(
              List(
                ShardMapRecord(
                  ShardId("1"),
                  HashKeyRange(BigInt("1"), BigInt("100"))
                )
              ),
              Instant.now()
            )
          )
        ),
        logger
      )
      .build
    defaultConfig = Producer.Config
      .default[IO](StreamNameOrArn.Name("foo"))
      .copy(
        retryPolicy = RetryPolicies.limitRetries[IO](5),
        raiseOnFailures = true
      )
  } yield new MockProducer(
    logger,
    shardMapCache,
    defaultConfig.copy(batcherConfig =
      defaultConfig.batcherConfig.copy(aggregate = aggregate)
    ),
    Producer.LogEncoders.show
  )
}

case class MockPutRequest(records: NonEmptyList[Record])

case class MockPutResponse(
    successRecords: NonEmptyList[Record],
    failedRecords: List[Record]
)

object MockPutResponse {
  implicit val mockPutResponseEq: Eq[MockPutResponse] =
    Eq.by(x => (x.successRecords, x.failedRecords))
}
