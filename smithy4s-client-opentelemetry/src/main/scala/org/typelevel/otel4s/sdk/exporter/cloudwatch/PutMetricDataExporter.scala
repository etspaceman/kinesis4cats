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

package org.typelevel.otel4s.sdk.exporter.cloudwatch

import cats.Applicative
import cats.Foldable
import cats.syntax.all._
import org.typelevel.otel4s.sdk.metrics.data.MetricData
import org.typelevel.otel4s.sdk.metrics.exporter.AggregationSelector
import org.typelevel.otel4s.sdk.metrics.exporter.AggregationTemporalitySelector
import org.typelevel.otel4s.sdk.metrics.exporter.CardinalityLimitSelector
import org.typelevel.otel4s.sdk.metrics.exporter.MetricExporter

/** A custom push [[org.typelevel.otel4s.sdk.metrics.exporter.MetricExporter
  * MetricExporter]] for the otel4s pure-Scala SDK.
  *
  * `MetricExporter.Push` is a sealed trait whose only extension point
  * (`Push.Unsealed`) is `private[sdk]`; the built-in OTLP exporter extends it the
  * same way, by living under the `org.typelevel.otel4s.sdk` package. This class
  * therefore lives here rather than in the kinesis4cats namespace.
  *
  * It is deliberately generic: it knows nothing about CloudWatch. The caller
  * supplies `exportFn`, which translates and ships a batch of aggregated
  * `MetricData` (kinesis4cats keeps the CloudWatch translation + transport in its
  * own package). Cumulative temporality.
  */
final class PutMetricDataExporter[F[_]: Applicative](
    exportFn: List[MetricData] => F[Unit]
) extends MetricExporter.Push.Unsealed[F] {

  def name: String = "PutMetricDataExporter"

  def aggregationTemporalitySelector: AggregationTemporalitySelector =
    AggregationTemporalitySelector.alwaysCumulative

  def defaultAggregationSelector: AggregationSelector =
    AggregationSelector.default

  def defaultCardinalityLimitSelector: CardinalityLimitSelector =
    CardinalityLimitSelector.default

  def exportMetrics[G[_]: Foldable](metrics: G[MetricData]): F[Unit] =
    metrics.toList match {
      case Nil  => Applicative[F].unit
      case data => exportFn(data)
    }

  def flush: F[Unit] = Applicative[F].unit
}
