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

import java.net.URI
import java.time.Duration
import java.util.function.{Supplier => JSupplier}
import java.util.{Map => JMap}

import cats.effect.Async
import cats.effect.Resource
import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporter
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.metrics.SdkMeterProvider
import io.opentelemetry.sdk.metrics.`export`.PeriodicMetricReader
import org.typelevel.otel4s.metrics.MeterProvider
import org.typelevel.otel4s.oteljava.OtelJava
import org.typelevel.otel4s.oteljava.context.LocalContextProvider
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.http.SdkHttpMethod
import software.amazon.awssdk.http.SdkHttpRequest
import software.amazon.awssdk.http.auth.aws.signer.AwsV4FamilyHttpSigner
import software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner
import software.amazon.awssdk.http.auth.spi.signer.SignRequest
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain

/** Builds a [[org.typelevel.otel4s.metrics.MeterProvider MeterProvider]] that
  * exports producer metrics to the CloudWatch native OTLP endpoint
  * (`https://monitoring.{region}.amazonaws.com/v1/metrics`), SigV4-signed via
  * the AWS SDK v2 `AwsV4HttpSigner`.
  *
  * Region and credentials default to the AWS SDK provider chains.
  *
  * '''Preview caveat:''' the CloudWatch OTLP endpoint is in public preview and
  * available only in a subset of AWS regions. Be aware of the currently active
  * regions for the feature before enabling it — see
  * [[https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/CloudWatch-OTLPEndpoint.html the CloudWatch OTLP endpoint documentation]].
  * Requests from unsupported regions fail at runtime (logged by the exporter,
  * never propagated to the produce path).
  */
object CloudWatchMeterProvider {

  private val serviceName = "monitoring"
  private val exportInterval = Duration.ofSeconds(60)
  private val signer = AwsV4HttpSigner.create()

  private def endpoint(region: Region): String =
    s"https://monitoring.${region.id()}.amazonaws.com/v1/metrics"

  /** SigV4-signs a synthetic request to the CloudWatch OTLP endpoint and
    * returns the resulting auth headers. Invoked by the OTLP exporter before
    * each export. Payload signing is disabled, so the request is signed over
    * the literal `UNSIGNED-PAYLOAD` content hash, which the CloudWatch preview
    * endpoint accepts (the body is not available at header-supplier time).
    */
  private[opentelemetry] def headerSupplier(
      credentials: AwsCredentialsProvider,
      region: Region
  ): JSupplier[JMap[String, String]] =
    new JSupplier[JMap[String, String]] {
      def get(): JMap[String, String] = {
        val unsigned = SdkHttpRequest
          .builder()
          .method(SdkHttpMethod.POST)
          .uri(URI.create(endpoint(region)))
          .build()
        val signRequest = SignRequest
          .builder(credentials.resolveCredentials())
          .request(unsigned)
          .putProperty(AwsV4HttpSigner.REGION_NAME, region.id())
          .putProperty(AwsV4FamilyHttpSigner.SERVICE_SIGNING_NAME, serviceName)
          .putProperty(
            AwsV4FamilyHttpSigner.PAYLOAD_SIGNING_ENABLED,
            java.lang.Boolean.FALSE
          )
          .build()
        val signed = signer.sign(signRequest)
        signed
          .request()
          .headers()
          .asScala
          .collect {
            case (k, v) if !v.isEmpty => k -> v.get(0)
          }
          .toMap
          .asJava
      }
    }

  def resource[F[_]](
      region: Option[Region] = None,
      credentials: AwsCredentialsProvider =
        DefaultCredentialsProvider.builder().build()
  )(implicit
      F: Async[F],
      L: LocalContextProvider[F]
  ): Resource[F, MeterProvider[F]] =
    for {
      resolvedRegion <- Resource.eval(
        region.fold(
          F.blocking(
            DefaultAwsRegionProviderChain.builder().build().getRegion()
          )
        )(F.pure)
      )
      // OtelJava.resource closes the OpenTelemetrySdk on release, which shuts
      // down (and flushes) the SdkMeterProvider/exporter.
      otelJava <- OtelJava.resource[F](
        F.blocking {
          val exporter = OtlpHttpMetricExporter
            .builder()
            .setEndpoint(endpoint(resolvedRegion))
            .setHeaders(headerSupplier(credentials, resolvedRegion))
            .build()
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
