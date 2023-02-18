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

package kinesis4cats.instances

import cats.Eq
import cats.syntax.all._

import kinesis4cats.models.ShardId
import kinesis4cats.producer

object eq {
  implicit val arrayByteEq: Eq[Array[Byte]] = (x, y) => x.sameElements(y)

  implicit val recordEq: Eq[producer.Record] = (x, y) =>
    x.data === y.data && x.explicitHashKey === y.explicitHashKey && x.partitionKey === y.partitionKey

  implicit val shardIdEq: Eq[ShardId] = Eq.fromUniversalEquals

  implicit val shardBatchEq: Eq[producer.batching.ShardBatch] = (x, y) =>
    x.shardId === y.shardId &&
      x.records === y.records &&
      x.count === y.count &&
      x.batchSize === y.batchSize

  implicit val batchEq: Eq[producer.batching.Batch] = (x, y) =>
    x.shardBatches === y.shardBatches && x.batchSize === y.batchSize && x.count === y.count

  implicit val failedRecordEq: Eq[producer.Producer.FailedRecord] = (x, y) =>
    x.record === y.record && x.errorCode === y.errorCode && x.erorrMessage === y.erorrMessage

  implicit val recordTooLargeEq
      : Eq[producer.Producer.InvalidRecord.RecordTooLarge] = (x, y) =>
    x.record === y.record

  implicit val invalidPartitionKeyEq
      : Eq[producer.Producer.InvalidRecord.InvalidPartitionKey] =
    Eq.fromUniversalEquals

  implicit val invalidExplicitHashKeyEq
      : Eq[producer.Producer.InvalidRecord.InvalidExplicitHashKey] =
    Eq.fromUniversalEquals

  implicit val invalidRecordEq: Eq[producer.Producer.InvalidRecord] = {
    case (
          x: producer.Producer.InvalidRecord.RecordTooLarge,
          y: producer.Producer.InvalidRecord.RecordTooLarge
        ) =>
      x === y

    case (
          x: producer.Producer.InvalidRecord.InvalidPartitionKey,
          y: producer.Producer.InvalidRecord.InvalidPartitionKey
        ) =>
      x === y

    case (
          x: producer.Producer.InvalidRecord.InvalidExplicitHashKey,
          y: producer.Producer.InvalidRecord.InvalidExplicitHashKey
        ) =>
      x === y
    case _ => false
  }

  implicit val producerErrorEq: Eq[producer.Producer.Error] = (x, y) =>
    x.errors === y.errors
}
