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

import scala.jdk.CollectionConverters._

import io.opentelemetry.api.common.Attributes
import io.opentelemetry.sdk.common.InstrumentationScopeInfo
import io.opentelemetry.sdk.metrics.data.AggregationTemporality
import io.opentelemetry.sdk.metrics.data.HistogramPointData
import io.opentelemetry.sdk.metrics.data.LongPointData
import io.opentelemetry.sdk.metrics.data.MetricData
import io.opentelemetry.sdk.metrics.internal.data.ImmutableHistogramData
import io.opentelemetry.sdk.metrics.internal.data.ImmutableHistogramPointData
import io.opentelemetry.sdk.metrics.internal.data.ImmutableLongPointData
import io.opentelemetry.sdk.metrics.internal.data.ImmutableMetricData
import io.opentelemetry.sdk.metrics.internal.data.ImmutableSumData
import io.opentelemetry.sdk.resources.Resource
import munit.FunSuite

import kinesis4cats.producer.metrics.cloudwatch.CloudWatchDatum

class JvmMetricTranslationSpec extends FunSuite {

  private val res = Resource.empty()
  private val scope = InstrumentationScopeInfo.create("kinesis4cats")

  private val streamAttrs =
    Attributes.builder().put("stream.name", "test-stream").build()

  private def sumMetric: MetricData =
    ImmutableMetricData.createLongSum(
      res,
      scope,
      "kinesis4cats.producer.user_records.received",
      "User records received",
      "{record}",
      ImmutableSumData.create(
        true,
        AggregationTemporality.CUMULATIVE,
        List[LongPointData](
          ImmutableLongPointData.create(0L, 2000000L, streamAttrs, 3L)
        ).asJava
      )
    )

  private def histogramMetric: MetricData =
    ImmutableMetricData.createDoubleHistogram(
      res,
      scope,
      "kinesis4cats.producer.request.duration",
      "Request duration",
      "s",
      ImmutableHistogramData.create(
        AggregationTemporality.CUMULATIVE,
        List[HistogramPointData](
          ImmutableHistogramPointData.create(
            0L, // startEpochNanos
            2000000L, // epochNanos
            streamAttrs,
            6.0d, // sum
            true, // hasMin
            1.0d, // min
            true, // hasMax
            5.0d, // max
            List.empty[java.lang.Double].asJava, // boundaries (0 -> 1 bucket)
            List(java.lang.Long.valueOf(2L)).asJava // counts -> total 2
          )
        ).asJava
      )
    )

  test("monotonic sum translates to a single-value datum (Count unit)") {
    val data = JvmMetricTranslation.translate(sumMetric)
    assertEquals(data.size, 1)
    val d = data.head
    assertEquals(d.metricName, "kinesis4cats.producer.user_records.received")
    assertEquals(
      d.value,
      Left(3.0d): Either[Double, CloudWatchDatum.StatisticSet]
    )
    assertEquals(d.unit, "Count")
    assertEquals(d.dimensions, List("stream.name" -> "test-stream"))
    assertEquals(d.timestampMs, 2L) // 2_000_000 ns -> 2 ms
  }

  test("histogram translates to a StatisticSet datum (Seconds unit)") {
    val data = JvmMetricTranslation.translate(histogramMetric)
    assertEquals(data.size, 1)
    val d = data.head
    assertEquals(d.metricName, "kinesis4cats.producer.request.duration")
    assertEquals(d.unit, "Seconds")
    assertEquals(
      d.value,
      Right(
        CloudWatchDatum.StatisticSet(min = 1.0d, max = 5.0d, sum = 6.0d, count = 2.0d)
      ): Either[Double, CloudWatchDatum.StatisticSet]
    )
  }
}
