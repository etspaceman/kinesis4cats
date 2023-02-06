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

import cats.effect.Concurrent
import cats.syntax.all._
import org.http4s.{Charset, MediaType, Message}
import scodec.bits.ByteVector

object Utils {
  def logBody[F[_]: Concurrent](
      message: Message[F]
  ): F[String] = {
    val isBinary = message.contentType.exists(_.mediaType.binary)
    val isJson = message.contentType.exists(mT =>
      mT.mediaType == MediaType.application.json || mT.mediaType.subType
        .endsWith("+json")
    )
    val string =
      if (!isBinary || isJson)
        message
          .bodyText(implicitly, message.charset.getOrElse(Charset.`UTF-8`))
          .compile
          .string
      else
        message.body.compile.to(ByteVector).map(_.toHex)

    string
  }
}
