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

package kinesis4cats.instances

import scala.concurrent.duration.Duration
import scala.jdk.CollectionConverters._
import scala.util.Try

import _root_.ciris.{ConfigDecoder, ConfigError}
import cats.syntax.all._

/** Common [[https://cir.is/docs/configurations#decoders ConfigDecoder]]
  * instances
  */
object ciris {
  implicit val durationConfigDecoder: ConfigDecoder[String, Duration] =
    ConfigDecoder[String].mapEither { case (_, value) =>
      Try(Duration(value)).toEither.leftMap(e =>
        ConfigError(s"Could not decode duration $value: ${e}")
      )
    }
  implicit def mapConfigDecoder[A, B](implicit
      CDA: ConfigDecoder[String, A],
      CDB: ConfigDecoder[String, B]
  ): ConfigDecoder[String, Map[A, B]] = ConfigDecoder[String].mapEither {
    case (configKey, x) =>
      x.split(",")
        .toList
        .traverse { kv =>
          kv.split(":").toList match {
            case keyStr :: valueStr :: Nil =>
              for {
                key <- CDA.decode(configKey, keyStr)
                value <- CDB.decode(configKey, valueStr)
              } yield key -> value
            case y =>
              Left(
                ConfigError(
                  s"Could not decode Map entry. Expected key:value, got $y. Full config value: $x"
                )
              )
          }
        }
        .map(_.toMap)
  }

  implicit def javaMapConfigDecoder[A, B](implicit
      CDA: ConfigDecoder[String, A],
      CDB: ConfigDecoder[String, B]
  ): ConfigDecoder[String, java.util.Map[A, B]] =
    mapConfigDecoder[A, B].map(_.asJava)
}
