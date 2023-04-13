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

import cats.syntax.all._
import ciris._

/** Https or Http enum
  *
  * @param name
  *   Name of protocol
  */
sealed abstract class Protocol(val name: String)

object Protocol {
  case object Http extends Protocol("http")
  case object Https extends Protocol("https")

  val values: List[Protocol] = List(Http, Https)

  implicit val protocolConfigDecoder: ConfigDecoder[String, Protocol] =
    ConfigDecoder[String, String].mapEither { case (_, x) =>
      Either.fromOption(
        Protocol.values.find(r => x === r.name),
        ConfigError(s"$x is not a known protocol")
      )
    }
}
