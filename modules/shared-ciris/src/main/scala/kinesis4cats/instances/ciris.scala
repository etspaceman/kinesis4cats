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

import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.jdk.CollectionConverters._
import scala.util.Try

import java.time.Instant

import _root_.ciris.{ConfigDecoder, ConfigError}
import cats.syntax.all._

import kinesis4cats.compat.DurationConverters._
import kinesis4cats.models.{AwsRegion, ConsumerArn}
import kinesis4cats.syntax.string._

/** Common [[https://cir.is/docs/configurations#decoders ConfigDecoder]]
  * instances
  */
object ciris {
  implicit val awsRegionConfigDecoder: ConfigDecoder[String, AwsRegion] =
    ConfigDecoder[String, String].mapEither { case (_, x) =>
      Either.fromOption(
        AwsRegion.values.find(r => x === r.name),
        ConfigError(s"$x is not a known region")
      )
    }

  implicit val consumerArnConfigDecoder: ConfigDecoder[String, ConsumerArn] =
    ConfigDecoder[String].mapEither { case (_, value) =>
      ConsumerArn.fromArn(value).leftMap(e => ConfigError(e))
    }

  implicit val durationConfigDecoder: ConfigDecoder[String, Duration] =
    ConfigDecoder[String].mapEither { case (_, value) =>
      Try(Duration(value)).toEither.leftMap(e =>
        ConfigError(s"Could not decode duration $value: ${e}")
      )
    }
  implicit val finiteDurationDecoder: ConfigDecoder[String, FiniteDuration] =
    durationConfigDecoder.map(x => FiniteDuration(x.length, x.unit))

  implicit val javaDurationDecoder: ConfigDecoder[String, java.time.Duration] =
    finiteDurationDecoder.map(_.toJava)

  implicit def seqDecoder[A](implicit
      CDA: ConfigDecoder[String, A]
  ): ConfigDecoder[String, Seq[A]] = ConfigDecoder[String].mapEither {
    case (configKey, value) =>
      value.asList.traverse(x => CDA.decode(configKey, x))
  }

  implicit def listDecoder[A](implicit
      CDA: ConfigDecoder[String, A]
  ): ConfigDecoder[String, List[A]] = seqDecoder[A].map(_.toList)

  implicit def setDecoder[A](implicit
      CDA: ConfigDecoder[String, A]
  ): ConfigDecoder[String, Set[A]] = seqDecoder[A].map(_.toSet)

  implicit def javaSetDecoder[A](implicit
      CDA: ConfigDecoder[String, A]
  ): ConfigDecoder[String, java.util.Set[A]] = setDecoder[A].map(_.asJava)

  implicit def mapConfigDecoder[A, B](implicit
      CDA: ConfigDecoder[String, A],
      CDB: ConfigDecoder[String, B]
  ): ConfigDecoder[String, Map[A, B]] = ConfigDecoder[String].mapEither {
    case (configKey, x) =>
      x.asMap
        .leftMap(ConfigError(_))
        .flatMap(
          _.toList
            .traverse { case (keyStr, valueStr) =>
              for {
                key <- CDA.decode(configKey, keyStr)
                value <- CDB.decode(configKey, valueStr)
              } yield key -> value
            }
            .map(_.toMap)
        )
  }

  implicit def javaMapConfigDecoder[A, B](implicit
      CDA: ConfigDecoder[String, A],
      CDB: ConfigDecoder[String, B]
  ): ConfigDecoder[String, java.util.Map[A, B]] =
    mapConfigDecoder[A, B].map(_.asJava)

  implicit val instantConfigDecoder: ConfigDecoder[String, Instant] =
    ConfigDecoder[String].mapEither { case (_, value) =>
      Try(Instant.parse(value)).toEither.leftMap(e =>
        ConfigError(s"Unable to parse timestamp value $value: ${e.getMessage}")
      )
    }

  implicit val javaUtilDateConfigDecoder
      : ConfigDecoder[String, java.util.Date] =
    ConfigDecoder[String].mapEither { case (configKey, value) =>
      instantConfigDecoder
        .decode(configKey, value)
        .map(java.util.Date.from)
    }

  implicit val javaIntegerConfigDecoder
      : ConfigDecoder[String, java.lang.Integer] =
    ConfigDecoder[String, Int].map(java.lang.Integer.valueOf)

  implicit val javaLongConfigDecoder: ConfigDecoder[String, java.lang.Long] =
    ConfigDecoder[String, Long].map(java.lang.Long.valueOf)
}
