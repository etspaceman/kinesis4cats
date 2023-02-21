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

import scala.concurrent.duration.FiniteDuration

import _root_.fs2.Stream
import cats.data.Ior
import cats.data.NonEmptyList
import cats.effect.Async
import cats.effect.Resource
import cats.effect.std.Queue
import cats.effect.syntax.all._
import cats.syntax.all._

abstract class FS2Producer[F[_], PutReq, PutRes](implicit
    F: Async[F]
) {

  def config: FS2Producer.Config
  protected def queue: Queue[F, Record]
  protected def callback
      : (Ior[Producer.Error, NonEmptyList[PutRes]], Async[F]) => F[Unit]

  protected def underlying: Producer[F, PutReq, PutRes]

  private[kinesis4cats] def start(): Resource[F, Unit] = Stream
    .fromQueueUnterminated(queue)
    .groupWithin(config.putMaxChunk, config.putMaxWait)
    .evalMap { x =>
      x.toNel.fold(F.unit) { records =>
        underlying
          .putWithRetry(records, config.putMaxRetries, config.putRetryInterval)
          .flatMap(callback(_, implicitly))
          .void
      }
    }
    .compile
    .drain
    .background
    .void

}

object FS2Producer {

  final case class Config(
      queueSize: Int,
      putMaxChunk: Int,
      putMaxWait: FiniteDuration,
      putMaxRetries: Option[Int],
      putRetryInterval: FiniteDuration,
      producerConfig: Producer.Config
  )

}
