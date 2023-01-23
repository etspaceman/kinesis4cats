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

import cats.effect.IO
import cats.effect.std.Queue
import cats.effect.unsafe.implicits.global
import software.amazon.awssdk.services.kinesis.model.Record
import software.amazon.kinesis.processor.{
  Checkpointer,
  PreparedCheckpointer,
  RecordProcessorCheckpointer
}

case class MockRecordProcessorCheckpointer(queue: Queue[IO, MockCheckpoint])
    extends RecordProcessorCheckpointer {
  override def checkpoint(): Unit = ()

  override def checkpoint(record: Record): Unit = ()

  override def checkpoint(sequenceNumber: String): Unit = ()

  override def checkpoint(
      sequenceNumber: String,
      subSequenceNumber: Long
  ): Unit = queue
    .offer(MockCheckpoint(sequenceNumber, subSequenceNumber))
    .unsafeRunSync()

  override def prepareCheckpoint(): PreparedCheckpointer =
    MockPreparedCheckpointer

  override def prepareCheckpoint(
      applicationState: Array[Byte]
  ): PreparedCheckpointer = MockPreparedCheckpointer

  override def prepareCheckpoint(record: Record): PreparedCheckpointer =
    MockPreparedCheckpointer

  override def prepareCheckpoint(
      record: Record,
      applicationState: Array[Byte]
  ): PreparedCheckpointer = MockPreparedCheckpointer

  override def prepareCheckpoint(sequenceNumber: String): PreparedCheckpointer =
    MockPreparedCheckpointer

  override def prepareCheckpoint(
      sequenceNumber: String,
      applicationState: Array[Byte]
  ): PreparedCheckpointer = MockPreparedCheckpointer

  override def prepareCheckpoint(
      sequenceNumber: String,
      subSequenceNumber: Long
  ): PreparedCheckpointer = MockPreparedCheckpointer

  override def prepareCheckpoint(
      sequenceNumber: String,
      subSequenceNumber: Long,
      applicationState: Array[Byte]
  ): PreparedCheckpointer = MockPreparedCheckpointer

  override def checkpointer(): Checkpointer = MockCheckpointer

}
