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

import io.opentelemetry.sdk.metrics.data.MetricData
import io.opentelemetry.sdk.metrics.data.MetricDataType
import io.opentelemetry.sdk.metrics.data.PointData
import software.amazon.awssdk.services.cloudwatch.model.Dimension
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest
import software.amazon.awssdk.services.cloudwatch.model.StandardUnit
import software.amazon.awssdk.services.cloudwatch.model.StatisticSet

import kinesis4cats.producer.metrics.cloudwatch.CloudWatchConventions
import kinesis4cats.producer.metrics.cloudwatch.CloudWatchDatum

/** Translates aggregated OTel-Java `MetricData` into the neutral
  * [[kinesis4cats.producer.metrics.cloudwatch.CloudWatchDatum CloudWatchDatum]]
  * shape and on to an SDK-v2 `PutMetricDataRequest`. Only the metric kinds the
  * producer emits are handled (monotonic sums + histograms); other kinds yield
  * no datums.
  */
private[opentelemetry] object JvmMetricTranslation {

  def translate(md: MetricData): List[CloudWatchDatum] = {
    val unit = CloudWatchConventions.cloudWatchUnit(md.getUnit())
    md.getType() match {
      case MetricDataType.LONG_SUM =>
        md.getLongSumData().getPoints().asScala.toList.map { p =>
          datum(md.getName(), Left(p.getValue().toDouble), unit, p)
        }
      case MetricDataType.DOUBLE_SUM =>
        md.getDoubleSumData().getPoints().asScala.toList.map { p =>
          datum(md.getName(), Left(p.getValue()), unit, p)
        }
      case MetricDataType.HISTOGRAM =>
        md.getHistogramData().getPoints().asScala.toList.map { p =>
          val stats = CloudWatchDatum.StatisticSet(
            min = p.getMin(),
            max = p.getMax(),
            sum = p.getSum(),
            count = p.getCount().toDouble
          )
          datum(md.getName(), Right(stats), unit, p)
        }
      case _ => Nil
    }
  }

  private def datum(
      name: String,
      value: Either[Double, CloudWatchDatum.StatisticSet],
      unit: String,
      point: PointData
  ): CloudWatchDatum =
    CloudWatchDatum(
      metricName = name,
      value = value,
      unit = unit,
      dimensions = dimensions(point),
      timestampMs = point.getEpochNanos() / 1000000L
    )

  private def dimensions(point: PointData): List[(String, String)] =
    point
      .getAttributes()
      .asMap()
      .asScala
      .toList
      .map { case (k, v) => k.getKey() -> String.valueOf(v) }
      .sortBy(_._1)

  def toRequest(
      namespace: String,
      batch: List[CloudWatchDatum]
  ): PutMetricDataRequest =
    PutMetricDataRequest
      .builder()
      .namespace(namespace)
      .metricData(batch.map(toMetricDatum).asJava)
      .build()

  private def toMetricDatum(d: CloudWatchDatum): MetricDatum = {
    val base = MetricDatum
      .builder()
      .metricName(d.metricName)
      .timestamp(java.time.Instant.ofEpochMilli(d.timestampMs))
      .unit(StandardUnit.fromValue(d.unit))
      .dimensions(
        d.dimensions.map { case (k, v) =>
          Dimension.builder().name(k).value(v).build()
        }.asJava
      )
    d.value match {
      case Left(v)  => base.value(v).build()
      case Right(s) =>
        base
          .statisticValues(
            StatisticSet
              .builder()
              .minimum(s.min)
              .maximum(s.max)
              .sum(s.sum)
              .sampleCount(s.count)
              .build()
          )
          .build()
    }
  }
}
