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

package kinesis4cats.client.producer

import cats.effect._
import cats.effect.syntax.all._
import cats.syntax.all._
import software.amazon.awssdk.services.kinesis.model.PutRecordsRequest
import software.amazon.awssdk.services.kinesis.model.PutRecordsResponse
import software.amazon.kinesis.processor.SingleStreamTracker

import kinesis4cats.Utils
import kinesis4cats.client.KinesisClient
import kinesis4cats.client.localstack.LocalstackKinesisClient
import kinesis4cats.client.logging.instances.show._
import kinesis4cats.client.producer.localstack.LocalstackKinesisProducer
import kinesis4cats.kcl.CommittableRecord
import kinesis4cats.kcl.localstack.LocalstackKCLConsumer
import kinesis4cats.kcl.logging.instances.show._
import kinesis4cats.models.StreamNameOrArn
import kinesis4cats.producer.Producer
import kinesis4cats.producer.ProducerSpec
import kinesis4cats.producer.ShardMapCache
import kinesis4cats.producer.logging.instances.show._
import kinesis4cats.syntax.bytebuffer._

class KinesisProducerNoShardMapSpec
    extends ProducerSpec[
      PutRecordsRequest,
      PutRecordsResponse,
      CommittableRecord[IO]
    ]() {
  override lazy val streamName: String =
    s"kinesis-client-producer-spec-${Utils.randomUUIDString}"
  override def producerResource
      : Resource[IO, Producer[IO, PutRecordsRequest, PutRecordsResponse]] =
    LocalstackKinesisProducer.resource[IO](
      streamName,
      shardMapF = (
          _: KinesisClient[IO],
          _: StreamNameOrArn,
          _: Async[IO]
      ) =>
        IO.pure(
          ShardMapCache
            .ListShardsError(
              new RuntimeException("Expected Exception")
            )
            .asLeft
        )
    )

  override def aAsBytes(a: CommittableRecord[IO]): Array[Byte] = a.data.asArray

  override def fixture(
      shardCount: Int,
      appName: String
  ): SyncIO[FunFixture[ProducerSpec.Resources[
    IO,
    PutRecordsRequest,
    PutRecordsResponse,
    CommittableRecord[IO]
  ]]] = ResourceFunFixture(
    for {
      _ <- LocalstackKinesisClient.streamResource[IO](streamName, shardCount)
      deferredWithResults <- LocalstackKCLConsumer.kclConsumerWithResults(
        new SingleStreamTracker(streamName),
        appName
      )((_: List[CommittableRecord[IO]]) => IO.unit)
      _ <- deferredWithResults.deferred.get.toResource
      producer <- producerResource
    } yield ProducerSpec.Resources(deferredWithResults.resultsQueue, producer)
  )

}
