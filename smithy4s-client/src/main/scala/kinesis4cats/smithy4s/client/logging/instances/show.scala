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
package smithy4s.client
package logging
package instances

import cats.Show
import org.http4s.{Request, Response}

import kinesis4cats.logging.instances.show._

/** Smithy4s Client [[kinesis4cats.logging.LogEncoder LogEncoder]] instances for
  * string encoding of log structures using [[cats.Show Show]]
  */
object show {
  implicit def requestShow[F[_]]: Show[Request[F]] = x =>
    ShowBuilder("Request")
      .add("method", x.method)
      .add("headers", x.headers)
      .add("httpVersion", x.httpVersion)
      .add("uri", x.uri)
      .build

  implicit def responseShow[F[_]]: Show[Response[F]] = x =>
    ShowBuilder("Response")
      .add("headers", x.headers)
      .add("httpVersion", x.httpVersion)
      .add("status", x.status)
      .build

  implicit def smithy4sClientLogEncoders[F[_]]
      : LogEncoders[F] = new LogEncoders[F]()

}
