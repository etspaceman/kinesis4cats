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
package middleware

import cats.effect.{Async, Ref, Resource}
import cats.syntax.all._
import fs2.{Chunk, Pipe, Stream}
import org.http4s.Response
import org.http4s.client.Client
import org.typelevel.log4cats.StructuredLogger

import kinesis4cats.logging.LogContext

object ResponseLogger {
  def apply[F[_]](logger: StructuredLogger[F])(client: Client[F])(implicit
      F: Async[F],
      LE: KinesisClient.LogEncoders[F]
  ): Client[F] = {
    import LE._

    def logResponse(response: Response[F]): Resource[F, Response[F]] =
      Resource.suspend {
        val ctx = LogContext().addEncoded("response", response)

        Ref[F].of(Vector.empty[Chunk[Byte]]).map { vec =>
          val dumpChunksToVec: Pipe[F, Byte, Nothing] =
            _.chunks.flatMap(s => Stream.exec(vec.update(_ :+ s)))

          val observeDumpChunks: Pipe[F, Byte, Byte] =
            _.observe(dumpChunksToVec)

          Resource.make(
            // Cannot Be Done Asynchronously - Otherwise All Chunks May Not Be Appended before Finalization
            F.pure(response.withBodyStream(observeDumpChunks(response.body)))
          ) { _ =>
            val newBody = Stream.eval(vec.get).flatMap(Stream.emits).unchunks
            val newResponseBody: Response[F] = response.withBodyStream(newBody)

            for {
              _ <- logger.debug(ctx.context)("Successfully completed request")
              _ <- logger
                .trace(ctx.addEncoded("responseBody", newResponseBody).context)(
                  "Logging response body"
                )
                .handleErrorWith(t =>
                  logger.error(ctx.context, t)("Error logging response body")
                )
            } yield ()
          }
        }
      }

    Client(req => client.run(req).flatMap(logResponse))
  }

}
