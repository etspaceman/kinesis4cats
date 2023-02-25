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

import _root_.fs2.io.net.tls.TLSContext
import cats.effect._
import cats.effect.syntax.all._
import com.amazonaws.kinesis.PutRecordsInput
import com.amazonaws.kinesis.PutRecordsOutput
import org.http4s.ember.client.EmberClientBuilder
import org.typelevel.log4cats.slf4j.Slf4jLogger
import smithy4s.aws.kernel.AwsRegion

import kinesis4cats.Utils
import kinesis4cats.kcl.CommittableRecord
import kinesis4cats.kcl.localstack.LocalstackKCLConsumer
import kinesis4cats.kcl.logging.instances.show._
import kinesis4cats.localstack.LocalstackConfig
import kinesis4cats.localstack.Protocol
import kinesis4cats.logging.instances.show._
import kinesis4cats.models
import kinesis4cats.producer.Producer
import kinesis4cats.producer.ProducerSpec
import kinesis4cats.producer.logging.instances.show._
import kinesis4cats.smithy4s.client.KinesisClient
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
    s"kinesis-client-producer-spec-${Utils.randomUUIDString}"

  def http4sClientResource = for {
    tlsContext <- TLSContext.Builder.forAsync[IO].insecureResource
    client <- EmberClientBuilder.default[IO].withTLSContext(tlsContext).build
  } yield client

  lazy val region = IO.pure(AwsRegion.US_EAST_1)

  override def producerResource
      : Resource[IO, Producer[IO, PutRecordsInput, PutRecordsOutput]] =
    for {
      http4sClient <- http4sClientResource
      producer <- LocalstackKinesisProducer
        .resource[IO](
          http4sClient,
          region,
          Producer.Config.default(models.StreamNameOrArn.Name(streamName)),
          // TODO: Go back to default when Localstack updates to the newest kinesis-mock
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
          (_: Async[IO]) => Slf4jLogger.create[IO],
          (
              client: KinesisClient[IO],
              streamNameOrArn: models.StreamNameOrArn,
              _: Async[IO]
          ) => KinesisProducer.getShardMap(client, streamNameOrArn)
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
  ]]] = ResourceFunFixture(
    for {
      http4sClient <- http4sClientResource
      _ <- LocalstackKinesisClient
        .streamResource[IO](
          http4sClient,
          region,
          streamName,
          shardCount,
          loggerF = (f: Async[IO]) => Slf4jLogger.create[IO](f, implicitly)
        )
      deferredWithResults <- LocalstackKCLConsumer.kclConsumerWithResults(
        streamName,
        appName
      )((_: List[CommittableRecord[IO]]) => IO.unit)
      _ <- deferredWithResults.deferred.get.toResource
      producer <- producerResource
    } yield ProducerSpec.Resources(deferredWithResults.resultsQueue, producer)
  )

}
