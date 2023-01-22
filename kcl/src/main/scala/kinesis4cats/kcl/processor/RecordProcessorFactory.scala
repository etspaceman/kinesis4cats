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
package processor

import cats.effect.std.Dispatcher
import cats.effect.{Async, Deferred, Ref, Resource}
import cats.syntax.all._
import org.typelevel.log4cats.slf4j.Slf4jLogger
import software.amazon.kinesis.common.StreamIdentifier
import software.amazon.kinesis.processor._

class RecordProcessorFactory[F[_]] private[kinesis4cats] (
    config: RecordProcessorConfig,
    dispatcher: Dispatcher[F],
    deferredException: Deferred[F, Throwable],
    raiseOnError: Boolean
)(cb: List[CommittableRecord[F]] => F[Unit])(implicit
    F: Async[F],
    encoders: RecordProcessorLogEncoders
) extends ShardRecordProcessorFactory {
  override def shardRecordProcessor(): ShardRecordProcessor =
    dispatcher.unsafeRunSync(
      for {
        lastRecordDeferred <- Deferred[F, Unit]
        state <- Ref.of[F, RecordProcessorState](RecordProcessorState.NoState)
        logger <- Slf4jLogger.create[F]
      } yield new RecordProcessor[F](
        config,
        dispatcher,
        lastRecordDeferred,
        state,
        deferredException,
        logger,
        raiseOnError
      )(cb)
    )
  override def shardRecordProcessor(
      streamIdentifier: StreamIdentifier
  ): ShardRecordProcessor = shardRecordProcessor()
}

object RecordProcessorFactory {
  def apply[F[_]](
      config: RecordProcessorConfig,
      deferredException: Deferred[F, Throwable],
      raiseOnError: Boolean
  )(
      cb: List[CommittableRecord[F]] => F[Unit]
  )(implicit
      F: Async[F],
      encoders: RecordProcessorLogEncoders
  ): Resource[F, RecordProcessorFactory[F]] =
    Dispatcher.parallel.map { dispatcher =>
      new RecordProcessorFactory[F](
        config,
        dispatcher,
        deferredException,
        raiseOnError
      )(cb)
    }
}
