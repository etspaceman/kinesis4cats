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

package kinesis4cats.producer.metrics.cloudwatch

/** A backend-neutral CloudWatch metric datum. Both the JVM (OTel-Java) and
  * cross (otel4s-sdk) metric translations produce this shape; each backend then
  * converts it to its own CloudWatch request type (AWS SDK v2 `MetricDatum` on
  * the JVM, the smithy4s-codegen `MetricDatum` on the cross path).
  *
  * @param value
  *   `Left(v)` for a single value (monotonic sum / gauge); `Right(stats)` for a
  *   histogram summarised as a CloudWatch StatisticSet.
  * @param unit
  *   a CloudWatch StandardUnit string (see
  *   [[CloudWatchConventions.cloudWatchUnit]]).
  * @param timestampMs
  *   epoch milliseconds; a `Long` to avoid `java.time` on JS/Native.
  */
final case class CloudWatchDatum(
    metricName: String,
    value: Either[Double, CloudWatchDatum.StatisticSet],
    unit: String,
    dimensions: List[(String, String)],
    timestampMs: Long
)

object CloudWatchDatum {
  final case class StatisticSet(
      min: Double,
      max: Double,
      sum: Double,
      count: Double
  )
}
