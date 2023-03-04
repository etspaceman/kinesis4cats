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

import cats.effect.syntax.all._
import cats.effect.{Async, Resource}
import cats.syntax.all._
import fs2.{Chunk, Pipe, Stream}
import org.http4s.Response
import org.http4s.client.Client
import org.typelevel.log4cats.StructuredLogger

import kinesis4cats.logging.LogContext
import kinesis4cats.smithy4s.client.logging.LogEncoders

/** Mostly copied from the Http4s
  * [[https://github.com/http4s/http4s/blob/series/0.23/client/shared/src/main/scala/org/http4s/client/middleware/RequestLogger.scala RequestLogger]].
  * Main difference is that this logs information to the context rather than the
  * message body. Also uses debug/trace instead of info.
  */
object RequestLogger {

  /** Apply
    * [[kinesis4cats.smithy4s.client.middleware.RequestLogger RequestLogger]]
    * middleware to a [[https://http4s.org/v0.23/docs/client.html Client]]
    *
    * @param logger
    *   [[org.typelevel.log4cats.StructuredLogger StructuredLogger]]
    * @param client
    *   [[https://http4s.org/v0.23/docs/client.html Client]]
    * @param F
    *   [[cats.effect.Async Async]]
    * @param LE
    *   [[kinesis4cats.smithy4s.client.logging.LogEncoders LogEncoders]]
    * @return
    *   [[https://http4s.org/v0.23/docs/client.html Client]] that logs its
    *   requests in debug and trace levels
    */
  def apply[F[_]](logger: StructuredLogger[F])(client: Client[F])(implicit
      F: Async[F],
      LE: LogEncoders[F]
  ): Client[F] = Client { req =>
    import LE._

    val ctx = LogContext().addEncoded("request", req)

    for {
      _ <- logger.debug(ctx.context)("Received request").toResource

      result <- Resource.suspend {
        (F.ref(false), F.ref(Vector.empty[Chunk[Byte]])).mapN {
          case (hasLogged, vec) =>
            val newBody =
              Stream.eval(vec.get).flatMap(v => Stream.emits(v)).unchunks

            val logOnceAtEnd: F[Unit] = hasLogged
              .getAndSet(true)
              .ifM(
                F.unit,
                for {
                  bdy <- Utils.logBody(req.withBodyStream(newBody))
                  _ <- logger
                    .trace(ctx.addEncoded("requestBody", bdy).context)(
                      "Logging request body"
                    )
                    .handleErrorWith { case t =>
                      logger.error(ctx.context, t)("Error logging request body")
                    }
                } yield ()
              )

            // Cannot Be Done Asynchronously - Otherwise All Chunks May Not Be Appended Previous to Finalization
            val logPipe: Pipe[F, Byte, Byte] =
              _.observe(_.chunks.flatMap(s => Stream.exec(vec.update(_ :+ s))))
                .onFinalizeWeak(logOnceAtEnd)

            val resp = client.run(req.withBodyStream(logPipe(req.body)))

            val finalizerPipe: Pipe[F, Byte, Byte] =
              _.onFinalizeWeak(logOnceAtEnd)

            // If the request body was not consumed (ex: bodiless GET)
            // the second best we can do is log on the response body finalizer
            // the third best is on the response resource finalizer (ex: if the client failed to pull the body)
            resp
              .map[Response[F]](x => x.withBodyStream(finalizerPipe(x.body)))
              .onFinalize(logOnceAtEnd)
        }
      }
    } yield result

  }
}
