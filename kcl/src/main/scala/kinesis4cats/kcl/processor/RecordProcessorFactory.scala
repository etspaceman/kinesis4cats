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

/** An implementation of the
  * [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/processor/ShardRecordProcessorFactory.java ShardRecordProcessorFactory]]
  * interface. This is passed to the KCL Scheduler for generating
  * [[kinesis4cats.kcl.processor.RecordProcessor RecordProcessors]]
  *
  * @param config
  *   [[kinesis4cats.kcl.processor.RecordProcessorConfig RecordProcessorConfig]]
  *   instance
  * @param dispatcher
  *   [[cats.effect.std.Dispatcher Dispatcher]] instance, for running effects
  * @param state
  *   [[cats.effect.Ref Ref]] that tracks the current state, via
  *   [[kinesis4cats.kcl.processor.RecordProcessorState RecordProcessorState]]
  * @param deferredException
  *   [[cats.effect.Deferred Deferred]] instance, for handling exceptions
  * @param raiseOnError
  *   Whether the RecordProcessor should raise exceptions or simply log them.
  * @param cb
  *   Function to process
  *   [[kinesis4cats.kcl.CommittableRecord CommittableRecords]] received from
  *   Kinesis
  * @param F
  *   [[cats.effect.Async Async]] instance
  * @param encoders
  *   [[kinesis4cats.kcl.processor.RecordProcessorLogEncoders RecordProcessorLogEncoders]]
  *   for encoding structured logs
  */
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
        state <- Ref.of[F, RecordProcessorState](RecordProcessorState.Created)
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

  /** Creates a
    * [[kinesis4cats.kcl.processor.RecordProcessorFactory RecordProcessorFactory]]
    * as a [[cats.effect.Resource Resource]]
    *
    * @param config
    *   [[kinesis4cats.kcl.processor.RecordProcessorConfig RecordProcessorConfig]]
    *   instance
    * @param state
    *   [[cats.effect.Ref Ref]] that tracks the current state, via
    *   [[kinesis4cats.kcl.processor.RecordProcessorState RecordProcessorState]]
    * @param deferredException
    *   [[cats.effect.Deferred Deferred]] instance, for handling exceptions
    * @param raiseOnError
    *   Whether the
    *   [[kinesis4cats.kcl.processor.RecordProcessor RecordProcessor]] should
    *   raise exceptions or simply log them.
    * @param cb
    *   Function to process
    *   [[kinesis4cats.kcl.CommittableRecord CommittableRecords]] received from
    *   Kinesis
    * @param F
    *   [[cats.effect.Async Async]] instance
    * @param encoders
    *   [[kinesis4cats.kcl.processor.RecordProcessorLogEncoders RecordProcessorLogEncoders]]
    *   for encoding structured logs
    * @return
    *   [[cats.effect.Resource Resource]] containing a
    *   [[kinesis4cats.kcl.processor.RecordProcessorFactory RecordProcessorFactory]]
    */
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
