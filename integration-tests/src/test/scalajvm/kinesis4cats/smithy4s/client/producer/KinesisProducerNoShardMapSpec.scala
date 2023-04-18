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

import cats.effect._
import cats.effect.syntax.all._
import cats.syntax.all._
import com.amazonaws.kinesis.PutRecordsInput
import com.amazonaws.kinesis.PutRecordsOutput
import org.http4s.blaze.client.BlazeClientBuilder
import org.typelevel.log4cats.slf4j.Slf4jLogger
import smithy4s.aws.kernel.AwsRegion
import software.amazon.kinesis.common._
import software.amazon.kinesis.processor.SingleStreamTracker

import kinesis4cats.SSL
import kinesis4cats.Utils
import kinesis4cats.kcl.CommittableRecord
import kinesis4cats.kcl.localstack.LocalstackKCLConsumer
import kinesis4cats.models.StreamNameOrArn
import kinesis4cats.producer.Producer
import kinesis4cats.producer.ProducerSpec
import kinesis4cats.producer.ShardMapCache
import kinesis4cats.smithy4s.client.KinesisClient
import kinesis4cats.smithy4s.client.localstack.LocalstackKinesisClient
import kinesis4cats.smithy4s.client.producer.localstack.LocalstackKinesisProducer
import kinesis4cats.syntax.bytebuffer._

class KinesisProducerNoShardMapSpec
    extends ProducerSpec[
      PutRecordsInput,
      PutRecordsOutput,
      CommittableRecord[IO]
    ]() {
  override lazy val streamName: String =
    s"kinesis-client-producer-no-shard-map-spec-${Utils.randomUUIDString}"

  def http4sClientResource =
    BlazeClientBuilder[IO].withSslContext(SSL.context).resource

  lazy val region = IO.pure(AwsRegion.US_EAST_1)

  override def producerResource
      : Resource[IO, Producer[IO, PutRecordsInput, PutRecordsOutput]] =
    for {
      http4sClient <- http4sClientResource
      producer <- LocalstackKinesisProducer
        .resource[IO](
          http4sClient,
          streamName,
          region,
          loggerF = (_: Async[IO]) => Slf4jLogger.create[IO],
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
        new SingleStreamTracker(
          StreamIdentifier.singleStreamInstance(streamName),
          InitialPositionInStreamExtended.newInitialPosition(
            InitialPositionInStream.TRIM_HORIZON
          )
        ),
        appName
      )((_: List[CommittableRecord[IO]]) => IO.unit)
      _ <- deferredWithResults.deferred.get.toResource
      producer <- producerResource
    } yield ProducerSpec.Resources(deferredWithResults.resultsQueue, producer)
  )

}
