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
package fs2

import scala.concurrent.duration._

import java.util.UUID

import _root_.fs2.Stream
import cats.effect.kernel.Deferred
import cats.effect.{IO, Resource, SyncIO}
import cats.syntax.all._
import io.circe.parser._
import io.circe.syntax._
import org.scalacheck.Arbitrary
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.kinesis.model.PutRecordRequest

import kinesis4cats.client.KinesisClient
import kinesis4cats.client.localstack.LocalstackKinesisClient
import kinesis4cats.kcl.fs2.localstack.LocalstackKCLConsumerFS2
import kinesis4cats.localstack.TestData
import kinesis4cats.localstack.syntax.scalacheck._
import kinesis4cats.syntax.bytebuffer._

abstract class KCLConsumerFS2Spec(implicit
    KCLLE: RecordProcessor.LogEncoders,
    CLE: KinesisClient.LogEncoders
) extends munit.CatsEffectSuite {
  def fixture(
      streamName: String,
      shardCount: Int,
      appName: String
  ): SyncIO[FunFixture[KCLConsumerFS2Spec.Resources[IO]]] = ResourceFixture(
    KCLConsumerFS2Spec.resource(streamName, shardCount, appName)
  )

  override def munitTimeout: Duration = 5.minutes

  val streamName = s"kcl-fs2-consumer-spec-${UUID.randomUUID().toString()}"
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
        results <- resources.stream
          .through(resources.consumer.commitRecords)
          .take(5)
          .timeout(30.seconds)
          .compile
          .toList
        resultRecords <- results.traverse { x =>
          IO.fromEither(decode[TestData](new String(x.data.asArray)))
        }
      } yield assertEquals(resultRecords, records)
  }
}

object KCLConsumerFS2Spec {
  def resource(streamName: String, shardCount: Int, appName: String)(implicit
      KCLLE: RecordProcessor.LogEncoders,
      CLE: KinesisClient.LogEncoders
  ): Resource[IO, Resources[IO]] = for {
    client <- LocalstackKinesisClient.streamResource[IO](streamName, shardCount)
    consumer <- LocalstackKCLConsumerFS2.kclConsumer[IO](
      streamName,
      appName
    )
    streamAndDeferred <- consumer.streamWithDeferredListener()
  } yield Resources(
    client,
    consumer,
    streamAndDeferred.stream,
    streamAndDeferred.deferred
  )

  final case class Resources[F[_]](
      client: KinesisClient[F],
      consumer: KCLConsumerFS2[F],
      stream: Stream[F, CommittableRecord[F]],
      deferredStarted: Deferred[F, Unit]
  )
}
