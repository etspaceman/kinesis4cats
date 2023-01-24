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

package kinesis4cats.kpl

import scala.concurrent.duration._

import java.nio.ByteBuffer

import cats.effect.{IO, Resource, SyncIO}
import com.amazonaws.services.kinesis.producer._
import io.circe.syntax._

import kinesis4cats.localstack.LocalstackConfig
import kinesis4cats.localstack.aws.v1.{AwsClients, AwsCreds}

abstract class KPLProducerSpec(implicit LE: KPLProducerLogEncoders)
    extends munit.CatsEffectSuite
    with munit.CatsEffectFunFixtures {
  def fixture(
      streamName: String,
      shardCount: Int
  ): SyncIO[FunFixture[KPLProducer[IO]]] = ResourceFixture(
    KPLProducerSpec.resource(streamName, shardCount)
  )

  fixture("test1", 1).test("It should produce successfully") { producer =>
    val testData = TestData("foo", 1.0f, 2.0, true, 3, 4L)
    val testDataBB = ByteBuffer.wrap(testData.asJson.noSpaces.getBytes())

    producer.put(new UserRecord("test1", "partitionKey", testDataBB)).map {
      result =>
        assert(result.isSuccessful())
    }
  }
}

object KPLProducerSpec {

  def resource(
      streamName: String,
      shardCount: Int
  )(implicit LE: KPLProducerLogEncoders): Resource[IO, KPLProducer[IO]] = for {
    config <- LocalstackConfig.resource[IO]()
    _ <- AwsClients
      .kinesisStreamResource[IO](config, streamName, shardCount, 5, 500.millis)
    producer <- KPLProducer[IO](
      new KinesisProducerConfiguration()
        .setVerifyCertificate(false)
        .setKinesisEndpoint(config.host)
        .setCloudwatchEndpoint(config.host)
        .setCredentialsProvider(AwsCreds.LocalCredsProvider)
        .setKinesisPort(config.servicePort.toLong)
        .setCloudwatchPort(config.servicePort.toLong)
        .setMetricsLevel("none")
        .setLogLevel("warning")
        .setRegion(config.region.name)
    )
  } yield producer
}
