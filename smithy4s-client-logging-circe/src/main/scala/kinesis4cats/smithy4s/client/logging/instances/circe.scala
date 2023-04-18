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

package kinesis4cats.smithy4s.client
package logging.instances

import io.circe.Encoder
import org.http4s.circe._
import org.http4s.{Headers, HttpVersion, Method, Request, Response, Status}

import kinesis4cats.logging.instances.circe._

/** Smithy4s Client [[kinesis4cats.logging.LogEncoder LogEncoder]] instances for
  * string encoding of log structures using [[io.circe.Encoder Encoder]]
  */
object circe {

  def kinesisClient[F[_]]: KinesisClient.LogEncoders[F] = {
    implicit val headersCirceEncoder: Encoder[Headers] =
      Encoder[List[(String, String)]]
        .contramap(_.headers.map(x => x.name.toString -> x.value))

    implicit val httpVersionEncoder: Encoder[HttpVersion] =
      Encoder.forProduct2("major", "minor")(x => (x.major, x.minor))

    implicit val methodEncoder: Encoder[Method] =
      Encoder[String].contramap(_.name)

    implicit val statusEncoder: Encoder[Status] =
      Encoder.forProduct2("code", "reason")(x => (x.code, x.reason))

    implicit val requestCirceEncoder: Encoder[Request[F]] =
      Encoder.forProduct4("headers", "httpVersion", "method", "uri")(x =>
        (x.headers, x.httpVersion, x.method, x.uri)
      )

    implicit val responseCirceEncoder: Encoder[Response[F]] =
      Encoder.forProduct3("headers", "httpVersion", "status")(x =>
        (x.headers, x.httpVersion, x.status)
      )

    new KinesisClient.LogEncoders[F]()
  }

}
