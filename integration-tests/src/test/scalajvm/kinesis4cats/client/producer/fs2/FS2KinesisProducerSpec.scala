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

package kinesis4cats.client.producer.fs2

import cats.effect._
import cats.effect.syntax.all._
import software.amazon.awssdk.services.kinesis.model.PutRecordsRequest
import software.amazon.awssdk.services.kinesis.model.PutRecordsResponse
import software.amazon.kinesis.common._
import software.amazon.kinesis.processor.SingleStreamTracker

import kinesis4cats.Utils
import kinesis4cats.client.producer.fs2.localstack.LocalstackFS2KinesisProducer
import kinesis4cats.kcl.CommittableRecord
import kinesis4cats.kcl.localstack.LocalstackKCLConsumer
import kinesis4cats.localstack.TestStreamConfig
import kinesis4cats.models.StreamNameOrArn
import kinesis4cats.producer.fs2.FS2Producer
import kinesis4cats.producer.fs2.FS2ProducerSpec
import kinesis4cats.syntax.bytebuffer._

class KinesisFS2ProducerSpec
    extends FS2ProducerSpec[
      PutRecordsRequest,
      PutRecordsResponse,
      CommittableRecord[IO]
    ]() {
  override lazy val streamName: String =
    s"kinesis-client-fs2-producer-spec-${Utils.randomUUIDString}"
  override def producerResource
      : Resource[IO, FS2Producer[IO, PutRecordsRequest, PutRecordsResponse]] =
    LocalstackFS2KinesisProducer.Builder
      .default[IO](StreamNameOrArn.Name(streamName))
      .map(_.withStreamsToCreate(List(TestStreamConfig.default(streamName, 3))))
      .toResource
      .flatMap(_.build)

  override def aAsBytes(a: CommittableRecord[IO]): Array[Byte] = a.data.asArray

  override def fixture(
      appName: String
  ): SyncIO[FunFixture[FS2ProducerSpec.Resources[
    IO,
    PutRecordsRequest,
    PutRecordsResponse,
    CommittableRecord[IO]
  ]]] = ResourceFunFixture(
    for {
      producer <- producerResource
      builder <- LocalstackKCLConsumer.Builder.default[IO](
        new SingleStreamTracker(
          StreamIdentifier.singleStreamInstance(streamName),
          InitialPositionInStreamExtended.newInitialPosition(
            InitialPositionInStream.TRIM_HORIZON
          )
        ),
        appName
      )
      deferredWithResults <- builder.runWithResults()
      _ <- deferredWithResults.deferred.get.toResource
    } yield FS2ProducerSpec.Resources(
      deferredWithResults.resultsQueue,
      producer
    )
  )

}
