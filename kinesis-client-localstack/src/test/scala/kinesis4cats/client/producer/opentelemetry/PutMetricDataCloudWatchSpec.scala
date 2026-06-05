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

package kinesis4cats.client.producer.opentelemetry

import scala.concurrent.duration._
import scala.jdk.CollectionConverters._

import cats.effect.IO
import org.typelevel.otel4s.Attribute
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient
import software.amazon.awssdk.services.cloudwatch.model.ListMetricsRequest

import kinesis4cats.Utils
import kinesis4cats.localstack.LocalstackConfig
import kinesis4cats.localstack.aws.v2.AwsClients
import kinesis4cats.producer.metrics.cloudwatch.CloudWatchConventions
import kinesis4cats.testkit.IntegrationSuite

/** Exercises the full PutMetricData export pipeline (otel4s `MeterProvider` ->
  * custom exporter -> SDK-v2 CloudWatch client) against Localstack CloudWatch.
  */
class PutMetricDataCloudWatchSpec extends IntegrationSuite {

  private val streamName =
    s"pmd-otel-spec-${Utils.randomUUIDString}"

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

  test("PutMetricData export reaches Localstack CloudWatch") {
    LocalstackConfig.load[IO]().flatMap { config =>
      AwsClients.cloudwatchClientResource[IO](config).use { cwClient =>
        // Record one observation, then RELEASE the MeterProvider (the nested
        // `use` completes here) to force a final flush/export to Localstack
        // CloudWatch before we poll.
        val recordAndFlush = PutMetricDataMeterProvider
          .fromClientResource[IO](
            CloudWatchConventions.defaultCloudWatchNamespace,
            AwsClients.cloudwatchClientResource[IO](config)
          )
          .use { mp =>
            for {
              meter <- mp.get("kinesis4cats")
              counter <- meter
                .counter[Long](metricName)
                .withUnit("{record}")
                .create
              _ <- counter.add(3L, Attribute("stream.name", streamName))
            } yield ()
          }

        recordAndFlush *>
          // Localstack CloudWatch is eventually consistent; poll briefly.
          (IO.sleep(1.second) *> listMetricNames(cwClient).map(
            _.contains(metricName)
          ))
            .iterateUntil(identity)
            .timeout(30.seconds)
            .map(found => assert(found, s"$metricName not found in CloudWatch"))
      }
    }
  }
}
