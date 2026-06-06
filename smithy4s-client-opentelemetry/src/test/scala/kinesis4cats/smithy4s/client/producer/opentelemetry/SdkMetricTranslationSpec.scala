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

import cats.data.NonEmptyVector
import munit.FunSuite
import org.typelevel.otel4s.Attribute
import org.typelevel.otel4s.Attributes
import org.typelevel.otel4s.metrics.BucketBoundaries
import org.typelevel.otel4s.sdk.TelemetryResource
import org.typelevel.otel4s.sdk.common.InstrumentationScope
import org.typelevel.otel4s.sdk.metrics.data.AggregationTemporality
import org.typelevel.otel4s.sdk.metrics.data.MetricData
import org.typelevel.otel4s.sdk.metrics.data.MetricPoints
import org.typelevel.otel4s.sdk.metrics.data.PointData
import org.typelevel.otel4s.sdk.metrics.data.TimeWindow

import kinesis4cats.producer.metrics.cloudwatch.CloudWatchDatum

class SdkMetricTranslationSpec extends FunSuite {

  private val scope =
    InstrumentationScope("kinesis4cats", None, None, Attributes.empty)
  private val attrs = Attributes(Attribute("stream.name", "test-stream"))
  private val window = TimeWindow(0.nanos, 2.millis)

  private def metric(
      name: String,
      unit: String,
      data: MetricPoints
  ): MetricData =
    MetricData(TelemetryResource.empty, scope, name, None, Some(unit), data)

  private val sumMetric: MetricData =
    metric(
      "kinesis4cats.producer.user_records.received",
      "{record}",
      MetricPoints.sum(
        NonEmptyVector.one(
          PointData.longNumber(window, attrs, Vector.empty, 3L)
        ),
        monotonic = true,
        AggregationTemporality.Cumulative
      )
    )

  private val histogramMetric: MetricData =
    metric(
      "kinesis4cats.producer.request.duration",
      "s",
      MetricPoints.histogram(
        NonEmptyVector.one(
          PointData.histogram(
            window,
            attrs,
            Vector.empty,
            Some(
              PointData.Histogram
                .Stats(sum = 6.0d, min = 1.0d, max = 5.0d, count = 2L)
            ),
            BucketBoundaries(Vector.empty),
            Vector(2L)
          )
        ),
        AggregationTemporality.Cumulative
      )
    )

  test("monotonic sum -> single-value datum (Count)") {
    val data = SdkMetricTranslation.translate(sumMetric)
    assertEquals(data.size, 1)
    val d = data.head
    assertEquals(
      d.value,
      Left(3.0d): Either[Double, CloudWatchDatum.StatisticSet]
    )
    assertEquals(d.unit, "Count")
    assertEquals(d.dimensions, List("stream.name" -> "test-stream"))
    assertEquals(d.timestampMs, 2L)
  }

  test("histogram -> StatisticSet datum (Seconds)") {
    val data = SdkMetricTranslation.translate(histogramMetric)
    assertEquals(data.size, 1)
    val d = data.head
    assertEquals(d.unit, "Seconds")
    assertEquals(
      d.value,
      Right(
        CloudWatchDatum.StatisticSet(
          min = 1.0d,
          max = 5.0d,
          sum = 6.0d,
          count = 2.0d
        )
      ): Either[Double, CloudWatchDatum.StatisticSet]
    )
  }
}
