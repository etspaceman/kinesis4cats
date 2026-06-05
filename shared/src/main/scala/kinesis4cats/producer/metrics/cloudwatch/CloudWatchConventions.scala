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

/** Shared conventions for translating otel4s producer metrics to CloudWatch:
  * the default CloudWatch namespace, otel-unit -> StandardUnit mapping, the
  * dimension-key names (matching `MetricAttributes`), and the PutMetricData
  * batch limit. Pure constants/helpers -- no otel or AWS dependency.
  */
private[kinesis4cats] object CloudWatchConventions {

  /** KPL-compatible default CloudWatch `Namespace` for the put-metric-data
    * grouping. Distinct from the otel metric-name prefix (`ProducerMetrics`
    * `defaultNamespace`). Override if KPL compatibility is not needed.
    */
  val defaultCloudWatchNamespace: String = "KinesisProducerLibrary"

  /** CloudWatch caps a single PutMetricData request at 1000 datums.
    * @see
    *   [[https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_PutMetricData.html PutMetricData]]
    */
  val maxDatumsPerRequest: Int = 1000

  // Dimension-key names; mirror kinesis4cats.producer.metrics.MetricAttributes.
  val DimStreamName: String = "stream.name"
  val DimShardId: String = "shard.id"
  val DimErrorCode: String = "error.code"
  val DimReason: String = "reason"

  /** Maps an otel unit annotation to a CloudWatch StandardUnit string. Unknown
    * units map to `"None"`.
    */
  def cloudWatchUnit(otelUnit: String): String = otelUnit match {
    case "By"                               => "Bytes"
    case "s"                                => "Seconds"
    case "{record}" | "{error}" | "{retry}" => "Count"
    case _                                  => "None"
  }
}
