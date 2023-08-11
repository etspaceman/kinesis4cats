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
package kcl
package multistream

import scala.concurrent.duration._

import cats.effect.Deferred
import cats.effect.std.Queue
import cats.effect.{IO, Resource, SyncIO}
import cats.syntax.all._
import io.circe.parser._
import io.circe.syntax._
import org.scalacheck.Arbitrary
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.kinesis.model.PutRecordRequest
import software.amazon.kinesis.common._

import kinesis4cats.client.KinesisClient
import kinesis4cats.client.localstack.LocalstackKinesisClient
import kinesis4cats.compat.retry.RetryPolicies._
import kinesis4cats.compat.retry._
import kinesis4cats.kcl.localstack.LocalstackKCLConsumer
import kinesis4cats.localstack.TestStreamConfig
import kinesis4cats.models.{AwsRegion, StreamArn}
import kinesis4cats.syntax.bytebuffer._
import kinesis4cats.syntax.scalacheck._

class KCLConsumerMultiSpec extends munit.CatsEffectSuite {
  def fixture(
      streamArn1: StreamArn,
      streamArn2: StreamArn,
      shardCount: Int,
      appName: String
  ): SyncIO[FunFixture[KCLConsumerMultiSpec.Resources[IO]]] =
    ResourceFunFixture(
      KCLConsumerMultiSpec.resource(streamArn1, streamArn2, shardCount, appName)
    )

  override def munitIOTimeout: Duration = 5.minutes

  val accountId = "000000000000"
  val streamArn1 = StreamArn(
    AwsRegion.US_EAST_1,
    s"kcl-multi-consumer-spec-1-${Utils.randomUUIDString}",
    accountId
  )
  val streamArn2 = StreamArn(
    AwsRegion.US_EAST_1,
    s"kcl-multi-consumer-spec-2-${Utils.randomUUIDString}",
    accountId
  )
  val appName = s"kcl-multi-consumer-spec-${Utils.randomUUIDString}"

  fixture(streamArn1, streamArn2, 1, appName).test(
    "It should receive produced records"
  ) { resources =>
    for {
      _ <- resources.deferredStarted.get
      records1 <- IO(Arbitrary.arbitrary[TestData].take(5).toList)
      _ <- records1.traverse(record =>
        resources.client.putRecord(
          PutRecordRequest
            .builder()
            .data(SdkBytes.fromUtf8String(record.asJson.noSpacesSortKeys))
            .streamName(streamArn1.streamName)
            .partitionKey("foo")
            .build()
        )
      )
      records2 <- IO(Arbitrary.arbitrary[TestData].take(5).toList)
      _ <- records2.traverse(record =>
        resources.client.putRecord(
          PutRecordRequest
            .builder()
            .data(SdkBytes.fromUtf8String(record.asJson.noSpacesSortKeys))
            .streamName(streamArn2.streamName)
            .partitionKey("foo")
            .build()
        )
      )
      records = records1 ++ records2
      retryPolicy = limitRetries[IO](30).join(constantDelay(1.second))
      size <- retryingOnFailures(
        retryPolicy,
        (x: Int) => IO(x === 10),
        noop[IO, Int]
      )(resources.resultsQueue.size)
      _ <- IO(assert(size === 10))
      results <- resources.resultsQueue.tryTakeN(None)
      resultRecords <- results.traverse { x =>
        IO.fromEither(decode[TestData](new String(x.data.asArray)))
      }
    } yield assert(records.forall(x => resultRecords.contains(x)))
  }
}

object KCLConsumerMultiSpec {
  def resource(
      streamArn1: StreamArn,
      streamArn2: StreamArn,
      shardCount: Int,
      appName: String
  ): Resource[IO, Resources[IO]] = for {
    client <- LocalstackKinesisClient.Builder
      .default[IO]()
      .toResource
      .flatMap(
        _.withStreamsToCreate(
          List(
            TestStreamConfig.default(streamArn1.streamName, shardCount),
            TestStreamConfig.default(streamArn2.streamName, shardCount)
          )
        ).build
      )
    position = InitialPositionInStreamExtended.newInitialPosition(
      InitialPositionInStream.TRIM_HORIZON
    )
    tracker <- MultiStreamTracker
      .noLeaseDeletionFromArns[IO](
        client,
        Map(streamArn1 -> position, streamArn2 -> position)
      )
      .toResource
    builder <- LocalstackKCLConsumer.Builder.default[IO](tracker, appName)
    deferredWithResults <- builder.runWithResults()
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
