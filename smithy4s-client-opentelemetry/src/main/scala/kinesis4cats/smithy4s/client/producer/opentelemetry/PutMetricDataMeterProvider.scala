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

import cats.effect.Async
import cats.effect.Resource
import cats.effect.std.Console
import cats.effect.std.Random
import cats.mtl.Ask
import fs2.compression.Compression
import org.http4s.client.Client
import org.typelevel.otel4s.metrics.MeterProvider
import org.typelevel.otel4s.sdk.context.Context
import org.typelevel.otel4s.sdk.exporter.cloudwatch.PutMetricDataExporter
import org.typelevel.otel4s.sdk.metrics.SdkMeterProvider
import org.typelevel.otel4s.sdk.metrics.exporter.MetricReader
import smithy4s.aws.kernel.AwsCredentials
import smithy4s.aws.kernel.AwsRegion

import kinesis4cats.producer.metrics.cloudwatch.CloudWatchConventions
import kinesis4cats.smithy4s.client.CloudWatchClient

/** Builds a [[org.typelevel.otel4s.metrics.MeterProvider MeterProvider]] that
  * exports producer metrics to CloudWatch via the GA `PutMetricData` API, using
  * the cross-platform smithy4s CloudWatch client. Works in all regions.
  */
object PutMetricDataMeterProvider {

  private val exportInterval = 60.seconds
  private val exportTimeout = 30.seconds

  def resource[F[_]](
      region: AwsRegion,
      httpClient: Client[F],
      cloudWatchNamespace: String =
        CloudWatchConventions.defaultCloudWatchNamespace,
      credentials: Client[F] => Resource[F, F[AwsCredentials]]
  )(implicit
      F: Async[F],
      C: Compression[F]
  ): Resource[F, MeterProvider[F]] = {
    implicit val askContext: Ask[F, Context] = Ask.const(Context.root)
    implicit val console: Console[F] = Console.make[F]
    for {
      sink <- CloudWatchClient.resource[F](httpClient, region, credentials)
      exporter = new PutMetricDataExporter[F](metrics =>
        metrics.flatMap(SdkMetricTranslation.translate) match {
          case Nil  => F.unit
          case data => sink.putMetricData(cloudWatchNamespace, data)
        }
      )
      reader <- MetricReader.periodic(exporter, exportInterval, exportTimeout)
      random <- Resource.eval(Random.scalaUtilRandom[F])
      meterProvider <- Resource.eval {
        implicit val r: Random[F] = random
        SdkMeterProvider.builder[F].registerMetricReader(reader).build
      }
    } yield meterProvider
  }
}
