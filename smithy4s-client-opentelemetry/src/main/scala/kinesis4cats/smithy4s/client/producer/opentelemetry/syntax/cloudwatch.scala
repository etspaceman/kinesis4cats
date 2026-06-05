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
package syntax

import _root_.fs2.compression.Compression
import _root_.fs2.io.file.Files
import cats.effect.Async
import cats.effect.Resource
import org.http4s.client.Client
import smithy4s.aws.AwsCredentialsProvider
import smithy4s.aws.kernel.AwsCredentials
import smithy4s.aws.kernel.AwsRegion

import kinesis4cats.producer.metrics.ProducerMetrics
import kinesis4cats.smithy4s.client.producer.KinesisProducer
import kinesis4cats.smithy4s.client.producer.fs2.FS2KinesisProducer

/** Terminal `buildWithCloudWatch` extension methods that bundle a
  * CloudWatch-exporting [[CloudWatchMeterProvider]] with a producer's
  * `Resource`. `region` and `httpClient` default to the builder's own fields.
  * Import `kinesis4cats.smithy4s.client.producer.opentelemetry.syntax.cloudwatch._`.
  */
object cloudwatch extends CloudWatchSyntax

trait CloudWatchSyntax {
  implicit def toCloudWatchProducerBuilderOps[F[_]: Async: Compression: Files](
      builder: KinesisProducer.Builder[F]
  ): CloudWatchSyntax.ProducerBuilderOps[F] =
    new CloudWatchSyntax.ProducerBuilderOps(builder)

  implicit def toCloudWatchFS2ProducerBuilderOps[
      F[_]: Async: Compression: Files
  ](
      builder: FS2KinesisProducer.Builder[F]
  ): CloudWatchSyntax.FS2ProducerBuilderOps[F] =
    new CloudWatchSyntax.FS2ProducerBuilderOps(builder)
}

object CloudWatchSyntax {

  private def defaultCreds[F[_]: Async: Files]
      : Client[F] => Resource[F, F[AwsCredentials]] =
    backend => AwsCredentialsProvider.default(backend)

  final class ProducerBuilderOps[F[_]: Async: Compression: Files](
      private val builder: KinesisProducer.Builder[F]
  ) {
    def buildWithCloudWatch(
        region: AwsRegion = builder.region,
        httpClient: Client[F] = builder.client,
        namespace: String = ProducerMetrics.defaultNamespace,
        credentials: Client[F] => Resource[F, F[AwsCredentials]] =
          defaultCreds[F]
    ): Resource[F, KinesisProducer[F]] =
      CloudWatchMeterProvider
        .resource[F](region, httpClient, credentials)
        .flatMap(mp => builder.withMetrics(mp, namespace).build)
  }

  final class FS2ProducerBuilderOps[F[_]: Async: Compression: Files](
      private val builder: FS2KinesisProducer.Builder[F]
  ) {
    def buildWithCloudWatch(
        region: AwsRegion = builder.region,
        httpClient: Client[F] = builder.client,
        namespace: String = ProducerMetrics.defaultNamespace,
        credentials: Client[F] => Resource[F, F[AwsCredentials]] =
          defaultCreds[F]
    ): Resource[F, FS2KinesisProducer[F]] =
      CloudWatchMeterProvider
        .resource[F](region, httpClient, credentials)
        .flatMap(mp => builder.withMetrics(mp, namespace).build)
  }
}
