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
import kinesis4cats.logging.LogContext

/** Middleware for [[https://http4s.org/v0.23/docs/client.html Clients]] that
  * proxies request against the configured Localstack environment
  */
object LocalstackProxy {

  /** Updates the uri of a [[org.http4s.Request Request]] to use the configured
    * Localstack environment. Also logs the before/after shape of the request.
    *
    * @param config
    *   [[kinesis4cats.localstack.LocalstackConfig LocalstackConfig]]
    * @param req
    *   [[org.http4s.Request Request]] to proxy
    * @param logger
    *   [[org.typelevel.log4cats.StructuredLogger StructuredLogger]]
    * @param F
    *   [[cats.effect.Async Async]]
    * @param kinesisClientEncoders
    *   [[kinesis4cats.smithy4s.client.KinesisClient.LogEncoders KinesisClient.LogEncoders]]
    * @param localstackConfigEncoders
    *   [[kinesis4cats.localstack.LocalstackConfig.LogEncoders LocalstackConfig.LogEncoders]]
    * @return
    *   [[cats.effect.Async Async]] of a proxied [[org.http4s.Request Request]]
    */
  private def proxyUri[F[_]](
      config: LocalstackConfig,
      req: Request[F],
      logger: StructuredLogger[F],
      kinesisClientEncoders: KinesisClient.LogEncoders[F],
      localstackConfigEncoders: LocalstackConfig.LogEncoders
  )(implicit F: Async[F]): F[Request[F]] = {
    import kinesisClientEncoders._
    import localstackConfigEncoders._
    val newReq = req
      .withUri(
        req.uri.copy(authority =
          req.uri.authority.map(x =>
            x.copy(
              host = Uri.RegName(config.kinesisHost),
              port = config.kinesisPort.some
            )
          )
        )
      )
      .putHeaders(Header.Raw(ci"host", config.kinesisHost))
    val ctx = LogContext()
      .addEncoded("localstackConfig", config)
      .addEncoded("originalRequest", req)
      .addEncoded("newRequest", newReq)

    logger.debug(ctx.context)("Proxying request").as(newReq)
  }

  /** Applies middleware to a
    * [[https://http4s.org/v0.23/docs/client.html Client]] that updates the uri
    * of a [[org.http4s.Request Request]] to use the configured Localstack
    * environment. Also logs the before/after shape of the request.
    *
    * @param config
    *   [[kinesis4cats.localstack.LocalstackConfig LocalstackConfig]]
    * @param logger
    *   [[org.typelevel.log4cats.StructuredLogger StructuredLogger]]
    * @param F
    *   [[cats.effect.Async Async]]
    * @param kinesisClientEncoders
    *   [[kinesis4cats.smithy4s.client.KinesisClient.LogEncoders KinesisClient.LogEncoders]]
    * @param localstackConfigEncoders
    *   [[kinesis4cats.localstack.LocalstackConfig.LogEncoders LocalstackConfig.LogEncoders]]
    * @return
    *   [[cats.effect.Async Async]] of a proxied [[org.http4s.Request Request]]
    */
  def apply[F[_]](
      config: LocalstackConfig,
      logger: StructuredLogger[F],
      kinesisClientEncoders: KinesisClient.LogEncoders[F],
      localstackConfigEncoders: LocalstackConfig.LogEncoders
  )(
      client: Client[F]
  )(implicit
      F: Async[F]
  ): Client[F] = Client { req =>
    for {
      proxied <- proxyUri(
        config,
        req,
        logger,
        kinesisClientEncoders,
        localstackConfigEncoders
      ).toResource
      res <- client.run(proxied)
    } yield res
  }

  /** Applies middleware to a
    * [[https://http4s.org/v0.23/docs/client.html Client]] that updates the uri
    * of a [[org.http4s.Request Request]] to use the configured Localstack
    * environment. Also logs the before/after shape of the request.
    *
    * @param logger
    *   [[org.typelevel.log4cats.StructuredLogger StructuredLogger]]
    * @param prefix
    *   Optional string prefix to apply when loading configuration. Default to
    *   None
    * @param F
    *   [[cats.effect.Async Async]]
    * @param kinesisClientEncoders
    *   [[kinesis4cats.smithy4s.client.KinesisClient.LogEncoders KinesisClient.LogEncoders]]
    * @param localstackConfigEncoders
    *   [[kinesis4cats.localstack.LocalstackConfig.LogEncoders LocalstackConfig.LogEncoders]]
    * @return
    *   [[cats.effect.Async Async]] of a proxied [[org.http4s.Request Request]]
    */
  def apply[F[_]](
      logger: StructuredLogger[F],
      prefix: Option[String] = None,
      kinesisClientEncoders: KinesisClient.LogEncoders[F] =
        KinesisClient.LogEncoders.show[F],
      localstackConfigEncoders: LocalstackConfig.LogEncoders =
        LocalstackConfig.LogEncoders.show
  )(
      client: Client[F]
  )(implicit
      F: Async[F]
  ): Client[F] = Client { req =>
    for {
      config <- LocalstackConfig.resource[F](prefix)
      proxied <- proxyUri(
        config,
        req,
        logger,
        kinesisClientEncoders,
        localstackConfigEncoders
      ).toResource
      res <- client.run(proxied)
    } yield res
  }
}
