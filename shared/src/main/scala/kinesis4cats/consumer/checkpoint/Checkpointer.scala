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

package kinesis4cats.consumer.checkpoint

import scala.concurrent.duration._

import cats.effect.Async
import cats.syntax.all._
import org.typelevel.log4cats.StructuredLogger

import kinesis4cats.compat.retry.RetryPolicies._
import kinesis4cats.compat.retry._
import kinesis4cats.consumer.CommittableRecord
import kinesis4cats.logging.LogContext
import kinesis4cats.logging.LogEncoder

abstract class Checkpointer[F[_]](implicit
    F: Async[F],
    LE: Checkpointer.LogEncoders
) {

  import LE._

  def logger: StructuredLogger[F]
  def config: Checkpointer.Config

  lazy val retryPolicy: RetryPolicy[F] =
    config.commitMaxRetries.fold(constantDelay(config.commitRetryInterval))(
      retries =>
        limitRetries(retries).join(constantDelay(config.commitRetryInterval))
    )

  protected def checkpointImpl(
      record: CommittableRecord
  ): F[Either[Checkpointer.Error, Unit]]

  private[kinesis4cats] def _checkpoint(
      record: CommittableRecord
  ): F[Either[Checkpointer.Error, Unit]] = {
    val ctx = LogContext()
    for {
      _ <- logger.debug(ctx.context)(s"Received checkpoint request")
      res <- retryingOnFailuresAndAllErrors[Either[Checkpointer.Error, Unit]][
        F,
        Throwable
      ](
        retryPolicy,
        (x: Either[Checkpointer.Error, Unit]) =>
          x.bitraverse(
            {
              case e: Checkpointer.Error.LeaseLost =>
                logger
                  .warn(ctx.context, e)("Lease Lost, stopping retry attempts")
                  .as(true)
              case _ => F.pure(false)
            },
            _ => F.pure(true)
          ).map(_.merge),
        (x: Either[Checkpointer.Error, Unit], details: RetryDetails) =>
          x.leftTraverse(e =>
            logger.error(ctx.addEncoded("retryDetails", details).context, e)(
              "Error putting leases, retrying"
            )
          ).void,
        (e: Throwable, details: RetryDetails) =>
          logger.error(ctx.addEncoded("retryDetails", details).context, e)(
            "Error putting leases, retrying"
          )
      )(
        checkpointImpl(record)
      )
    } yield res
  }

  def checkpoint(record: CommittableRecord): F[Unit]
}

object Checkpointer {

  abstract class Simple[F[_]](implicit
      F: Async[F],
      LE: Checkpointer.LogEncoders
  ) extends Checkpointer[F] {
    override def checkpoint(record: CommittableRecord): F[Unit] =
      _checkpoint(record).rethrow
  }

  final case class Config(
      commitMaxRetries: Option[Int],
      commitRetryInterval: FiniteDuration
  )

  final class LogEncoders(implicit
      val comittableRecordLogEncoder: LogEncoder[CommittableRecord],
      val retryDetailsLogEncoder: LogEncoder[RetryDetails]
  )

  sealed abstract class Error(msg: String, e: Option[Throwable] = None)
      extends Exception(msg, e.orNull)

  object Error {
    final case class LeaseLost(leaseKey: String)
        extends Error(
          s"Lease for key ${leaseKey} was lost before a checkpoint could be made."
        )

    final case class Unhandled(leaseKey: String, e: Throwable)
        extends Error(
          s"Unhandled exception occurred during checkpoint operation for lease key ${leaseKey}",
          Some(e)
        )
  }
}
