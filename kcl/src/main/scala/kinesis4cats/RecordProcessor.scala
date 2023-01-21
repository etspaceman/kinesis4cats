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

package kinesis4cats

import scala.jdk.CollectionConverters._

import cats.effect.std.Dispatcher
import cats.effect.{Deferred, Ref, Sync}
import cats.syntax.all._
import software.amazon.kinesis.lifecycle.events.{InitializationInput, LeaseLostInput, ProcessRecordsInput, ShardEndedInput, ShutdownRequestedInput}
import software.amazon.kinesis.processor.ShardRecordProcessor
import software.amazon.kinesis.retrieval.kpl.ExtendedSequenceNumber

private[kinesis4cats] class RecordProcessor[F[_]](
    dispatcher: Dispatcher[F],
    val lastRecordDeferred: Deferred[F, Unit],
    val state: Ref[F, RecordProcessorState],
    val deferredException: Deferred[F, Throwable]
)(cb: List[CommittableRecord[F]] => F[Unit])(implicit F: Sync[F])
    extends ShardRecordProcessor {
  
  private var shardId: String = _ // scalafix:ok
  private var extendedSequenceNumber: ExtendedSequenceNumber = _ // scalafix:ok

  def getShardId: String = shardId
  def getExtendedSequenceNumber: ExtendedSequenceNumber = extendedSequenceNumber

  override def initialize(initializationInput: InitializationInput): Unit =
    dispatcher.unsafeRunSync(
      for {
        _ <- F.delay(this.shardId = initializationInput.shardId())
        _ <- F.delay(this.extendedSequenceNumber =
          initializationInput.extendedSequenceNumber()
        )
        _ <- state.set(RecordProcessorState.Initialized)
      } yield ()
    )

  override def processRecords(processRecordsInput: ProcessRecordsInput): Unit =
    dispatcher.unsafeRunSync(
      for {
        _ <- state.set(RecordProcessorState.Processing)
        batch = processRecordsInput
          .records()
          .asScala
          .toList
          .map(x =>
            CommittableRecord(
              shardId,
              extendedSequenceNumber,
              processRecordsInput.millisBehindLatest(),
              x,
              this,
              processRecordsInput.checkpointer(),
              lastRecordDeferred
            )
          )
        records =
          if (processRecordsInput.isAtShardEnd)
            batch match {
              case head :+ last => head :+ last.copy(isLastInShard = true)
              case _            => Nil
            }
          else batch
          _ <- cb(records)
      } yield ()
    )

  override def leaseLost(leaseLostInput: LeaseLostInput): Unit =
    dispatcher.unsafeRunSync(
      for {
        _ <- state.set(RecordProcessorState.LeaseLost)
      } yield ()
    )

  override def shardEnded(shardEndedInput: ShardEndedInput): Unit =
    dispatcher.unsafeRunSync(
      for {
        _ <- state.set(RecordProcessorState.ShardEnded)
        _ <- lastRecordDeferred.get // TODO Add timeout config
        _ <- F.interruptibleMany(shardEndedInput.checkpointer().checkpoint())
      } yield ()
    )

  override def shutdownRequested(
      shutdownRequestedInput: ShutdownRequestedInput
  ): Unit = dispatcher.unsafeRunSync(
    for {
      _ <- state.set(RecordProcessorState.Shutdown)
    } yield ()
  )

}
