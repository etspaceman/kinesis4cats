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
package syntax

import cats.effect.Async
import cats.effect.Resource
import org.typelevel.otel4s.metrics.MeterProvider
import org.typelevel.otel4s.oteljava.context.LocalContextProvider
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.regions.Region

import kinesis4cats.client.producer.KinesisProducer
import kinesis4cats.client.producer.fs2.FS2KinesisProducer
import kinesis4cats.producer.metrics.ProducerMetrics
import kinesis4cats.producer.metrics.cloudwatch.CloudWatchBackend
import kinesis4cats.producer.metrics.cloudwatch.CloudWatchConventions

/** Terminal `buildWithCloudWatch` extension methods that bundle a
  * CloudWatch-exporting `MeterProvider` with a producer's `Resource`. The
  * default backend is the GA `PutMetricData` API; `CloudWatchBackend.Otlp`
  * selects the public-preview native OTLP endpoint. Import
  * `kinesis4cats.client.producer.opentelemetry.syntax.cloudwatch._`.
  */
object cloudwatch extends CloudWatchSyntax

trait CloudWatchSyntax {
  implicit def toCloudWatchProducerBuilderOps[F[_]](
      builder: KinesisProducer.Builder[F]
  ): CloudWatchSyntax.ProducerBuilderOps[F] =
    new CloudWatchSyntax.ProducerBuilderOps(builder)

  implicit def toCloudWatchFS2ProducerBuilderOps[F[_]](
      builder: FS2KinesisProducer.Builder[F]
  ): CloudWatchSyntax.FS2ProducerBuilderOps[F] =
    new CloudWatchSyntax.FS2ProducerBuilderOps(builder)
}

object CloudWatchSyntax {

  private def meterProvider[F[_]](
      backend: CloudWatchBackend,
      region: Option[Region],
      cloudWatchNamespace: String,
      credentials: AwsCredentialsProvider
  )(implicit
      F: Async[F],
      L: LocalContextProvider[F]
  ): Resource[F, MeterProvider[F]] =
    backend match {
      case CloudWatchBackend.PutMetricData =>
        PutMetricDataMeterProvider
          .resource[F](region, cloudWatchNamespace, credentials)
      case CloudWatchBackend.Otlp =>
        CloudWatchMeterProvider.resource[F](region, credentials)
    }

  final class ProducerBuilderOps[F[_]](
      private val builder: KinesisProducer.Builder[F]
  ) extends AnyVal {
    def buildWithCloudWatch(
        region: Option[Region] = None,
        namespace: String = ProducerMetrics.defaultNamespace,
        cloudWatchNamespace: String =
          CloudWatchConventions.defaultCloudWatchNamespace,
        credentials: AwsCredentialsProvider =
          DefaultCredentialsProvider.builder().build(),
        backend: CloudWatchBackend = CloudWatchBackend.PutMetricData
    )(implicit
        F: Async[F],
        L: LocalContextProvider[F]
    ): Resource[F, KinesisProducer[F]] =
      CloudWatchSyntax
        .meterProvider[F](backend, region, cloudWatchNamespace, credentials)
        .flatMap(mp => builder.withMetrics(mp, namespace).build)
  }

  final class FS2ProducerBuilderOps[F[_]](
      private val builder: FS2KinesisProducer.Builder[F]
  ) extends AnyVal {
    def buildWithCloudWatch(
        region: Option[Region] = None,
        namespace: String = ProducerMetrics.defaultNamespace,
        cloudWatchNamespace: String =
          CloudWatchConventions.defaultCloudWatchNamespace,
        credentials: AwsCredentialsProvider =
          DefaultCredentialsProvider.builder().build(),
        backend: CloudWatchBackend = CloudWatchBackend.PutMetricData
    )(implicit
        F: Async[F],
        L: LocalContextProvider[F]
    ): Resource[F, FS2KinesisProducer[F]] =
      CloudWatchSyntax
        .meterProvider[F](backend, region, cloudWatchNamespace, credentials)
        .flatMap(mp => builder.withMetrics(mp, namespace).build)
  }
}
