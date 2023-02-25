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
package smithy4s.client

import _root_.smithy4s.ByteArray
import _root_.smithy4s.aws.AwsRegion
import cats.effect._
import cats.syntax.all._
import com.amazonaws.kinesis._
import fs2.io.net.tls.TLSContext
import io.circe.jawn._
import io.circe.syntax._
import org.http4s.ember.client.EmberClientBuilder
import org.scalacheck.Arbitrary

import kinesis4cats.Utils
import kinesis4cats.localstack._
import kinesis4cats.logging.ConsoleLogger
import kinesis4cats.logging.instances.show._
import kinesis4cats.models
import kinesis4cats.models.StreamArn
import kinesis4cats.smithy4s.client.localstack.LocalstackKinesisClient
import kinesis4cats.syntax.scalacheck._

abstract class KinesisClientSpec(implicit LE: KinesisClient.LogEncoders[IO])
    extends munit.CatsEffectSuite {

  // allow flaky tests on ci
  override def munitFlakyOK: Boolean = sys.env.contains("CI")

  val region = AwsRegion.US_EAST_1
  def fixture: SyncIO[FunFixture[KinesisClient[IO]]] =
    ResourceFunFixture(
      for {
        tlsContext <- TLSContext.Builder.forAsync[IO].insecureResource
        underlying <- EmberClientBuilder
          .default[IO]
          .withTLSContext(tlsContext)
          .withoutCheckEndpointAuthentication
          .build
        client <- LocalstackKinesisClient.clientResource[IO](
          underlying,
          IO.pure(region),
          LocalstackConfig(
            4566,
            Protocol.Https,
            "localhost",
            4567,
            Protocol.Https,
            "localhost",
            4566,
            Protocol.Https,
            "localhost",
            4566,
            Protocol.Https,
            "localhost",
            models.AwsRegion.US_EAST_1
          ),
          loggerF = (f: Async[IO]) => f.pure(new ConsoleLogger[IO])
        )
      } yield client
    )

  // This is flaky as we seem to be getting non-deterministic failures via SSL connections to Localstack.
  // Will look into this more later.
  fixture.test("It should work through all commands") { client =>
    val streamName =
      s"smithy4s-kinesis-client-spec-${Utils.randomUUIDString}"
    val accountId = "000000000000"

    val streamArn = StreamArn(
      kinesis4cats.models.AwsRegion.values
        .find(_.name === region.value)
        .getOrElse(fail("Could not find region")),
      streamName,
      accountId
    ).streamArn

    for {
      _ <- client.createStream(
        StreamName(streamName),
        Some(PositiveIntegerObject(1)),
        Some(StreamModeDetails(StreamMode.PROVISIONED))
      )
      _ <- client
        .addTagsToStream(
          Map(TagKey("foo") -> TagValue("bar")),
          Some(StreamName(streamName))
        )
      _ <- client
        .increaseStreamRetentionPeriod(
          RetentionPeriodHours(48),
          Some(StreamName(streamName))
        )
      _ <- client
        .decreaseStreamRetentionPeriod(
          RetentionPeriodHours(24),
          Some(StreamName(streamName))
        )
      _ <- client
        .registerStreamConsumer(StreamARN(streamArn), ConsumerName("foo"))
      _ <- client.describeLimits()
      _ <- client.describeStream(Some(StreamName(streamName)))
      _ <- client
        .describeStreamConsumer(
          Some(StreamARN(streamArn)),
          Some(ConsumerName("foo"))
        )
      _ <- client.describeStreamSummary(Some(StreamName(streamName)))
      _ <- client
        .enableEnhancedMonitoring(
          List(MetricsName.ALL),
          Some(StreamName(streamName))
        )
      _ <- client
        .disableEnhancedMonitoring(
          List(MetricsName.ALL),
          Some(StreamName(streamName))
        )
      record1 <- IO(Arbitrary.arbitrary[TestData].one)
      _ <- client
        .putRecord(
          Data(ByteArray(record1.asJson.noSpaces.getBytes())),
          PartitionKey("foo"),
          Some(StreamName(streamName))
        )
      record2 <- IO(Arbitrary.arbitrary[TestData].one)
      record3 <- IO(Arbitrary.arbitrary[TestData].one)
      _ <- client
        .putRecords(
          List(
            PutRecordsRequestEntry(
              Data(ByteArray(record2.asJson.noSpaces.getBytes())),
              PartitionKey("foo")
            ),
            PutRecordsRequestEntry(
              Data(ByteArray(record3.asJson.noSpaces.getBytes())),
              PartitionKey("foo")
            )
          ),
          Some(StreamName(streamName))
        )
      shards <- client.listShards(Some(StreamName(streamName)))
      shard = shards.shards.map(_.head).getOrElse(fail("No shards returned"))
      shardIterator <- client
        .getShardIterator(
          shard.shardId,
          ShardIteratorType.TRIM_HORIZON,
          Some(StreamName(streamName))
        )
      records <- client
        .getRecords(
          shardIterator.shardIterator
            .getOrElse(fail("No shard iterator returned")),
          streamARN = Some(StreamARN(streamArn))
        )
      recordBytes = records.records
        .map(x => new String(x.data.value.array))
      recordsParsed <- recordBytes.traverse(bytes =>
        IO.fromEither(decode[TestData](bytes))
      )
      consumers <- client.listStreamConsumers(StreamARN(streamArn))
      _ <- client
        .deregisterStreamConsumer(
          Some(StreamARN(streamArn)),
          Some(ConsumerName("foo"))
        )
      tags <- client.listTagsForStream(Some(StreamName(streamName)))
      _ <- client
        .removeTagsFromStream(List(TagKey("foo")), Some(StreamName(streamName)))
      _ <- client
        .startStreamEncryption(
          EncryptionType.KMS,
          KeyId("12345678-1234-1234-1234-123456789012"),
          Some(StreamName(streamName))
        )
      _ <- client
        .stopStreamEncryption(
          EncryptionType.KMS,
          KeyId("12345678-1234-1234-1234-123456789012"),
          Some(StreamName(streamName))
        )
      _ <- client
        .updateShardCount(
          PositiveIntegerObject(2),
          ScalingType.UNIFORM_SCALING,
          Some(StreamName(streamName))
        )
      shards2response <- client.listShards(Some(StreamName(streamName)))
      shards2 = shards2response.shards.getOrElse(fail("No shards returned"))
      newShards = shards2.takeRight(2)
      shard2 :: shard3 :: Nil = newShards
      _ <- client
        .mergeShards(
          shard2.shardId,
          shard3.shardId,
          Some(StreamName(streamName))
        )
      _ <- client
        .updateStreamMode(
          StreamARN(streamArn),
          StreamModeDetails(StreamMode.ON_DEMAND)
        )
      _ <- client.deleteStream(Some(StreamName(streamName)))
    } yield {
      assertEquals(List(record1, record2, record3), recordsParsed)
      assert(consumers.consumers.size === 1)
      assertEquals(
        tags.tags.map(x => (x.key, x.value)),
        List(TagKey("foo") -> Some(TagValue("bar")))
      )
    }
  }

}
