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

final case class LocalstackConfig(
    servicePort: Int,
    protocol: String,
    host: String,
    region: AwsRegion
) {
  val endpoint: String = s"$protocol://$host:$servicePort"
  val endpointUri: URI = URI.create(endpoint)
}

object LocalstackConfig {
  def read(
      prefix: Option[String] = None
  ): ConfigValue[Effect, LocalstackConfig] = for {
    servicePort <- CirisReader.readDefaulted(
      List("localstack", "service", "port"),
      4566,
      prefix
    )
    protocol <- CirisReader.readDefaulted(
      List("localstack", "protocol"),
      "https"
    )
    region <- CirisReader.readDefaulted[AwsRegion](
      List("aws", "region"),
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
