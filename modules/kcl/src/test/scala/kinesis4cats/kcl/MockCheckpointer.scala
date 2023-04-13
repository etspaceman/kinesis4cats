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

import software.amazon.kinesis.checkpoint.Checkpoint
import software.amazon.kinesis.processor.Checkpointer
import software.amazon.kinesis.retrieval.kpl.ExtendedSequenceNumber

object MockCheckpointer extends Checkpointer {
  override def setCheckpoint(
      leaseKey: String,
      checkpointValue: ExtendedSequenceNumber,
      concurrencyToken: String
  ): Unit = ()

  override def getCheckpoint(leaseKey: String): ExtendedSequenceNumber =
    ExtendedSequenceNumber.LATEST

  override def getCheckpointObject(leaseKey: String): Checkpoint =
    new Checkpoint(
      ExtendedSequenceNumber.LATEST,
      ExtendedSequenceNumber.LATEST,
      Array.emptyByteArray
    )

  override def prepareCheckpoint(
      leaseKey: String,
      pendingCheckpoint: ExtendedSequenceNumber,
      concurrencyToken: String
  ): Unit = ()

  override def prepareCheckpoint(
      leaseKey: String,
      pendingCheckpoint: ExtendedSequenceNumber,
      concurrencyToken: String,
      pendingCheckpointState: Array[Byte]
  ): Unit = ()

  override def operation(operation: String): Unit = ()

  override def operation(): String = ""

}
