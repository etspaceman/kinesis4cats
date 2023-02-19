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
package producer

import scala.concurrent.duration._

import java.util.UUID

import cats.data.NonEmptyList
import cats.effect._
import cats.effect.std.Queue
import cats.syntax.all._
import io.circe.parser._
import io.circe.syntax._
import org.scalacheck.Arbitrary
import retry.RetryPolicies._
import retry._

import kinesis4cats.syntax.scalacheck._

abstract class ProducerSpec[PutReq, PutRes, A] extends munit.CatsEffectSuite {

  def producerResource: Resource[IO, Producer[IO, PutReq, PutRes]]
  def streamName: String
  def aAsBytes(a: A): Array[Byte]

  def fixture(
      shardCount: Int,
      appName: String
  ): SyncIO[FunFixture[ProducerSpec.Resources[IO, PutReq, PutRes, A]]]

  override def munitTimeout: Duration = 5.minutes

  def appName = streamName

  fixture(3, appName).test("It should produce records end to end") {
    resources =>
      for {
        data <- IO(Arbitrary.arbitrary[TestData].take(50).toList)
        records = NonEmptyList.fromListUnsafe(
          data.map(x =>
            Record(
              x.asJson.noSpaces.getBytes(),
              UUID.randomUUID().toString(),
              None,
              None
            )
          )
        )
        _ <- resources.producer.put(records)
        retryPolicy = limitRetries[IO](30).join(constantDelay(1.second))
        size <- retryingOnFailures(
          retryPolicy,
          (x: Int) => IO(x === 50),
          noop[IO, Int]
        )(resources.resultsQueue.size)
        _ <- IO(assert(size === 50))
        results <- resources.resultsQueue.tryTakeN(None)
        resultRecords <- results.traverse { x =>
          IO.fromEither(decode[TestData](new String(aAsBytes(x))))
        }
      } yield assert(
        resultRecords.forall(data.contains),
        s"res: ${resultRecords}\nexp: ${data}"
      )
  }
}

object ProducerSpec {

  final case class Resources[F[_], PutReq, PutRes, A](
      resultsQueue: Queue[F, A],
      producer: Producer[F, PutReq, PutRes]
  )

}
