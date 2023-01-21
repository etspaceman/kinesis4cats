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

import cats.effect.Sync
import cats.effect.kernel.Deferred
import cats.syntax.all._
import software.amazon.kinesis.processor.RecordProcessorCheckpointer
import software.amazon.kinesis.retrieval.KinesisClientRecord
import software.amazon.kinesis.retrieval.kpl.ExtendedSequenceNumber

/** A message type from Kinesis which has not yet been commited or checkpointed.
  *
  * @constructor
  *   create a new commitable record with a name and age.
  * @param shardId
  *   the unique identifier for the shard from which this record originated
  * @param millisBehindLatest
  *   ms behind the latest record, used to detect if the consumer is lagging the
  *   producer
  * @param record
  *   the original record document from Kinesis
  * @param recordProcessor
  *   reference to the record processor that is responsible for processing this
  *   message
  * @param checkpointer
  *   reference to the checkpointer used to commit this record
  */
final case class CommittableRecord[F[_]](
    shardId: String,
    recordProcessorStartingSequenceNumber: ExtendedSequenceNumber,
    millisBehindLatest: Long,
    record: KinesisClientRecord,
    recordProcessor: RecordProcessor[F],
    checkpointer: RecordProcessorCheckpointer,
    lastRecordDeferred: Deferred[F, Unit],
    isLastInShard: Boolean = false
)(implicit F: Sync[F]) {
  val sequenceNumber: String = record.sequenceNumber()
  val subSequenceNumber: Long = record.subSequenceNumber()
  def checkpoint: F[Unit] =
    for {
      _ <- F.interruptible(
        checkpointer.checkpoint(
          record.sequenceNumber(),
          record.subSequenceNumber()
        )
      )
      _ <- if (isLastInShard) lastRecordDeferred.complete(()) else F.unit
    } yield ()

}

object CommittableRecord {

  // Only makes sense to compare Records belonging to the same shard
  // Records that have been batched by the KCL producer all have the
  // same sequence number but will differ by subsequence number
  implicit def orderBySequenceNumber[F[_]]: Ordering[CommittableRecord[F]] =
    Ordering[(String, Long)].on(cr =>
      (
        cr.sequenceNumber,
        cr.record match {
          case ur: KinesisClientRecord => ur.subSequenceNumber()
          case null                    => 0 // scalafix:ok
        }
      )
    )
}
