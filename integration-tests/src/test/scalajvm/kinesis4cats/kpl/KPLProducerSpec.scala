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

package kinesis4cats
package kpl

import scala.concurrent.duration._

import java.nio.ByteBuffer

import cats.effect.{IO, SyncIO}
import com.amazonaws.services.kinesis.producer._
import io.circe.syntax._
import org.scalacheck.Arbitrary

import kinesis4cats.Utils
import kinesis4cats.kpl.localstack.LocalstackKPLProducer
import kinesis4cats.localstack.TestStreamConfig
import kinesis4cats.syntax.scalacheck._

abstract class KPLProducerSpec
    extends munit.CatsEffectSuite
    with munit.CatsEffectFunFixtures {

  override def munitIOTimeout: Duration = 45.seconds

  def fixture(
      streamName: String,
      shardCount: Int
  ): SyncIO[FunFixture[KPLProducer[IO]]] = ResourceFunFixture(
    LocalstackKPLProducer.Builder
      .default[IO]()
      .toResource
      .flatMap(
        _.withStreamsToCreate(
          List(TestStreamConfig.default(streamName, shardCount))
        ).build
      )
  )

  val streamName = s"kpl-producer-spec-${Utils.randomUUIDString}"

  fixture(streamName, 1).test("It should run commands successfully") {
    producer =>
      val testData = Arbitrary.arbitrary[TestData].one
      val testDataBB = ByteBuffer.wrap(testData.asJson.noSpaces.getBytes())

      for {
        _ <- producer
          .put(new UserRecord(streamName, "partitionKey", testDataBB))
          .map { result =>
            assert(result.isSuccessful())
          }
        _ <- producer.put(streamName, "partitionKey", None, testDataBB)
        _ <- producer.getOutstandingRecordsCount()
        _ <- producer.getOldestRecordTimeInMillis()
        _ <- producer.getMetrics()
        _ <- producer.getMetrics(1)
        _ <- producer.getMetrics("foo")
        _ <- producer.getMetrics("foo", 1)
        _ <- producer.flush(streamName)
        _ <- producer.flush()
      } yield ()
  }
}
