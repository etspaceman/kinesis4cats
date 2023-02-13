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

package kinesis4cats.smithy4s.client.producer

import java.util.UUID

import cats.effect._
import cats.effect.syntax.all._
import com.amazonaws.kinesis.PutRecordsInput
import com.amazonaws.kinesis.PutRecordsOutput
import org.http4s.ember.client.EmberClientBuilder
import org.typelevel.log4cats.slf4j.Slf4jLogger
import smithy4s.aws.kernel.AwsRegion

import kinesis4cats.kcl.CommittableRecord
import kinesis4cats.kcl.localstack.LocalstackKCLConsumer
import kinesis4cats.kcl.logging.instances.show._
import kinesis4cats.logging.instances.show._
import kinesis4cats.producer.Producer
import kinesis4cats.producer.ProducerSpec
import kinesis4cats.producer.logging.instances.show._
import kinesis4cats.smithy4s.client.localstack.LocalstackKinesisClient
import kinesis4cats.smithy4s.client.logging.instances.show._
import kinesis4cats.smithy4s.client.producer.localstack.LocalstackKinesisProducer
import kinesis4cats.syntax.bytebuffer._

class KinesisProducerSpec
    extends ProducerSpec[
      PutRecordsInput,
      PutRecordsOutput,
      CommittableRecord[IO]
    ]() {
  override lazy val streamName: String =
    s"kinesis-client-producer-spec-${UUID.randomUUID().toString()}"

  val emberClientResource = EmberClientBuilder
    .default[IO]
    .withoutCheckEndpointAuthentication
    .build

  val region = IO.pure(AwsRegion.US_EAST_1)

  override def producerResource
      : Resource[IO, Producer[IO, PutRecordsInput, PutRecordsOutput]] =
    for {
      http4sClient <- emberClientResource
      producer <- LocalstackKinesisProducer
        .resource[IO](
          http4sClient,
          streamName,
          region,
          loggerF = (_: Async[IO]) => Slf4jLogger.create[IO]
        )
    } yield producer

  override def aAsBytes(a: CommittableRecord[IO]): Array[Byte] = a.data.asArray

  override def fixture(
      shardCount: Int,
      appName: String
  ): SyncIO[FunFixture[ProducerSpec.Resources[
    IO,
    PutRecordsInput,
    PutRecordsOutput,
    CommittableRecord[IO]
  ]]] = ResourceFixture(
    for {
      http4sClient <- emberClientResource
      _ <- LocalstackKinesisClient
        .streamResource[IO](http4sClient, region, streamName, shardCount)
      deferredWithResults <- LocalstackKCLConsumer.kclConsumerWithResults(
        streamName,
        appName
      )((_: List[CommittableRecord[IO]]) => IO.unit)
      _ <- deferredWithResults.deferred.get.toResource
      producer <- producerResource
    } yield ProducerSpec.Resources(deferredWithResults.resultsQueue, producer)
  )

}
