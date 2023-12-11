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
import java.util.Base64

class ProducerSpec extends munit.CatsEffectSuite {
  def fixture: SyncIO[FunFixture[MockProducer]] = ResourceFunFixture(
    MockProducer()
  )

  fixture.test("It should retry methods and eventually produce") { producer =>
    val record1 = Record("record 1".getBytes(), "1")
    val record2 = Record("record 2".getBytes(), "2")


    val data = NonEmptyList.of(
      record1, record2
    )

    val response1 = MockPutResponse(
      NonEmptyList.one(record1),
      List(record2)
    )

    val response2 = MockPutResponse(
      NonEmptyList.one(record2),
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
      _ = req.records.toList.foreach { record =>
        println(s"Record data: ${Base64.getEncoder().encodeToString(record.data)}")
      }
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
      defaultConfig.batcherConfig.copy(aggregate = true)
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
