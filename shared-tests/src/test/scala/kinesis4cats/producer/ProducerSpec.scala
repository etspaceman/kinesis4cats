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

import scala.concurrent.duration._

import java.time.Instant

import cats.Eq
import cats.data.Ior
import cats.data.NonEmptyList
import cats.effect.IO
import cats.effect.Resource
import cats.effect.SyncIO
import cats.effect.syntax.all._
import cats.syntax.all._
import org.typelevel.log4cats.StructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import kinesis4cats.instances.eq._
import kinesis4cats.models.HashKeyRange
import kinesis4cats.models.ShardId
import kinesis4cats.models.StreamNameOrArn
import kinesis4cats.producer.logging.instances.show._

class ProducerSpec extends munit.CatsEffectSuite {
  def fixture: SyncIO[FunFixture[MockProducer]] = ResourceFunFixture(
    MockProducer()
  )

  fixture.test("It should retry methods and eventually produce") { producer =>
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

    val failed1 = Producer.FailedRecord(
      record2,
      "ProvisionedThroughputExceededException",
      "Throughput was exceeded",
      1
    )
    val failed2 = Producer.FailedRecord(
      record3,
      "ProvisionedThroughputExceededException",
      "Throughput was exceeded",
      2
    )
    val failed3 = Producer.FailedRecord(
      record4,
      "ProvisionedThroughputExceededException",
      "Throughput was exceeded",
      3
    )
    val failed4 = Producer.FailedRecord(
      record5,
      "ProvisionedThroughputExceededException",
      "Throughput was exceeded",
      4
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

    val expected: Ior[Producer.Error, NonEmptyList[MockPutResponse]] = Ior.both(
      Producer.Error(
        Some(
          Ior.Right(
            NonEmptyList.of(
              failed1,
              failed2,
              failed3,
              failed4,
              failed2,
              failed3,
              failed4,
              failed3,
              failed4,
              failed4
            )
          )
        )
      ),
      NonEmptyList.of(response1, response2, response3, response4, response5)
    )

    producer.putWithRetry(data, Some(5), 0.seconds).map { res =>
      assert(res === expected)
    }
  }
}

class MockProducer(
    val logger: StructuredLogger[IO],
    val shardMapCache: ShardMapCache[IO],
    val config: Producer.Config
) extends Producer[IO, MockPutRequest, MockPutResponse] {

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
  def apply(): Resource[IO, MockProducer] = for {
    logger <- Slf4jLogger.create[IO].toResource
    shardMapCache <- ShardMapCache[IO](
      ShardMapCache.Config.default,
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
      Slf4jLogger.create[IO]
    )
    defaultConfig = Producer.Config.default(StreamNameOrArn.Name("foo"))
  } yield new MockProducer(
    logger,
    shardMapCache,
    defaultConfig.copy(batcherConfig =
      defaultConfig.batcherConfig.copy(aggregate = false)
    )
  )
}

case class MockPutRequest(records: NonEmptyList[Record])

case class MockPutResponse(
    successRecords: NonEmptyList[Record],
    failedRecords: List[Record]
)

object MockPutResponse {
  implicit val mockPutResponseEq: Eq[MockPutResponse] = (x, y) =>
    x.successRecords === y.successRecords &&
      x.failedRecords === y.failedRecords
}
