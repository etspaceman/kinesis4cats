/*
 * Copyright 2023-2026 etspaceman
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

package kinesis4cats.smithy4s.client.producer.opentelemetry

import scala.concurrent.duration._
import scala.jdk.CollectionConverters._

import cats.effect.IO
import cats.effect.Resource
import fs2.io.compression._
import org.http4s.blaze.client.BlazeClientBuilder
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.otel4s.Attribute
import smithy4s.aws.kernel.AwsCredentials
import smithy4s.aws.kernel.AwsRegion
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient
import software.amazon.awssdk.services.cloudwatch.model.ListMetricsRequest

import kinesis4cats.SSL
import kinesis4cats.Utils
import kinesis4cats.localstack.LocalstackConfig
import kinesis4cats.localstack.aws.v2.AwsClients
import kinesis4cats.producer.metrics.cloudwatch.CloudWatchConventions
import kinesis4cats.smithy4s.client.middleware.LocalstackProxy
import kinesis4cats.testkit.IntegrationSuite

/** Exercises the full PutMetricData export pipeline (otel4s-sdk `MeterProvider`
  * -> custom exporter -> smithy4s AwsQuery CloudWatch client) against Localstack
  * CloudWatch.
  */
class PutMetricDataCloudWatchSpec extends IntegrationSuite {

  private val mockCreds: IO[AwsCredentials] =
    IO.pure(AwsCredentials.Default("mock-key", "mock-secret", None))

  private val streamName =
    s"pmd-smithy4s-otel-spec-${Utils.randomUUIDString}"

  private val metricName = "kinesis4cats.producer.user_records.received"

  private def listMetricNames(
      client: CloudWatchAsyncClient
  ): IO[List[String]] =
    IO.fromCompletableFuture(
      IO.delay(
        client.listMetrics(
          ListMetricsRequest
            .builder()
            .namespace(CloudWatchConventions.defaultCloudWatchNamespace)
            .build()
        )
      )
    ).map(_.metrics().asScala.toList.map(_.metricName()))

  test("PutMetricData export reaches Localstack CloudWatch (smithy4s)") {
    val resources: Resource[IO, CloudWatchAsyncClient] = for {
      config <- Resource.eval(LocalstackConfig.load[IO]())
      cwClient <- AwsClients.cloudwatchClientResource[IO](config)
      baseClient <- BlazeClientBuilder[IO].withSslContext(SSL.context).resource
      proxiedClient = LocalstackProxy[IO](Slf4jLogger.getLogger[IO])(baseClient)
      _ <- PutMetricDataMeterProvider
        .resource[IO](
          AwsRegion.US_EAST_1,
          proxiedClient,
          CloudWatchConventions.defaultCloudWatchNamespace,
          _ => Resource.pure(mockCreds)
        )
        .evalMap { mp =>
          for {
            meter <- mp.get("kinesis4cats")
            counter <- meter
              .counter[Long](metricName)
              .withUnit("{record}")
              .create
            _ <- counter.add(3L, Attribute("stream.name", streamName))
          } yield ()
        }
    } yield cwClient

    resources.use { cwClient =>
      (IO.sleep(1.second) *> listMetricNames(cwClient).map(_.contains(metricName)))
        .iterateUntil(identity)
        .timeout(30.seconds)
        .map(found => assert(found, s"$metricName not found in CloudWatch"))
    }
  }
}
