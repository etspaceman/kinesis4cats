package kinesis4cats.smithy4s.client
package middleware

import cats.syntax.all._
import org.http4s.client.Client
import kinesis4cats.localstack.LocalstackConfig
import org.http4s.Uri
import cats.effect.Async
import org.http4s.Request

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
