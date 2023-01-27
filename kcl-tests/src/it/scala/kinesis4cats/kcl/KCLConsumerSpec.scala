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

package kinesis4cats.kcl

import scala.concurrent.duration._

import java.util.UUID

import cats.effect.kernel.Deferred
import cats.effect.std.Queue
import cats.effect.{IO, Resource, SyncIO}
import cats.syntax.all._
import io.circe.parser._
import io.circe.syntax._
import org.scalacheck.Arbitrary
import retry.RetryPolicies._
import retry._
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.kinesis.model.PutRecordRequest

import kinesis4cats.client.KinesisClient
import kinesis4cats.client.localstack.LocalstackKinesisClient
import kinesis4cats.kcl.localstack.LocalstackKCLConsumer
import kinesis4cats.localstack.TestData
import kinesis4cats.localstack.syntax.scalacheck._
import kinesis4cats.syntax.bytebuffer._

abstract class KCLConsumerSpec(implicit
    KCLLE: RecordProcessor.LogEncoders,
    CLE: KinesisClient.LogEncoders
) extends munit.CatsEffectSuite {
  def fixture(
      streamName: String,
      shardCount: Int,
      appName: String
  ): SyncIO[FunFixture[KCLConsumerSpec.Resources[IO]]] = ResourceFixture(
    KCLConsumerSpec.resource(streamName, shardCount, appName)
  )

  override def munitTimeout: Duration = 5.minutes

  val streamName = s"kcl-consumer-spec-${UUID.randomUUID().toString()}"
  val appName = streamName

  fixture(streamName, 1, appName).test("It should receive produced records") {
    resources =>
      for {
        _ <- resources.deferredStarted.get
        records <- IO(Arbitrary.arbitrary[TestData].take(5).toList)
        _ <- records.traverse(record =>
          resources.client.putRecord(
            PutRecordRequest
              .builder()
              .data(SdkBytes.fromUtf8String(record.asJson.noSpacesSortKeys))
              .streamName(streamName)
              .partitionKey("foo")
              .build()
          )
        )
        retryPolicy = limitRetries[IO](30).join(constantDelay(1.second))
        size <- retryingOnFailures(
          retryPolicy,
          (x: Int) => IO(x === 5),
          noop[IO, Int]
        )(resources.resultsQueue.size)
        _ <- IO(assert(size === 5))
        results <- resources.resultsQueue.tryTakeN(None)
        resultRecords <- results.traverse { x =>
          IO.fromEither(decode[TestData](new String(x.data.asArray)))
        }
      } yield assertEquals(resultRecords, records)
  }
}

object KCLConsumerSpec {
  def resource(streamName: String, shardCount: Int, appName: String)(implicit
      KCLLE: RecordProcessor.LogEncoders,
      CLE: KinesisClient.LogEncoders
  ): Resource[IO, Resources[IO]] = for {
    client <- LocalstackKinesisClient.streamResource[IO](streamName, shardCount)
    deferredWithResults <- LocalstackKCLConsumer.kclConsumerWithResults(
      streamName,
      appName
    )((_: List[CommittableRecord[IO]]) => IO.unit)
  } yield Resources(
    client,
    deferredWithResults.deferred,
    deferredWithResults.resultsQueue
  )

  final case class Resources[F[_]](
      client: KinesisClient[F],
      deferredStarted: Deferred[F, Unit],
      resultsQueue: Queue[F, CommittableRecord[F]]
  )
}
