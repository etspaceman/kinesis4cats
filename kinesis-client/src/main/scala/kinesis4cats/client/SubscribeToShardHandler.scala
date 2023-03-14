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

package kinesis4cats.client

import cats.effect.Async
import cats.effect.Deferred
import cats.effect.std.Dispatcher
import cats.syntax.all._
import org.typelevel.log4cats.StructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import software.amazon.awssdk.core.async.SdkPublisher
import software.amazon.awssdk.core.exception.SdkClientException
import software.amazon.awssdk.http.SdkCancellationException
import software.amazon.awssdk.services.kinesis.model.SubscribeToShardEventStream
import software.amazon.awssdk.services.kinesis.model.SubscribeToShardResponse
import software.amazon.awssdk.services.kinesis.model.SubscribeToShardResponseHandler

import kinesis4cats.logging.LogContext

private[kinesis4cats] class SubscribeToShardHandler[F[_]](
    dispatcher: Dispatcher[F],
    logger: StructuredLogger[F],
    val deferredResponse: Deferred[F, SubscribeToShardResponse],
    val deferredComplete: Deferred[F, Either[Throwable, Unit]],
    val deferredPublisher: Deferred[F, SdkPublisher[
      SubscribeToShardEventStream
    ]]
)(implicit F: Async[F])
    extends SubscribeToShardResponseHandler {

  override def responseReceived(response: SubscribeToShardResponse): Unit =
    dispatcher.unsafeRunSync(
      logger.debug(LogContext().context)(s"Received response") >>
        deferredResponse.complete(response).void
    )

  override def onEventStream(
      publisher: SdkPublisher[SubscribeToShardEventStream]
  ): Unit =
    dispatcher.unsafeRunSync(
      logger.debug(LogContext().context)(s"Received event stream") >>
        deferredPublisher.complete(publisher).void
    )

  override def exceptionOccurred(throwable: Throwable): Unit =
    dispatcher.unsafeRunSync(
      (Option(throwable.getCause()) match {
        // These are safe to simply debug, they will be raised if needed in the client
        case Some(x: SdkCancellationException) =>
          logger.debug(LogContext().context)(x.getMessage())
        // Non-deterministically this can get a 2nd exception with a nested cancellation exception
        case Some(x: SdkClientException) =>
          Option(x.getCause()) match {
            case Some(x: SdkCancellationException) =>
              logger.debug(LogContext().context)(x.getMessage())
            case _ =>
              logger.debug(LogContext().context, throwable)(
                s"Exception occurred"
              )
          }
        case _ =>
          logger.debug(LogContext().context, throwable)(s"Exception occurred")
      }) >>
        deferredComplete.complete(Left(throwable)).void
    )

  override def complete(): Unit =
    dispatcher.unsafeRunSync(
      logger.debug(LogContext().context)(s"Complete received") >>
        deferredComplete.complete(Right(())).void
    )
}

private[kinesis4cats] object SubscribeToShardHandler {
  def apply[F[_]](
      dispatcher: Dispatcher[F]
  )(implicit F: Async[F]): F[SubscribeToShardHandler[F]] = for {
    deferredResponse <- Deferred[F, SubscribeToShardResponse]
    deferredComplete <- Deferred[F, Either[Throwable, Unit]]
    deferredPublisher <- Deferred[F, SdkPublisher[SubscribeToShardEventStream]]
    logger <- Slf4jLogger.create[F]
  } yield new SubscribeToShardHandler(
    dispatcher,
    logger,
    deferredResponse,
    deferredComplete,
    deferredPublisher
  )
}
