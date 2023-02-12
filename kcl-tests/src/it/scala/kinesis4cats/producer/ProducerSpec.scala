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

import java.util.UUID

import cats.data.NonEmptyList
import cats.effect._
import cats.effect.std.Queue
import cats.effect.syntax.all._
import cats.syntax.all._
import io.circe.parser._
import io.circe.syntax._
import org.scalacheck.Arbitrary
import retry.RetryPolicies._
import retry._

import kinesis4cats.client.KinesisClient
import kinesis4cats.client.localstack.LocalstackKinesisClient
import kinesis4cats.kcl._
import kinesis4cats.kcl.localstack.LocalstackKCLConsumer
import kinesis4cats.localstack.TestData
import kinesis4cats.localstack.syntax.scalacheck._
import kinesis4cats.syntax.bytebuffer._

abstract class ProducerSpec[PutReq, PutRes](implicit
    val KCLLE: RecordProcessor.LogEncoders,
    val CLE: KinesisClient.LogEncoders
) extends munit.CatsEffectSuite {

  def producerResource: Resource[IO, Producer[IO, PutReq, PutRes]]
  def streamName: String

  def fixture(
      shardCount: Int,
      appName: String
  ): SyncIO[FunFixture[ProducerSpec.Resources[IO, PutReq, PutRes]]] =
    ResourceFixture(
      ProducerSpec.resource(streamName, shardCount, appName, producerResource)
    )

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
        _ <- resources.producer.put(PutRequest(records))
        retryPolicy = limitRetries[IO](30).join(constantDelay(1.second))
        size <- retryingOnFailures(
          retryPolicy,
          (x: Int) => IO(x === 50),
          noop[IO, Int]
        )(resources.resultsQueue.size)
        _ <- IO(assert(size === 50))
        results <- resources.resultsQueue.tryTakeN(None)
        resultRecords <- results.traverse { x =>
          IO.fromEither(decode[TestData](new String(x.data.asArray)))
        }
      } yield assert(
        resultRecords.forall(data.contains),
        s"res: ${resultRecords}\nexp: ${data}"
      )
  }
}

object ProducerSpec {
  def resource[PutReq, PutRes](
      streamName: String,
      shardCount: Int,
      appName: String,
      producerResource: Resource[IO, Producer[IO, PutReq, PutRes]]
  )(implicit
      KCLLE: RecordProcessor.LogEncoders,
      CLE: KinesisClient.LogEncoders
  ): Resource[IO, Resources[IO, PutReq, PutRes]] = for {
    _ <- LocalstackKinesisClient.streamResource[IO](streamName, shardCount)
    deferredWithResults <- LocalstackKCLConsumer.kclConsumerWithResults(
      streamName,
      appName
    )((_: List[CommittableRecord[IO]]) => IO.unit)
    _ <- deferredWithResults.deferred.get.toResource
    producer <- producerResource
  } yield Resources(
    deferredWithResults.resultsQueue,
    producer
  )

  final case class Resources[F[_], PutReq, PutRes](
      resultsQueue: Queue[F, CommittableRecord[F]],
      producer: Producer[F, PutReq, PutRes]
  )

}
