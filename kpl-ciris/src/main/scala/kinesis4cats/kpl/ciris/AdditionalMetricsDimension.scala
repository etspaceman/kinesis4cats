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

package kinesis4cats.kpl.ciris

import ciris._

final case class AdditionalMetricsDimension(
    key: String,
    value: String,
    granularity: AdditionalMetricsDimension.Granularity
)

object AdditionalMetricsDimension {
  implicit val additionalMetricsDimensionConfigDecoder
      : ConfigDecoder[String, AdditionalMetricsDimension] =
    ConfigDecoder[String].mapEither { case (configKey, value) =>
      value.split(":").toList match {
        case key :: value :: granularity :: Nil =>
          ConfigDecoder[String, Granularity]
            .decode(configKey, granularity)
            .map { x =>
              AdditionalMetricsDimension(key, value, x)
            }
        case _ =>
          Left(
            ConfigError(
              "Error parsing additional metric. Must be a colon separated string in the structure of key:value:granularity"
            )
          )
      }
    }

  sealed abstract class Granularity(val value: String)
  object Granularity {
    case object Global extends Granularity("global")
    case object Stream extends Granularity("stream")
    case object Shard extends Granularity("shard")

    implicit val metricsGranularityConfigReader
        : ConfigDecoder[String, Granularity] = ConfigDecoder[String].mapEither {
      case (_, value) =>
        value match {
          case "global" => Right(Global)
          case "stream" => Right(Stream)
          case "shard"  => Right(Shard)
          case _ =>
            Left(
              ConfigError(
                s"Unrecognized Metrics Granularity value $value. Acceptable values are global, stream and shard"
              )
            )
        }

    }
  }
}
