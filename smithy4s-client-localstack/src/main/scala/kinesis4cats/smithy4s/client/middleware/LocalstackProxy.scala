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

import cats.effect.Async
import cats.effect.syntax.all._
import cats.syntax.all._
import org.http4s.client.Client
import org.http4s.{Header, Request, Uri}
import org.typelevel.ci._
import org.typelevel.log4cats.StructuredLogger

import kinesis4cats.localstack.LocalstackConfig
import kinesis4cats.logging.{LogContext, LogEncoder}

object LocalstackProxy {

  private def proxyUri[F[_]](
      config: LocalstackConfig,
      req: Request[F],
      logger: StructuredLogger[F]
  )(implicit
      F: Async[F],
      LE: KinesisClient.LogEncoders[F],
      LELC: LogEncoder[LocalstackConfig]
  ): F[Request[F]] = {
    import LE._
    val newReq = req
      .withUri(
        req.uri.copy(authority =
          req.uri.authority.map(x =>
            x.copy(
              host = Uri.RegName(config.host),
              port = config.servicePort.some
            )
          )
        )
      )
      .putHeaders(Header.Raw(ci"host", config.host))
    val ctx = LogContext()
      .addEncoded("localstackConfig", config)
      .addEncoded("originalRequest", req)
      .addEncoded("newRequest", newReq)

    logger.debug(ctx.context)("Proxying request").as(newReq)
  }

  def apply[F[_]](config: LocalstackConfig, logger: StructuredLogger[F])(
      client: Client[F]
  )(implicit
      F: Async[F],
      LE: KinesisClient.LogEncoders[F],
      LELC: LogEncoder[LocalstackConfig]
  ): Client[F] = Client { req =>
    for {
      proxied <- proxyUri(config, req, logger).toResource
      res <- client.run(proxied)
    } yield res
  }

  def apply[F[_]](logger: StructuredLogger[F], prefix: Option[String] = None)(
      client: Client[F]
  )(implicit
      F: Async[F],
      LE: KinesisClient.LogEncoders[F],
      LELC: LogEncoder[LocalstackConfig]
  ): Client[F] = Client { req =>
    for {
      config <- LocalstackConfig.resource[F](prefix)
      proxied <- proxyUri(config, req, logger).toResource
      res <- client.run(proxied)
    } yield res
  }
}