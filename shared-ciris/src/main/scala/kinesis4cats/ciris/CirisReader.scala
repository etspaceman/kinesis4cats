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

/** Convenient methods for constructing configuration via
  * [[https://cir.is/ Ciris]]
  */
private[kinesis4cats] object CirisReader {

  /** Constructs an environment variable string reader given parts and an
    * optional prefix.
    *
    * @param parts
    *   List of string parts. List("one", "two", "three") becomes ONE_TWO_THREE
    * @param prefix
    *   Optional prefix. Default to None. List("one", "two", "three") with a
    *   prefix of "zero" becomes ZERO_ONE_TWO_THREE
    * @return
    *   [[https://cir.is/api/ciris/ConfigValue.html ConfigValue]] for the config
    */
  def readEnvString(
      parts: List[String],
      prefix: Option[String] = None
  ): ConfigValue[Effect, String] =
    env(
      (prefix.toList ++ parts).map(_.toUpperCase()).mkString("_")
    )

  /** Constructs a system property string reader given parts and an optional
    * prefix.
    *
    * @param parts
    *   List of string parts. List("one", "two", "three") becomes one.two.three
    * @param prefix
    *   Optional prefix. Default to None. List("one", "two", "three") with a
    *   prefix of "zero" becomes zero.one.two.three
    * @return
    *   [[https://cir.is/api/ciris/ConfigValue.html ConfigValue]] for the config
    */
  def readPropString(
      parts: List[String],
      prefix: Option[String] = None
  ): ConfigValue[Effect, String] = prop(
    (prefix.toList ++ parts).map(_.toLowerCase()).mkString(".")
  )

  /** Constructs an environment variable and system property string reader given
    * parts and an optional prefix.
    *
    * @param parts
    *   List of string parts. List("one", "two", "three") becomes one.two.three
    *   for system properties, and ONE_TWO_THREE for environment variables
    * @param prefix
    *   Optional prefix. Default to None. List("one", "two", "three") with a
    *   prefix of "zero" becomes zero.one.two.three for system properties, and
    *   ZERO_ONE_TWO_THREE for environment variables
    * @return
    *   [[https://cir.is/api/ciris/ConfigValue.html ConfigValue]] for the config
    */
  def readString(
      parts: List[String],
      prefix: Option[String] = None
  ): ConfigValue[Effect, String] =
    readEnvString(parts, prefix).or(readPropString(parts, prefix))

  /** Constructs an environment variable and system property reader for a type
    * of A given parts and an optional prefix.
    *
    * @param parts
    *   List of string parts. List("one", "two", "three") becomes one.two.three
    *   for system properties, and ONE_TWO_THREE for environment variables
    * @param prefix
    *   Optional prefix. Default to None. List("one", "two", "three") with a
    *   prefix of "zero" becomes zero.one.two.three for system properties, and
    *   ZERO_ONE_TWO_THREE for environment variables
    * @param CD
    *   [[https://cir.is/api/ciris/ConfigDecoder.html ConfigDecoder]] for String
    *   \=> A
    * @return
    *   [[https://cir.is/api/ciris/ConfigValue.html ConfigValue]] for the config
    */
  def read[A](
      parts: List[String],
      prefix: Option[String] = None
  )(implicit
      CD: ConfigDecoder[String, A]
  ): ConfigValue[Effect, A] =
    readString(parts, prefix).as[A]

  /** Constructs an environment variable and system property reader for a type
    * of A given parts and an optional prefix. Provides a means to default the
    * value if not configured.
    *
    * @param parts
    *   List of string parts. List("one", "two", "three") becomes one.two.three
    *   for system properties, and ONE_TWO_THREE for environment variables
    * @param default
    *   value of type A
    * @param prefix
    *   Optional prefix. Default to None. List("one", "two", "three") with a
    *   prefix of "zero" becomes zero.one.two.three for system properties, and
    *   ZERO_ONE_TWO_THREE for environment variables
    * @param CD
    *   [[https://cir.is/api/ciris/ConfigDecoder.html ConfigDecoder]] for String
    *   \=> A
    * @return
    *   [[https://cir.is/api/ciris/ConfigValue.html ConfigValue]] for the config
    */
  def readDefaulted[A](
      parts: List[String],
      default: A,
      prefix: Option[String] = None
  )(implicit
      CD: ConfigDecoder[String, A]
  ): ConfigValue[Effect, A] =
    read(parts, prefix).default(default)

  /** Constructs an environment variable and system property reader for a type
    * of A given parts and an optional prefix. Used for optional configuration
    * and resolved as Option types.
    *
    * @param parts
    *   List of string parts. List("one", "two", "three") becomes one.two.three
    *   for system properties, and ONE_TWO_THREE for environment variables
    * @param prefix
    *   Optional prefix. Default to None. List("one", "two", "three") with a
    *   prefix of "zero" becomes zero.one.two.three for system properties, and
    *   ZERO_ONE_TWO_THREE for environment variables
    * @param CD
    *   [[https://cir.is/api/ciris/ConfigDecoder.html ConfigDecoder]] for String
    *   \=> A
    * @return
    *   [[https://cir.is/api/ciris/ConfigValue.html ConfigValue]] for the
    *   config, containing an Option[A]
    */
  def readOptional[A](
      parts: List[String],
      prefix: Option[String] = None
  )(implicit
      CD: ConfigDecoder[String, A]
  ): ConfigValue[Effect, Option[A]] =
    read(parts, prefix).option
}
