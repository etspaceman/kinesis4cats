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

import java.util.concurrent.CompletableFuture

import scala.jdk.CollectionConverters._

import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.metrics.InstrumentType
import io.opentelemetry.sdk.metrics.data.AggregationTemporality
import io.opentelemetry.sdk.metrics.data.MetricData
import io.opentelemetry.sdk.metrics.`export`.MetricExporter
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataResponse

import kinesis4cats.producer.metrics.cloudwatch.CloudWatchConventions
import kinesis4cats.producer.metrics.cloudwatch.CloudWatchDatum

/** A pure-Java OTel `MetricExporter` that pushes aggregated producer metrics to
  * CloudWatch via `PutMetricData`. Runs on the SDK's scheduler thread; bridges
  * the async client's `CompletableFuture` to the SDK's `CompletableResultCode`.
  * No cats-effect involvement.
  */
private[opentelemetry] class PutMetricDataExporter(
    client: CloudWatchAsyncClient,
    cloudWatchNamespace: String
) extends MetricExporter {

  def getAggregationTemporality(
      instrumentType: InstrumentType
  ): AggregationTemporality = AggregationTemporality.CUMULATIVE

  def `export`(
      metrics: java.util.Collection[MetricData]
  ): CompletableResultCode = {
    val data: List[CloudWatchDatum] =
      metrics.asScala.toList.flatMap(JvmMetricTranslation.translate)
    if (data.isEmpty) CompletableResultCode.ofSuccess()
    else {
      val result = new CompletableResultCode()
      val futures: List[CompletableFuture[PutMetricDataResponse]] =
        data
          .grouped(CloudWatchConventions.maxDatumsPerRequest)
          .toList
          .map(batch =>
            client.putMetricData(
              JvmMetricTranslation.toRequest(cloudWatchNamespace, batch)
            )
          )
      CompletableFuture
        .allOf(futures: _*)
        .whenComplete { (_, err) =>
          Option(err).fold { result.succeed(); () } { _ =>
            result.fail(); ()
          }
        }
      result
    }
  }

  def flush(): CompletableResultCode = CompletableResultCode.ofSuccess()

  def shutdown(): CompletableResultCode = CompletableResultCode.ofSuccess()
}
