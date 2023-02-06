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
import cats.syntax.all._
import org.http4s.client.Client
import org.http4s.{Request, Uri}

import kinesis4cats.localstack.LocalstackConfig

object LocalstackProxy {
  private def proxyUri[F[_]](
      config: LocalstackConfig,
      req: Request[F]
  ): Request[F] =
    req.withUri(
      req.uri.copy(authority =
        req.uri.authority.map(x =>
          x.copy(
            host = Uri.RegName(config.host),
            port = config.servicePort.some
          )
        )
      )
    )

  def apply[F[_]](config: LocalstackConfig)(client: Client[F])(implicit
      F: Async[F]
  ): Client[F] = Client { req =>
    client.run(proxyUri(config, req))
  }

  def apply[F[_]](prefix: Option[String] = None)(client: Client[F])(implicit
      F: Async[F]
  ): Client[F] = Client { req =>
    for {
      config <- LocalstackConfig.resource[F](prefix)
      res <- client.run(proxyUri(config, req))
    } yield res
  }
}
