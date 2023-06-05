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

package kinesis4cats.kcl
package http4s

import _root_.fs2.io.net.Network
import cats.effect.{Async, Ref, Resource}
import cats.syntax.all._
import com.comcast.ip4s.{Host, Port}
import org.http4s.HttpRoutes
import org.http4s.ember.server._
import org.http4s.implicits._
import org.http4s.server.Server
import smithy4s.http4s.SimpleRestJsonBuilder
import smithy4s.http4s.swagger.docs
import software.amazon.kinesis.coordinator.WorkerStateChangeListener.WorkerState

import kinesis4cats.kcl.http4s.generated.{Response, ServiceNotReadyError}

// $COVERAGE-OFF$

/** A simple [[https://http4s.org/ Http4s]] service for KCL users. Often times
  * users need to deploy their consumers as a part of an orchestration service,
  * such as Kubernetes. Those orchestration services typically depend on some
  * basic http routes to be available so that the deployments can be determined
  * as ready and healthy.
  *
  * This service provides 2 routes:
  *   - `healthcheck` - always returns 200
  *   - `initialized` - returns 200 if the KCL has started, otherwise returns
  *     503
  *
  * @param state
  *   [[cats.effect.Ref Ref]] containing the
  *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/coordinator/WorkerStateChangeListener.java WorkerState]]
  * @param F
  *   [[cats.effect.Async Async]]
  */
class KCLService[F[_]] private[kinesis4cats] (state: Ref[F, WorkerState])(
    implicit F: Async[F]
) extends generated.KCLService[F] {
  override def initialized(): F[Response] = state.get.flatMap {
    case WorkerState.STARTED => F.pure(Response("Ok"))
    case _ =>
      F.raiseError(
        ServiceNotReadyError(
          Some("The kinesis consumer is not yet initialized")
        )
      )
  }
  override def healthcheck(): F[Response] = F.pure(Response("Ok"))
}

object KCLService {

  /** Basic constuctor for the KCLService
    *
    * @param consumer
    *   [[kinesis4cats.kcl.KCLConsumer KCLConsumer]]
    * @param F
    *   [[cats.effect.Async Async]]
    * @return
    *   [[cats.effect.Resource Resource]] containing a
    *   [[kinesis4cats.kcl.http4s.KCLService KCLService]]
    */
  def apply[F[_]](consumer: KCLConsumer[F])(implicit
      F: Async[F]
  ): Resource[F, KCLService[F]] =
    consumer.runWithRefListener().map(new KCLService[F](_))

  /** Creates [[org.http4s.HttpRoutes HttpRoutes]] that represents this service.
    * Includes Swagger routes.
    *
    * @param consumer
    *   [[kinesis4cats.kcl.KCLConsumer KCLConsumer]]
    * @param F
    *   [[cats.effect.Async Async]]
    * @return
    *   [[cats.effect.Resource Resource]] containing
    *   [[org.http4s.HttpRoutes HttpRoutes]]
    */
  def routes[F[_]](
      consumer: KCLConsumer[F]
  )(implicit F: Async[F]): Resource[F, HttpRoutes[F]] = for {
    service <- KCLService[F](consumer)
    docRoutes = docs[F](generated.KCLService)
    routes <- SimpleRestJsonBuilder
      .routes(service)
      .resource
      .map(serviceRoutes => docRoutes <+> serviceRoutes)
  } yield routes

  /** Creates [[org.http4s.server.Server Server]] that represents this service.
    *
    * @param consumer
    *   [[kinesis4cats.kcl.KCLConsumer KCLConsumer]]
    * @param port
    *   [[com.comcast.ip4s.Port Port]] for the server
    * @param host
    *   [[com.comcast.ip4s.Host Host]] for the server
    * @param F
    *   [[cats.effect.Async Async]]
    * @return
    *   [[cats.effect.Resource Resource]] containing
    *   [[org.http4s.server.Server Server]]
    */
  def server[F[_]](
      consumer: KCLConsumer[F],
      port: Port,
      host: Host
  )(implicit F: Async[F], N: Network[F]): Resource[F, Server] =
    routes(consumer).flatMap { routes =>
      EmberServerBuilder
        .default[F]
        .withPort(port)
        .withHost(host)
        .withHttpApp(routes.orNotFound)
        .build
    }
}
// $COVERAGE-ON$
