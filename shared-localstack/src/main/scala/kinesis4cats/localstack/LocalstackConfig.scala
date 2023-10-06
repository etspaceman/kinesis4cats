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

package kinesis4cats.localstack

import java.net.URI

import cats.Show
import cats.effect.{Async, Resource}
import ciris._
import io.circe.Encoder

import kinesis4cats.ShowBuilder
import kinesis4cats.ciris.CirisReader
import kinesis4cats.instances.ciris._
import kinesis4cats.logging.LogEncoder
import kinesis4cats.models.AwsRegion

/** Configuration loading for
  * [[https://docs.localstack.cloud/references/configuration/ Localstack]]
  *
  * @param cloudwatchPort
  *   Port for Cloudwatch in Localstack. Default is 4566
  * @param cloudwatchProtocol
  *   Protocol for Cloudwatch in Localstack. Default is https
  * @param cloudwatchHost
  *   Host for Cloudwatch in Localstack. Default is localhost
  * @param kinesisPort
  *   Port for Kinesis in Localstack. Default is 4566
  * @param kinesisProtocol
  *   Protocol for Kinesis in Localstack. Default is https
  * @param kinesisHost
  *   Host for Kinesis in Localstack. Default is localhost
  * @param dynamoPort
  *   Port for Dynamo in Localstack. Default is 4566
  * @param dynamoProtocol
  *   Protocol for Dynamo in Localstack. Default is https
  * @param dynamoHost
  *   Host for Dynamo in Localstack. Default is localhost
  * @param region
  *   Default region for Localstack. Default is us-east-1
  */
final case class LocalstackConfig(
    cloudwatchPort: Int,
    cloudwatchProtocol: Protocol,
    cloudwatchHost: String,
    kinesisPort: Int,
    kinesisProtocol: Protocol,
    kinesisHost: String,
    dynamoPort: Int,
    dynamoProtocol: Protocol,
    dynamoHost: String,
    stsPort: Int,
    stsProtocol: Protocol,
    stsHost: String,
    region: AwsRegion
) {
  val cloudwatchEndpoint: String =
    s"${cloudwatchProtocol.name}://$cloudwatchHost:$cloudwatchPort"
  val cloudwatchEndpointUri: URI = URI.create(cloudwatchEndpoint)
  val kinesisEndpoint: String =
    s"${kinesisProtocol.name}://$kinesisHost:$kinesisPort"
  val kinesisEndpointUri: URI = URI.create(kinesisEndpoint)
  val dynamoEndpoint: String =
    s"${dynamoProtocol.name}://$dynamoHost:$dynamoPort"
  val dynamoEndpointUri: URI = URI.create(dynamoEndpoint)
  val stsEndpoint: String =
    s"${stsProtocol.name}://$stsHost:$stsPort"
  val stsEndpointUri: URI = URI.create(stsEndpoint)
}

object LocalstackConfig {
  implicit val localstackConfigShow: Show[LocalstackConfig] = x =>
    ShowBuilder("LocalstackConfig")
      .add("cloudwatchPort", x.cloudwatchPort)
      .add("cloudwatchProtocol", x.cloudwatchProtocol.name)
      .add("cloudwatchHost", x.cloudwatchHost)
      .add("cloudwatchEndpoint", x.cloudwatchEndpoint)
      .add("kinesisPort", x.kinesisPort)
      .add("kinesisProtocol", x.kinesisProtocol.name)
      .add("kinesisHost", x.kinesisHost)
      .add("kinesisEndpoint", x.kinesisEndpoint)
      .add("dynamoPort", x.dynamoPort)
      .add("dynamoProtocol", x.dynamoProtocol.name)
      .add("dynamoHost", x.dynamoHost)
      .add("dynamoEndpoint", x.dynamoEndpoint)
      .add("stsProtocol", x.stsProtocol.name)
      .add("stsHost", x.stsHost)
      .add("stsEndpoint", x.stsEndpoint)
      .add("region", x.region.name)
      .build

