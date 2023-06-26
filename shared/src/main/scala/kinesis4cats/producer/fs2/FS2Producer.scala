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

package kinesis4cats.producer
package fs2

import scala.concurrent.duration._

import _root_.fs2.concurrent.Channel
import cats.Applicative
import cats.effect.Outcome._
import cats.effect._
import cats.effect.syntax.all._
import cats.syntax.all._
import org.typelevel.log4cats.StructuredLogger

import kinesis4cats.logging.LogContext
import kinesis4cats.models.StreamNameOrArn

/** An interface that runs a [[kinesis4cats.producer.Producer Producer's]] put
  * method in the background against a stream of records, offered by the user.
  * This is intended to be used in the same way that the
  * [[https://github.com/awslabs/amazon-kinesis-producer KPL]].
  *
  * @param F
  *   [[cats.effect.Async Async]]
  * @tparam PutReq
  *   The class that represents a batch put request for the underlying client
  * @tparam PutRes
  *   The class that represents a batch put response for the underlying client
  */
abstract class FS2Producer[F[_], PutReq, PutRes](implicit
    F: Async[F]
) {

  def logger: StructuredLogger[F]
  def config: FS2Producer.Config[F]

  /** The underlying queue of records to process
    */
  protected def channel
      : Channel[F, (Record, Deferred[F, F[Producer.Result[PutRes]]])]

  protected def underlying: Producer[F, PutReq, PutRes]

  /** Put a record into the producer's buffer, to be batched and produced at a
    * defined interval
    *
    * @param record
    *   [[kinesis4cats.producer.Record Record]]
    * @return
    *   F of F of unit. Inner F represents a `deferred.get` call, which will
    *   complete when the record has been published.
    */
  def put(record: Record): F[F[Producer.Result[PutRes]]] = {
    val ctx = LogContext()

    for {
      _ <- logger.debug(ctx.context)("Received record to put")
      deferred <- Deferred[F, F[Producer.Result[PutRes]]]
      res <- channel.send(record -> deferred)
      _ <- res.bitraverse(
        _ =>
          logger.warn(ctx.context)(
            "Producer has been shut down and will not accept further requests"
          ),
        _ =>
          logger.debug(ctx.context)(
            "Successfully put record into processing queue"
          )
      )
    } yield deferred.get.flatten
  }

  /** Stop the processing of records
    */
  private[kinesis4cats] def stop(f: Fiber[F, Throwable, Unit]): F[Unit] = {
    val ctx = LogContext()
    for {
      _ <- logger.debug(ctx.context)("Stopping the FS2KinesisProducer")
      _ <- channel.close
      _ <- f.join.void.timeoutTo(config.gracefulShutdownWait, f.cancel)
    } yield ()
  }

  /** Start the processing of records
    */
  private[kinesis4cats] def start(): F[Unit] = {
    val ctx = LogContext()

    for {
      _ <- logger
        .debug(ctx.context)("Starting the FS2KinesisProducer")
      _ <- channel.stream
        .groupWithin(config.putMaxChunk, config.putMaxWait)
        .evalMap { x =>
          val c = ctx.addEncoded("batchSize", x.size)
          x.toNel
            .traverse_ { x =>
              val (records, deferreds) = x.unzip
              for {
                _ <- logger.debug(c.context)(
                  "Received batch to process"
                )
                _ <- underlying.put(records).guaranteeCase {
                  case Succeeded(x) =>
                    deferreds.traverse_(_.complete(x))
                  case Canceled() =>
                    val failedF =
                      F.canceled >> F.raiseError[Producer.Result[PutRes]](
                        new RuntimeException("Put request was cancelled")
                      )
                    deferreds.traverse_(_.complete(failedF))
                  case Errored(e) =>
                    val failedF = F.raiseError[Producer.Result[PutRes]](e)
                    deferreds.traverse_(d => d.complete(failedF))
                }
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

object FS2Producer {

  /** Configuration for the
    * [[kinesis4cats.producer.fs2.FS2Producer FS2Producer]]
    *
    * @param queueSize
    *   Size of underlying buffer of records
    * @param putMaxChunk
    *   Max records to buffer before running a put request
    * @param putMaxWait
    *   Max time to wait before running a put request
    * @param producerConfig
    *   [[kinesis4cats.producer.Producer.Config Producer.Config]]
    */
  final case class Config[F[_]](
      queueSize: Int,
      putMaxChunk: Int,
      putMaxWait: FiniteDuration,
      producerConfig: Producer.Config[F],
      gracefulShutdownWait: FiniteDuration
  )

  object Config {
    def default[F[_]](
        streamNameOrArn: StreamNameOrArn
    )(implicit F: Applicative[F]): Config[F] = Config[F](
      1000,
      500,
      100.millis,
      Producer.Config.default[F](streamNameOrArn),
      30.seconds
    )
  }

}
