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

import scala.collection.immutable.SortedMap
import scala.concurrent.duration._

import cats.effect.Async
import cats.effect.Resource
import cats.effect.std.Console
import cats.effect.std.Random
import cats.mtl.Ask
import cats.syntax.all._
import fs2.compression.Compression
import fs2.io.net.Network
import org.http4s.Header
import org.http4s.Request
import org.http4s.Uri
import org.http4s.client.Client
import org.typelevel.ci.CIString
import org.typelevel.otel4s.metrics.MeterProvider
import org.typelevel.otel4s.sdk.context.Context
import org.typelevel.otel4s.sdk.exporter.otlp.metrics.OtlpMetricExporter
import org.typelevel.otel4s.sdk.metrics.SdkMeterProvider
import org.typelevel.otel4s.sdk.metrics.exporter.MetricReader
import smithy4s.aws.kernel.AwsCredentials
import smithy4s.aws.kernel.AwsCrypto
import smithy4s.aws.kernel.AwsRegion
import smithy4s.aws.kernel.Timestamp

/** Builds a [[org.typelevel.otel4s.metrics.MeterProvider MeterProvider]] that
  * exports producer metrics to the CloudWatch native OTLP endpoint
  * (`https://monitoring.{region}.amazonaws.com/v1/metrics`), SigV4-signed by
  * wrapping the supplied http4s [[org.http4s.client.Client Client]].
  *
  * Signing is a small Signature-Version-4 implementation built on the
  * cross-platform `AwsCrypto` primitives (the same HMAC/SHA-256 routines
  * smithy4s-aws uses internally), so it works on JVM, JS, and Native. Requests
  * are signed over the literal `UNSIGNED-PAYLOAD` content hash, which the
  * CloudWatch preview endpoint accepts.
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
  private val unsignedPayload = "UNSIGNED-PAYLOAD"
  private val algorithm = "AWS4-HMAC-SHA256"
  private val exportInterval = 60.seconds
  private val exportTimeout = 30.seconds

  private def host(region: AwsRegion): String =
    s"monitoring.${region.value}.amazonaws.com"

  private def endpoint(region: AwsRegion): Uri =
    Uri.unsafeFromString(s"https://${host(region)}/v1/metrics")

  /** SigV4-signs a single request for the `monitoring` service over the
    * `UNSIGNED-PAYLOAD` content hash, returning the request with the
    * `Authorization` and `X-Amz-*` headers attached.
    */
  private def signRequest[F[_]](
      request: Request[F],
      region: AwsRegion,
      credentials: AwsCredentials,
      timestamp: Timestamp
  ): Request[F] = {
    import AwsCrypto._

    val amzDate = timestamp.conciseDateTime
    val dateStamp = timestamp.conciseDate
    val regionStr = region.value
    val canonicalUri = request.uri.path.renderString

    val signedHeaders: SortedMap[String, String] =
      SortedMap(
        "host" -> host(region),
        "x-amz-content-sha256" -> unsignedPayload,
        "x-amz-date" -> amzDate
      ) ++ credentials.sessionToken.map("x-amz-security-token" -> _)

    val signedHeaderNames = signedHeaders.keys.mkString(";")
    val canonicalHeaders =
      signedHeaders.map { case (k, v) => s"$k:${v.trim}\n" }.mkString
    val canonicalRequest =
      s"POST\n$canonicalUri\n\n$canonicalHeaders\n$signedHeaderNames\n$unsignedPayload"

    val credentialScope =
      s"$dateStamp/$regionStr/$serviceName/aws4_request"
    val stringToSign =
      s"$algorithm\n$amzDate\n$credentialScope\n${sha256HexDigest(canonicalRequest)}"

    val signingKey = {
      val kDate =
        hmacSha256(
          dateStamp,
          binaryFromString("AWS4" + credentials.secretAccessKey)
        )
      val kRegion = hmacSha256(regionStr, kDate)
      val kService = hmacSha256(serviceName, kRegion)
      hmacSha256("aws4_request", kService)
    }
    val signature = toHexString(hmacSha256(stringToSign, signingKey))

    val authorization =
      s"$algorithm Credential=${credentials.accessKeyId}/$credentialScope, " +
        s"SignedHeaders=$signedHeaderNames, Signature=$signature"

    val authHeaders: List[Header.ToRaw] = List[Header.ToRaw](
      Header.Raw(CIString("Authorization"), authorization),
      Header.Raw(CIString("X-Amz-Date"), amzDate),
      Header.Raw(CIString("X-Amz-Content-Sha256"), unsignedPayload)
    ) ++ credentials.sessionToken.map(token =>
      Header.Raw(CIString("X-Amz-Security-Token"), token)
    )

    request.putHeaders(authHeaders: _*)
  }

  /** Wraps `httpClient` so every outbound request is SigV4-signed for the
    * `monitoring` service.
    */
  private[opentelemetry] def signedClient[F[_]](
      httpClient: Client[F],
      region: AwsRegion,
      credentials: F[AwsCredentials]
  )(implicit F: Async[F]): Client[F] =
    Client[F] { request =>
      Resource
        .eval(
          (F.realTime, credentials).mapN { (now, creds) =>
            signRequest(
              request,
              region,
              creds,
              Timestamp.fromEpochMilli(now.toMillis)
            )
          }
        )
        .flatMap(httpClient.run)
    }

  def resource[F[_]](
      region: AwsRegion,
      httpClient: Client[F],
      credentials: Client[F] => Resource[F, F[AwsCredentials]]
  )(implicit
      F: Async[F],
      C: Compression[F]
  ): Resource[F, MeterProvider[F]] = {
    implicit val askContext: Ask[F, Context] = Ask.const(Context.root)
    implicit val console: Console[F] = Console.make[F]
    implicit val network: Network[F] = Network.forAsync[F]
    for {
      credsF <- credentials(httpClient)
      signed = signedClient(httpClient, region, credsF)
      exporter <- OtlpMetricExporter
        .builder[F]
        .withEndpoint(endpoint(region))
        .withClient(signed)
        .build
      reader <- MetricReader.periodic(
        exporter,
        exportInterval,
        exportTimeout
      )
      random <- Resource.eval(Random.scalaUtilRandom[F])
      meterProvider <- Resource.eval {
        implicit val r: Random[F] = random
        SdkMeterProvider.builder[F].registerMetricReader(reader).build
      }
    } yield meterProvider
  }
}
