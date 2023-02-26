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

package kinesis4cats.smithy4s.client.producer.fs2

import scala.concurrent.duration._

import cats.effect._
import cats.effect.syntax.all._
import com.amazonaws.kinesis.PutRecordsInput
import com.amazonaws.kinesis.PutRecordsOutput
import org.http4s.blaze.client.BlazeClientBuilder
import org.typelevel.log4cats.slf4j.Slf4jLogger
import smithy4s.aws.kernel.AwsRegion
import software.amazon.kinesis.common.InitialPositionInStream
import software.amazon.kinesis.common.InitialPositionInStreamExtended

import kinesis4cats.SSL
import kinesis4cats.Utils
import kinesis4cats.kcl.CommittableRecord
import kinesis4cats.kcl.KCLConsumer
import kinesis4cats.kcl.localstack.LocalstackKCLConsumer
import kinesis4cats.kcl.logging.instances.show._
import kinesis4cats.localstack.Custom
import kinesis4cats.logging.instances.show._
import kinesis4cats.models
import kinesis4cats.producer.Producer
import kinesis4cats.producer.fs2.FS2Producer
import kinesis4cats.producer.fs2.FS2ProducerSpec
import kinesis4cats.producer.logging.instances.show._
import kinesis4cats.smithy4s.client.localstack.LocalstackKinesisClient
import kinesis4cats.smithy4s.client.logging.instances.show._
import kinesis4cats.smithy4s.client.producer.fs2.localstack.LocalstackFS2KinesisProducer
import kinesis4cats.syntax.bytebuffer._

class FS2KinesisProducerSpec
    extends FS2ProducerSpec[
      PutRecordsInput,
      PutRecordsOutput,
      CommittableRecord[IO]
    ]() {
  override lazy val streamName: String =
    s"kinesis-client-fs2-producer-spec-${Utils.randomUUIDString}"

  def http4sClientResource =
    BlazeClientBuilder[IO].withSslContext(SSL.context).resource

  lazy val region = IO.pure(AwsRegion.US_EAST_1)

  override def producerResource
      : Resource[IO, FS2Producer[IO, PutRecordsInput, PutRecordsOutput]] =
    for {
      http4sClient <- http4sClientResource
      producer <- LocalstackFS2KinesisProducer
        .resource[IO](
          http4sClient,
          region,
          FS2Producer.Config.default(models.StreamNameOrArn.Name(streamName)),
          // TODO: Go back to default when Localstack updates to the newest kinesis-mock
          Custom.kinesisMockConfig,
          (_: Async[IO]) => Slf4jLogger.create[IO],
          (_: Producer.Res[PutRecordsOutput], _: Async[IO]) => IO.unit
        )
    } yield producer

  override def aAsBytes(a: CommittableRecord[IO]): Array[Byte] = a.data.asArray

  override def fixture(
      shardCount: Int,
      appName: String
  ): SyncIO[FunFixture[FS2ProducerSpec.Resources[
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
          Custom.kinesisMockConfig,
          streamName,
          shardCount,
          5,
          500.millis,
          (f: Async[IO]) => Slf4jLogger.create[IO](f, implicitly)
        )
      deferredWithResults <- LocalstackKCLConsumer.kclConsumerWithResults(
        Custom.kinesisMockConfig,
        streamName,
        appName,
        Utils.randomUUIDString,
        InitialPositionInStreamExtended.newInitialPosition(
          InitialPositionInStream.TRIM_HORIZON
        ),
        KCLConsumer.ProcessConfig.default,
        100
      )((_: List[CommittableRecord[IO]]) => IO.unit)
      _ <- deferredWithResults.deferred.get.toResource
      producer <- producerResource
    } yield FS2ProducerSpec.Resources(
      deferredWithResults.resultsQueue,
      producer
    )
  )

}
