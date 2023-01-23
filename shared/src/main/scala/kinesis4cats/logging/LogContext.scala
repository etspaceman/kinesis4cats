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

package kinesis4cats.logging

import java.util.UUID

// $COVERAGE-OFF$
/** Class that represents a structured logging context.
  *
  * @param context
  *   Map[String, String] representing the structured logs
  */
final class LogContext private (val context: Map[String, String]) {

  /** Add a String->String key-value pair to the Logging context
    *
    * @param kv
    *   String->String key-value pair to add
    * @return
    *   [[kinesis4cats.logging.LogContext LogContext]]
    */
  def +(kv: (String, String)): LogContext = new LogContext(context + kv)

  /** Add a String->A key-value pair to the Logging context, given a
    * [[kinesis4cats.logging.LogEncoder LogEncoder]] for the value's type
    *
    * @param key
    *   String key value
    * @param a
    *   Value to add, must have a [[kinesis4cats.logging.LogEncoder LogEncoder]]
    *   instance
    * @return
    *   [[kinesis4cats.logging.LogContext LogContext]]
    */
  def addEncoded[A](
      key: String,
      a: A
  )(implicit E: LogEncoder[A]): LogContext = new LogContext(
    context + (key -> Option(a).fold("null")(x => E.encode(x)))
  )
}

object LogContext {

  /** Constructor for [[kinesis4cats.logging.LogContext LogContext]].
    *
    * @return
    *   [[kinesis4cats.logging.LogContext LogContext]]
    */
  def apply(): LogContext = new LogContext(
    Map("contextId" -> UUID.randomUUID.toString)
  )
}
// $COVERAGE-ON$
