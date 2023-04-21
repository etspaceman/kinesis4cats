/*
 * Copyright 2023-2023 etspaceman
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

package kinesis4cats.kpl
package localstack

import cats.effect.{Async, Resource}
import com.amazonaws.services.kinesis.producer.KinesisProducerConfiguration
import org.typelevel.log4cats.StructuredLogger

import kinesis4cats.localstack.LocalstackConfig
import kinesis4cats.localstack.TestStreamConfig
import kinesis4cats.localstack.aws.v1.{AwsClients, AwsCreds}

/** Helpers for constructing and leveraging the KPL with Localstack.
  */
object LocalstackKPLProducer {

  final case class Builder[F[_]] private (
      kplBuilder: KPLProducer.Builder[F],
      streamsToCreate: List[TestStreamConfig[F]],
      localstackConfig: LocalstackConfig
  )(implicit F: Async[F]) {
    def withConfig(config: KPLProducer.Config): Builder[F] =
      copy(kplBuilder = kplBuilder.withConfig(config))
    def withLogger(logger: StructuredLogger[F]) =
      copy(kplBuilder = kplBuilder.withLogger(logger))
    def withLogEncoders(encoders: KPLProducer.LogEncoders): Builder[F] =
      copy(kplBuilder = kplBuilder.withLogEncoders(encoders))
    def configure(
        f: KinesisProducerConfiguration => KinesisProducerConfiguration
    ) = copy(kplBuilder = kplBuilder.configure(f))
    def withStreamsToCreate(
        streamsToCreate: List[TestStreamConfig[F]]
    ): Builder[F] = copy(streamsToCreate = streamsToCreate)

    def build: Resource[F, KPLProducer[F]] = for {
      _ <- AwsClients.kinesisStreamResource(localstackConfig, streamsToCreate)
      producer <- kplBuilder.build
    } yield producer
  }

  object Builder {
    def default[F[_]](localstackConfig: LocalstackConfig)(implicit
        F: Async[F]
    ): Builder[F] =
      Builder(
        KPLProducer.Builder
          .default[F]
          .configure(_ => kplConfig(localstackConfig)),
        Nil,
        localstackConfig
      )

    @annotation.unused
    def unapply[F[_]](builder: Builder[F]): Unit = ()
  }

  /** [[https://github.com/awslabs/amazon-kinesis-producer/blob/master/java/amazon-kinesis-producer/src/main/java/com/amazonaws/services/kinesis/producer/KinesisProducerConfiguration.java KinesisProducerConfiguration]]
    * configuration compliant with Localstack
    *
    * @param config
    *   [[kinesis4cats.localstack.LocalstackConfig LocalstackConfig]]
    * @return
    *   [[https://github.com/awslabs/amazon-kinesis-producer/blob/master/java/amazon-kinesis-producer/src/main/java/com/amazonaws/services/kinesis/producer/KinesisProducerConfiguration.java KinesisProducerConfiguration]]
    */
  private def kplConfig(
      config: LocalstackConfig
  ): KinesisProducerConfiguration =
    new KinesisProducerConfiguration()
      .setVerifyCertificate(false)
      .setKinesisEndpoint(config.kinesisHost)
      .setCloudwatchEndpoint(config.cloudwatchHost)
      .setStsEndpoint(config.stsHost)
      .setCredentialsProvider(AwsCreds.LocalCredsProvider)
      .setKinesisPort(config.kinesisPort.toLong)
      .setCloudwatchPort(config.cloudwatchPort.toLong)
      .setStsPort(config.stsPort.toLong)
      .setMetricsLevel("none")
      .setLogLevel("warning")
      .setRegion(config.region.name)
}
