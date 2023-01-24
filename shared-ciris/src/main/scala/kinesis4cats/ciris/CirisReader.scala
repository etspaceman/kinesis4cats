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

package kinesis4cats.ciris

import ciris._

object CirisReader {

  def readEnvString(
      parts: List[String],
      prefix: Option[String] = None
  ): ConfigValue[Effect, String] = {
    env(
      (prefix.toList ++ parts).map(_.toUpperCase()).mkString("_")
    )

  }

  def readPropString(
      parts: List[String],
      prefix: Option[String] = None
  ): ConfigValue[Effect, String] = prop(
    (prefix.toList ++ parts).map(_.toLowerCase()).mkString(".")
  )

  def readString(
      parts: List[String],
      prefix: Option[String] = None
  ): ConfigValue[Effect, String] =
    readEnvString(parts, prefix).or(readPropString(parts, prefix))

  def read[A](
      parts: List[String],
      prefix: Option[String] = None
  )(implicit
      CD: ConfigDecoder[String, A]
  ): ConfigValue[Effect, A] =
    readString(parts, prefix).as[A]

  def readDefaulted[A](
      parts: List[String],
      default: A,
      prefix: Option[String] = None
  )(implicit
      CD: ConfigDecoder[String, A]
  ): ConfigValue[Effect, A] =
    read(parts, prefix).default(default)

  def readOptional[A](
      parts: List[String],
      prefix: Option[String] = None
  )(implicit
      CD: ConfigDecoder[String, A]
  ): ConfigValue[Effect, Option[A]] =
    read(parts, prefix).option
}
