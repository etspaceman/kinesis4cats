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
package fs2

import scala.concurrent.duration.FiniteDuration

import _root_.fs2.concurrent.Channel
import cats.effect.Async
import cats.effect.Fiber
import cats.effect.Resource
import cats.effect.syntax.all._
import cats.syntax.all._

import kinesis4cats.consumer.CommittableRecord
import kinesis4cats.logging.LogContext

abstract class FS2Checkpointer[F[_]](implicit
    F: Async[F],
    LE: Checkpointer.LogEncoders
) extends Checkpointer[F] {

  def fs2Config: FS2Checkpointer.Config
  override def config: Checkpointer.Config = fs2Config.checkpointConfig

  /** The underlying queue of records to checkpoint
    */
  protected def channel: Channel[F, CommittableRecord]

  /** A user defined function that can be run against the results of a request
    */
  protected def callback
      : (Either[Checkpointer.Error, Unit], Async[F]) => F[Unit]

  /** Stage a checkpoint, to be batched and executed at a defined interval
    *
    * @param record
    *   [[kinesis4cats.consumer.CommittableRecord CommittableRecord]]
    * @return
    *   F of Unit
    */
  override def checkpoint(record: CommittableRecord): F[Unit] = {
    val ctx = LogContext()

    for {
      _ <- logger.debug(ctx.context)("Received checkpoint to stage")
      res <- channel.send(record)
      _ <- res.bitraverse(
        _ =>
          logger.warn(ctx.context)(
            "Checkpointer has been shut down and will not accept further requests"
          ),
        _ =>
          logger.debug(ctx.context)(
            "Successfully put checkpoint into processing queue"
          )
      )
    } yield ()
  }

  /** Stop the processing of records
    */
  private[kinesis4cats] def stop(f: Fiber[F, Throwable, Unit]): F[Unit] = {
    val ctx = LogContext()
    for {
      _ <- logger.debug(ctx.context)("Stopping the FS2Checkpointer")
      _ <- channel.close
      _ <- f.join.void.timeoutTo(fs2Config.gracefulShutdownWait, f.cancel)
    } yield ()
  }

  /** Start the processing of records
    */
  private[kinesis4cats] def start(): F[Unit] = {
    val ctx = LogContext()

    for {
      _ <- logger
        .debug(ctx.context)("Starting the FS2Checkpointer")
      _ <- channel.stream
        .groupWithin(fs2Config.checkpointMaxChunk, fs2Config.checkpointMaxWait)
        .evalMap { x =>
          val c = ctx.addEncoded("batchSize", x.size)
          x.toNel.fold(F.unit) { records =>
            for {
              _ <- logger.debug(c.context)(
                "Received checkpoint batch to process"
              )
              res <- _checkpoint(records.maximum)
              _ <- callback(res, F)
              _ <- logger.debug(c.context)(
                "Finished processing batch"
              )
            } yield ()
          }
        }
        .compile
        .drain
    } yield ()
  }

  private[kinesis4cats] def resource: Resource[F, Unit] =
    Resource.make(start().start)(stop).void
}

object FS2Checkpointer {

  /** Configuration for the
    * [[kinesis4cats.consumer.checkpoint.fs2.FS2Checkpointer FS2Checkpointer]]
    *
    * @param queueSize
    *   Size of underlying buffer of records
    * @param checkpointMaxChunk
    *   Max records to buffer before running a checkpoint request
    * @param checkpointMaxWait
    *   Max time to wait before running a checkpoint request
    * @param checkpointConfig
    *   [[kinesis4cats.consumer.checkpoint.Checkpoint.Config Checkpoint.Config]]
    */
  final case class Config(
      queueSize: Int,
      checkpointMaxChunk: Int,
      checkpointMaxWait: FiniteDuration,
      checkpointConfig: Checkpointer.Config,
      gracefulShutdownWait: FiniteDuration
  )
}
