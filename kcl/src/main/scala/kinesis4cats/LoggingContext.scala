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

package kinesis4cats

import java.util.UUID

import io.circe.{Encoder, Json}

// $COVERAGE-OFF$
final case class LoggingContext private (context: Map[String, String]) {
  def +(kv: (String, String)): LoggingContext = copy(context + kv)

  def addJson(key: String, js: Json): LoggingContext = copy(
    context + (key -> js.noSpacesSortKeys)
  )
  def addEncoded[A](
      key: String,
      a: A
  )(implicit E: Encoder[A]): LoggingContext =
    addJson(
      key,
      E(a)
    )
}

object LoggingContext {
  def create: LoggingContext = LoggingContext(
    Map("contextId" -> UUID.randomUUID.toString)
  )
}
// $COVERAGE-ON$
