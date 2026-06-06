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

import org.typelevel.otel4s.sdk.metrics.data.MetricData
import org.typelevel.otel4s.sdk.metrics.data.MetricPoints
import org.typelevel.otel4s.sdk.metrics.data.PointData

import kinesis4cats.producer.metrics.cloudwatch.CloudWatchConventions
import kinesis4cats.producer.metrics.cloudwatch.CloudWatchDatum

/** Translates aggregated otel4s-sdk `MetricData` into the neutral
  * [[kinesis4cats.producer.metrics.cloudwatch.CloudWatchDatum CloudWatchDatum]]
  * shape. Mirrors the JVM translation; only producer metric kinds (monotonic
  * sums + histograms) are handled.
  */
private[opentelemetry] object SdkMetricTranslation {

  def translate(md: MetricData): List[CloudWatchDatum] = {
    val unit = CloudWatchConventions.cloudWatchUnit(md.unit.getOrElse(""))
    md.data match {
      case sum: MetricPoints.Sum =>
        sum.points.toVector.toList.map { p =>
          datum(md.name, Left(numberValue(p)), unit, p)
        }
      case hist: MetricPoints.Histogram =>
        hist.points.toVector.toList.map { p =>
          val stats = CloudWatchDatum.StatisticSet(
            min = p.stats.flatMap(_.min).getOrElse(0.0d),
            max = p.stats.flatMap(_.max).getOrElse(0.0d),
            sum = p.stats.map(_.sum).getOrElse(0.0d),
            count = p.stats
              .map(_.count.toDouble)
              .getOrElse(p.counts.sum.toDouble)
          )
          datum(md.name, Right(stats), unit, p)
        }
      case _ => Nil
    }
  }

  private def numberValue(p: PointData.NumberPoint): Double = p match {
    case lp: PointData.LongNumber   => lp.value.toDouble
    case dp: PointData.DoubleNumber => dp.value
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
      dimensions = point.attributes.toList
        .map(a => a.key.name -> a.value.toString)
        .sortBy(_._1),
      timestampMs = point.timeWindow.end.toMillis
    )
}
