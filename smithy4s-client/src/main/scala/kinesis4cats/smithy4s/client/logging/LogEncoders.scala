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

package kinesis4cats.smithy4s.client.logging

import org.http4s.{Request, Response}

import kinesis4cats.logging.LogEncoder

/** Helper class containing required
  * [[kinesis4cats.logging.LogEncoder LogEncoders]] for the
  * [[kinesis4cats.smithy4s.client.DynamoClient DynamoClient]]
  */
final class LogEncoders[F[_]](implicit
    val http4sRequestLogEncoder: LogEncoder[Request[F]],
    val http4sResponseLogEncoder: LogEncoder[Response[F]]
)
