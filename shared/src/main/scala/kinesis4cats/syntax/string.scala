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

package kinesis4cats.syntax

import scala.concurrent.duration.Duration
import scala.util.Try

import cats.syntax.all._

object string extends StringSyntax

trait StringSyntax {
  implicit def toStringOps(
      str: String
  ): StringSyntax.StringOps =
    new StringSyntax.StringOps(str)
}

@SuppressWarnings(Array("scalafix:DisableSyntax.throw"))
object StringSyntax {
  final class StringOps(private val str: String) extends AnyVal {
    def asMap: Either[String, Map[String, String]] = str
      .split(",")
      .toList
      .traverse { kv =>
        kv.split(":").toList match {
          case key :: value :: Nil => Right(key -> value)
          case _ =>
            Left(
              s"Could not parse map $str. Must be in the format key1:value1,key2:value2"
            )
        }
      }
      .map(_.toMap)

    def asMapUnsafe: Map[String, String] =
      asMap.fold(e => throw new RuntimeException(e), identity)

    def asList: List[String] = str.split(",").toList

    def asMillis: Either[Throwable, Long] =
      Try(Duration(str)).toEither.map(_.toMillis)

    def asMillisUnsafe: Long =
      asMillis.fold(e => throw e, identity)

    def asSeconds: Either[Throwable, Long] =
      Try(Duration(str)).toEither.map(_.toSeconds)

    def asSecondsUnsafe: Long =
      asSeconds.fold(e => throw e, identity)
  }
}
