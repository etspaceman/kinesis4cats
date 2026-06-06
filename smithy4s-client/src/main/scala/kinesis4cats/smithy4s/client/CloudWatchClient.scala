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

package kinesis4cats
package smithy4s.client

import _root_.smithy4s.aws._
import _root_.smithy4s.time.{Timestamp => SmithyTimestamp}
import cats.effect.{Async, Resource}
import cats.syntax.all._
import com.amazonaws.cloudwatch._
import fs2.compression.Compression
import org.http4s.client.Client

import kinesis4cats.producer.metrics.cloudwatch.CloudWatchConventions
import kinesis4cats.producer.metrics.cloudwatch.PutMetricDataSink
import kinesis4cats.producer.metrics.cloudwatch.{CloudWatchDatum => Datum}

/** Builds a
  * [[kinesis4cats.producer.metrics.cloudwatch.PutMetricDataSink PutMetricDataSink]]
  * backed by the smithy4s-codegen CloudWatch client (AwsQuery protocol, signed
  * by the smithy4s-aws middleware). Cross-platform (JVM/JS/Native).
  */
object CloudWatchClient {

  def resource[F[_]: Compression](
      httpClient: Client[F],
      region: AwsRegion,
      credentials: Client[F] => Resource[F, F[AwsCredentials]]
  )(implicit F: Async[F]): Resource[F, PutMetricDataSink[F]] =
    for {
      creds <- credentials(httpClient)
      environment = AwsEnvironment.make[F](
        httpClient,
        F.pure(region),
        creds,
        F.realTime.map(_.toSeconds).map(Timestamp(_, 0))
      )
      cw <- AwsClient(CloudWatch.service, environment)
    } yield new PutMetricDataSink[F] {
      def putMetricData(
          namespace: String,
          data: List[Datum]
      ): F[Unit] =
        data
          .grouped(CloudWatchConventions.maxDatumsPerRequest)
          .toList
          .traverse_ { batch =>
            cw.putMetricData(
              namespace = Namespace(namespace),
              metricData = Some(batch.map(toMetricDatum))
            ).void
          }
    }

  private def toMetricDatum(d: Datum): MetricDatum = {
    val dims = d.dimensions.map { case (k, v) =>
      Dimension(name = DimensionName(k), value = DimensionValue(v))
    }
    val base = MetricDatum(
      metricName = MetricName(d.metricName),
      dimensions = if (dims.isEmpty) None else Some(dims),
      timestamp = Some(SmithyTimestamp.fromEpochMilli(d.timestampMs)),
      unit = Some(StandardUnit.fromStringOrUnknown(d.unit))
    )
    d.value match {
      case Left(v)  => base.copy(value = Some(DatapointValue(v)))
      case Right(s) =>
        base.copy(statisticValues =
          Some(
            StatisticSet(
              sampleCount = DatapointValue(s.count),
              sum = DatapointValue(s.sum),
              minimum = DatapointValue(s.min),
              maximum = DatapointValue(s.max)
            )
          )
        )
    }
  }
}
