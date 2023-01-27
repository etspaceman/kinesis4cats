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

import java.nio.ByteBuffer

import cats.effect.{Deferred, Sync}
import cats.syntax.all._
import software.amazon.kinesis.processor.RecordProcessorCheckpointer
import software.amazon.kinesis.retrieval.KinesisClientRecord
import software.amazon.kinesis.retrieval.kpl.ExtendedSequenceNumber

/** A message from Kinesis that is able to be committed.
  *
  * @param shardId
  *   The unique identifier for the shard from which this record originated
  * @param recordProcessorStartingSequenceNumber
  *   The starting sequence number for the
  *   [[kinesis4cats.kcl.RecordProcessor RecordProcessor]] which received this
  *   record
  * @param millisBehindLatest
  *   Milleseconds behind the latest record, used to detect if the consumer is
  *   lagging the producer
  * @param record
  *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/retrieval/KinesisClientRecord.java KinesisClientRecord]]
  *   representing the original record received by Kinesis.
  * @param recordProcessor
  *   Reference to the [[kinesis4cats.kcl.RecordProcessor RecordProcessor]] that
  *   is responsible for processing this message
  * @param checkpointer
  *   Reference to the
  *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/processor/RecordProcessorCheckpointer.java RecordProcessorCheckpointer]]
  *   responsible for committing the record
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

  val data: ByteBuffer = record.data()

  /** Determines if the [[kinesis4cats.kcl.RecordProcessor RecordProcesor]] is
    * in a state that is allowed to commit this record.
    *
    * @return
    *   F containing a Boolean indicator of commit availability
    */
  def canCheckpoint: F[Boolean] = recordProcessor.state.get.map {
    case RecordProcessor.State.Processing | RecordProcessor.State.ShardEnded =>
      true
    case _ => false
  }

  /** Commits this record. If it is the last record in the shard, completes the
    * [[cats.effect.Deferred lastRecordDeferred]]
    *
    * @return
    *   F of Unit
    */
  def checkpoint: F[Unit] =
    for {
      _ <- F.interruptibleMany(
        checkpointer.checkpoint(
          record.sequenceNumber(),
          record.subSequenceNumber()
        )
      )
      _ <- if (isLastInShard) lastRecordDeferred.complete(()).void else F.unit
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
