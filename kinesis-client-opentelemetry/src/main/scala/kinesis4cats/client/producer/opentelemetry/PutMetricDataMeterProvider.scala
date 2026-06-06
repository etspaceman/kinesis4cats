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

import java.time.Duration

import cats.effect.Async
import cats.effect.Resource
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.metrics.SdkMeterProvider
import io.opentelemetry.sdk.metrics.`export`.PeriodicMetricReader
import org.typelevel.otel4s.metrics.MeterProvider
import org.typelevel.otel4s.oteljava.OtelJava
import org.typelevel.otel4s.oteljava.context.LocalContextProvider
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient

import kinesis4cats.producer.metrics.cloudwatch.CloudWatchConventions

/** Builds a [[org.typelevel.otel4s.metrics.MeterProvider MeterProvider]] that
  * exports producer metrics to CloudWatch via the GA `PutMetricData` API. Works
  * in all regions. Region and credentials default to the AWS SDK provider
  * chains.
  *
  * For non-standard endpoints (Localstack, VPC/FIPS, a custom TLS trust store)
  * build your own `CloudWatchAsyncClient` and use [[fromClientResource]].
  */
object PutMetricDataMeterProvider {

  private val exportInterval = Duration.ofSeconds(60)

  def resource[F[_]](
      region: Option[Region] = None,
      cloudWatchNamespace: String =
        CloudWatchConventions.defaultCloudWatchNamespace,
      credentials: AwsCredentialsProvider =
        DefaultCredentialsProvider.builder().build()
  )(implicit
      F: Async[F],
      L: LocalContextProvider[F]
  ): Resource[F, MeterProvider[F]] =
    fromClientResource(
      cloudWatchNamespace,
      Resource
        .eval(
          region.fold(
            F.blocking(
              DefaultAwsRegionProviderChain.builder().build().getRegion()
            )
          )(F.pure)
        )
        .flatMap { resolvedRegion =>
          Resource.fromAutoCloseable(
            F.delay(
              CloudWatchAsyncClient
                .builder()
                .region(resolvedRegion)
                .credentialsProvider(credentials)
                .build()
            )
          )
        }
    )

  /** Builds a `MeterProvider` over a caller-supplied `CloudWatchAsyncClient`
    * resource. Use this for Localstack, VPC/FIPS endpoints, or any custom
    * client (HTTP client, TLS, endpoint override). The client's lifecycle is
    * owned by the supplied `Resource`.
    */
  def fromClientResource[F[_]](
      cloudWatchNamespace: String,
      clientResource: Resource[F, CloudWatchAsyncClient]
  )(implicit
      F: Async[F],
      L: LocalContextProvider[F]
  ): Resource[F, MeterProvider[F]] =
    for {
      client <- clientResource
      // OtelJava.resource closes the OpenTelemetrySdk on release, which shuts
      // down (and flushes) the SdkMeterProvider/exporter.
      otelJava <- OtelJava.resource[F](
        F.blocking {
          val exporter =
            new PutMetricDataExporter(client, cloudWatchNamespace)
          val reader = PeriodicMetricReader
            .builder(exporter)
            .setInterval(exportInterval)
            .build()
          val sdkMeterProvider =
            SdkMeterProvider.builder().registerMetricReader(reader).build()
          OpenTelemetrySdk.builder().setMeterProvider(sdkMeterProvider).build()
        }
      )
    } yield otelJava.meterProvider
}
