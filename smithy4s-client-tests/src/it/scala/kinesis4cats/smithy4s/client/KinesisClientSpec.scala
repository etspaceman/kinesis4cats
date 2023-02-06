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

package kinesis4cats.smithy4s.client

import java.util.UUID

import cats.effect.{IO, SyncIO}
import cats.syntax.all._
import io.circe.parser._
import io.circe.syntax._
import org.scalacheck.Arbitrary
import com.amazonaws.kinesis._

import kinesis4cats.smithy4s.client.localstack.LocalstackKinesisClient
import kinesis4cats.localstack._
import kinesis4cats.localstack.syntax.scalacheck._
import kinesis4cats.models.StreamArn
import org.http4s.ember.client.EmberClientBuilder
import smithy4s.aws.AwsRegion
import smithy4s.ByteArray

abstract class KinesisClientSpec(implicit LE: KinesisClient.LogEncoders[IO])
    extends munit.CatsEffectSuite {

  val region = AwsRegion.US_EAST_1
  def fixture: SyncIO[FunFixture[KinesisClient[IO]]] =
    ResourceFixture(
      for {
        underlying <- EmberClientBuilder.default[IO].build
        client <- LocalstackKinesisClient.clientResource(
          underlying,
          region
        )
      } yield client
    )

  fixture.test("It should work through all commands") { client =>
    val streamName =
      s"smithy4s-kinesis-client-spec-${UUID.randomUUID().toString()}"
    val accountId = "000000000000"

    val streamArn = StreamArn(
      kinesis4cats.models.AwsRegion.values
        .find(_.name === region.value)
        .getOrElse(fail("Could not find region")),
      streamName,
      accountId
    ).streamArn

    for {
      _ <- client.createStream(StreamName(streamName), Some(1)).run
      _ <- client
        .addTagsToStream(
          Map(TagKey("foo") -> TagValue("bar")),
          Some(StreamName(streamName))
        )
        .run
      _ <- client
        .increaseStreamRetentionPeriod(
          RetentionPeriodHours(48),
          Some(StreamName(streamName))
        )
        .run
      _ <- client
        .decreaseStreamRetentionPeriod(
          RetentionPeriodHours(24),
          Some(StreamName(streamName))
        )
        .run
      _ <- client
        .registerStreamConsumer(StreamARN(streamArn), ConsumerName("foo"))
        .run
      _ <- client.describeLimits().run
      _ <- client.describeStream(Some(StreamName(streamName))).run
      _ <- client
        .describeStreamConsumer(
          Some(StreamARN(streamArn)),
          Some(ConsumerName("foo"))
        )
        .run
      _ <- client.describeStreamSummary(Some(StreamName(streamName))).run
      _ <- client
        .enableEnhancedMonitoring(
          List(MetricsName.ALL),
          Some(StreamName(streamName))
        )
        .run
      _ <- client
        .disableEnhancedMonitoring(
          List(MetricsName.ALL),
          Some(StreamName(streamName))
        )
        .run
      record1 <- IO(Arbitrary.arbitrary[TestData].one)
      _ <- client
        .putRecord(
          Data(ByteArray(record1.asJson.noSpaces.getBytes())),
          PartitionKey("foo"),
          Some(StreamName(streamName))
        )
        .run
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
        .run
      shards <- client.listShards(Some(StreamName(streamName))).run
      shard = shards.shards.map(_.head).getOrElse(fail("No shards returned"))
      shardIterator <- client
        .getShardIterator(
          shard.shardId,
          ShardIteratorType.TRIM_HORIZON,
          Some(StreamName(streamName))
        )
        .run
      records <- client
        .getRecords(
          shardIterator.shardIterator
            .getOrElse(fail("No shard iterator returned")),
          streamARN = Some(StreamARN(streamArn))
        )
        .run
      recordBytes = records.records
        .map(x => new String(x.data.value.array))
      recordsParsed <- recordBytes.traverse(bytes =>
        IO.fromEither(decode[TestData](bytes))
      )
      consumers <- client.listStreamConsumers(StreamARN(streamArn)).run
      _ <- client
        .deregisterStreamConsumer(
          Some(StreamARN(streamArn)),
          Some(ConsumerName("foo"))
        )
        .run
      tags <- client.listTagsForStream(Some(StreamName(streamName))).run
      _ <- client
        .removeTagsFromStream(List(TagKey("foo")), Some(StreamName(streamName)))
        .run
      _ <- client
        .startStreamEncryption(
          EncryptionType.KMS,
          KeyId("12345678-1234-1234-1234-123456789012"),
          Some(StreamName(streamName))
        )
        .run
      _ <- client
        .stopStreamEncryption(
          EncryptionType.KMS,
          KeyId("12345678-1234-1234-1234-123456789012"),
          Some(StreamName(streamName))
        )
        .run
      _ <- client
        .updateShardCount(
          PositiveIntegerObject(2),
          ScalingType.UNIFORM_SCALING,
          Some(StreamName(streamName))
        )
        .run
      shards2response <- client.listShards(Some(StreamName(streamName))).run
      shards2 = shards2response.shards.getOrElse(fail("No shards returned"))
      newShards = shards2.takeRight(2)
      shard2 :: shard3 :: Nil = newShards
      _ <- client
        .mergeShards(
          shard2.shardId,
          shard3.shardId,
          Some(StreamName(streamName))
        )
        .run
      _ <- client
        .updateStreamMode(
          StreamARN(streamArn),
          StreamModeDetails(StreamMode.ON_DEMAND)
        )
        .run
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