  implicit val localstackCirceEncoder: Encoder[LocalstackConfig] =
    Encoder.forProduct17(
      "cloudwatchPort",
      "cloudwatchProtocol",
      "cloudwatchHost",
      "cloudwatchEndpoint",
      "kinesisPort",
      "kinesisProtocol",
      "kinesisHost",
      "kinesisEndpoint",
      "dynamoPort",
      "dynamoProtocol",
      "dynamoHost",
      "dynamoEndpoint",
      "stsPort",
      "stsProtocol",
      "stsHost",
      "stsEndpoint",
      "region"
    )(x =>
      (
        x.cloudwatchPort,
        x.cloudwatchProtocol.name,
        x.cloudwatchHost,
        x.cloudwatchEndpoint,
        x.kinesisPort,
        x.kinesisProtocol.name,
        x.kinesisHost,
        x.kinesisEndpoint,
        x.dynamoPort,
        x.dynamoProtocol.name,
        x.dynamoHost,
        x.dynamoEndpoint,
        x.stsPort,
        x.stsProtocol.name,
        x.stsHost,
        x.stsEndpoint,
        x.region.name
      )
    )

  def defaultPort(
      prefix: Option[String] = None
  ): ConfigValue[Effect, Int] =
    CirisReader.readDefaulted[Int](
      List("localstack", "port"),
      4566,
      prefix
    )

  def defaultProtocol(
      prefix: Option[String] = None
  ): ConfigValue[Effect, Protocol] =
    CirisReader.readDefaulted[Protocol](
      List("localstack", "protocol"),
      Protocol.Https,
      prefix
    )

  def defaultHost(prefix: Option[String] = None): ConfigValue[Effect, String] =
    CirisReader.readDefaulted(
      List("localstack", "host"),
      "127.0.0.1",
      prefix
    )

  def read(
      prefix: Option[String] = None
  ): ConfigValue[Effect, LocalstackConfig] = for {
    cloudwatchPort <- CirisReader
      .read[Int](
        List("localstack", "cloudwatch", "port"),
        prefix
      )
      .or(defaultPort(prefix))
    cloudwatchProtocol <- CirisReader
      .read[Protocol](
        List("localstack", "cloudwatch", "protocol"),
        prefix
      )
      .or(defaultProtocol(prefix))
    cloudwatchHost <- CirisReader
      .read[String](
        List("localstack", "cloudwatch", "host"),
        prefix
      )
      .or(defaultHost(prefix))
    kinesisPort <- CirisReader
      .read[Int](
        List("localstack", "kinesis", "port"),
        prefix
      )
      .or(defaultPort(prefix))
    kinesisProtocol <- CirisReader
      .read[Protocol](
        List("localstack", "kinesis", "protocol"),
        prefix
      )
      .or(defaultProtocol(prefix))
    kinesisHost <- CirisReader
      .read[String](
        List("localstack", "kinesis", "host"),
        prefix
      )
      .or(defaultHost(prefix))
    dynamoPort <- CirisReader
      .read[Int](
        List("localstack", "dynamo", "port"),
        prefix
      )
      .or(defaultPort(prefix))
    dynamoProtocol <- CirisReader
      .read[Protocol](
        List("localstack", "dynamo", "protocol"),
        prefix
      )
      .or(defaultProtocol(prefix))
    dynamoHost <- CirisReader
      .read[String](
        List("localstack", "dynamo", "host"),
        prefix
      )
      .or(defaultHost(prefix))
    stsPort <- CirisReader
      .read[Int](
        List("localstack", "sts", "port"),
        prefix
      )
      .or(defaultPort(prefix))
    stsProtocol <- CirisReader
      .read[Protocol](
        List("localstack", "sts", "protocol"),
        prefix
      )
      .or(defaultProtocol(prefix))
    stsHost <- CirisReader
      .read[String](
        List("localstack", "sts", "host"),
        prefix
      )
      .or(defaultHost(prefix))
    region <- CirisReader.readDefaulted[AwsRegion](
      List("localstack", "aws", "region"),
      AwsRegion.US_EAST_1,
      prefix
    )
  } yield LocalstackConfig(
    cloudwatchPort,
    cloudwatchProtocol,
    cloudwatchHost,
    kinesisPort,
    kinesisProtocol,
    kinesisHost,
    dynamoPort,
    dynamoProtocol,
    dynamoHost,
    stsPort,
    stsProtocol,
    stsHost,
    region
  )

  def load[F[_]: Async](prefix: Option[String] = None): F[LocalstackConfig] =
    read(prefix).load[F]

  def resource[F[_]: Async](
      prefix: Option[String] = None
  ): Resource[F, LocalstackConfig] = read(prefix).resource[F]

  final class LogEncoders(implicit
      val localstackConfigLogEncoder: LogEncoder[LocalstackConfig]
  )

  object LogEncoders {
    val show = {
      import kinesis4cats.logging.instances.show._

      new LogEncoders()
    }

    val circe = {
      import kinesis4cats.logging.instances.circe._

      new LogEncoders()
    }
  }
}
