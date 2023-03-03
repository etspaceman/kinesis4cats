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

package kinesis4cats.smithy4s.client.middleware

import cats.effect.Async
import org.http4s.client.Client
import org.typelevel.log4cats.StructuredLogger

import kinesis4cats.smithy4s.client.logging.LogEncoders

/** Combines
  * [[kinesis4cats.smithy4s.client.middleware.RequestLogger RequestLogger]] and
  * [[kinesis4cats.smithy4s.client.middleware.ResponseLogger ResponseLogger]]
  * middleware
  */
object RequestResponseLogger {

  /** Apply
    * [[kinesis4cats.smithy4s.client.middleware.RequestResponseLogger RequestResponseLogger]]
    * middleware to a [[https://http4s.org/v0.23/docs/client.html Client]]
    *
    * @param logger
    *   [[org.typelevel.log4cats.StructuredLogger StructuredLogger]]
    * @param client
    *   [[https://http4s.org/v0.23/docs/client.html Client]]
    * @param F
    *   [[cats.effect.Async Async]]
    * @param LE
    *   [[kinesis4cats.smithy4s.client.LogEncoders LogEncoders]]
    * @return
    *   [[https://http4s.org/v0.23/docs/client.html Client]] that logs its
    *   requests in debug and trace levels
    */
  def apply[F[_]](logger: StructuredLogger[F])(client: Client[F])(implicit
      F: Async[F],
      LE: LogEncoders[F]
  ): Client[F] = ResponseLogger(logger)(RequestLogger(logger)(client))
}
