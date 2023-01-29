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

import cats.effect.{Async, Resource}
import ciris._

import kinesis4cats.ciris.CirisReader

/** Configuration loading for
  * [[https://docs.localstack.cloud/references/configuration/ Localstack]]
  *
  * @param servicePort
  *   Port for Localstack. Default is 4566
  * @param protocol
  *   Protocol for Localstack. Default is https
  * @param host
  *   Host for Localstack. Default is localhost
  * @param region
  *   Default region for Localstack. Default is us-east-1
  */
final case class LocalstackConfig(
    servicePort: Int,
    protocol: Protocol,
    host: String,
    region: AwsRegion
) {
  val endpoint: String = s"${protocol.name}://$host:$servicePort"
  val endpointUri: URI = URI.create(endpoint)
}

object LocalstackConfig {
  def read(
      prefix: Option[String] = None
  ): ConfigValue[Effect, LocalstackConfig] = for {
    servicePort <- CirisReader.readDefaulted(
      List("localstack", "port"),
      4566,
      prefix
    )
    protocol <- CirisReader.readDefaulted[Protocol](
      List("localstack", "protocol"),
      Protocol.Https
    )
    region <- CirisReader.readDefaulted[AwsRegion](
      List("localstack", "aws", "region"),
      AwsRegion.US_EAST_1,
      prefix
    )
    host <- CirisReader.readDefaulted(
      List("localstack", "host"),
      "localhost",
      prefix
    )
  } yield LocalstackConfig(servicePort, protocol, host, region)

  def load[F[_]: Async](prefix: Option[String] = None): F[LocalstackConfig] =
    read(prefix).load[F]

  def resource[F[_]: Async](
      prefix: Option[String] = None
  ): Resource[F, LocalstackConfig] = read(prefix).resource[F]
}
