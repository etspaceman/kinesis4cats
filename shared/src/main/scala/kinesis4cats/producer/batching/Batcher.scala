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
package batching

import cats.Eq
import cats.data._
import cats.syntax.all._

import kinesis4cats.producer.Producer.InvalidRecord

/** A batcher of records against configured limits.
  *
  * @param config
  *   [[kinesis4cats.producer.batching.Batcher.Config Batcher.Config]]
  */
private[kinesis4cats] final class Batcher(config: Batcher.Config) {

  /** Batch a list of [[kinesis4cats.producer.Record.WithShard records]]
    *
    * @param records
    *   [[cats.data.NonEmptyList NonEmptyList]] of
    *   [[kinesis4cats.producer.Record.WithShard records]]
    * @param retrying
    *   Determines if the batcher is being executed during a retry. This helps
    *   to avoid re-aggregation of data during retries
    * @return
    *   [[kinesis4cats.producer.batching.Batcher.Result Batcher.Result]]
    */
  def batch(
      records: NonEmptyList[Record.WithShard],
      retrying: Boolean
  ): Batcher.Result = {
    val errors = records.collect {
      case record
          if !record.record
            .isValidPayloadSize(config.maxPayloadSizePerRecord) =>
        InvalidRecord.RecordTooLarge(record.record)
      case record
          if !record.record.isValidPartitionKey(
            config.minPartitionKeySize,
            config.maxPartitionKeySize
          ) =>
        InvalidRecord.InvalidPartitionKey(record.record.partitionKey)
      case record if !record.record.isValidExplicitHashKey =>
        InvalidRecord.InvalidExplicitHashKey(record.record.explicitHashKey)
    }

    val valid = records.filter(
      _.record.isValid(
        config.maxPayloadSizePerRecord,
        config.minPartitionKeySize,
        config.maxPartitionKeySize
      )
    )

    val batchRes: List[Batch] = NonEmptyList
      .fromList(valid)
      .flatMap(x =>
        if (config.aggregate && !retrying) _aggregateAndBatch(x)
        else _batch(x)
      )
      .map(_.toList)
      .getOrElse(Nil)

    Batcher.Result(errors, batchRes)

  }

  /** Aggregate a list of [[kinesis4cats.producer.Record.WithShard records]],
    * and then batch them
    *
    * @param records
    *   [[cats.data.NonEmptyList NonEmptyList]] of
    *   [[kinesis4cats.producer.Record.WithShard records]]
    * @param res
    *   Result to return. None when initiated.
    * @return
    *   List of batches
    */
  private[kinesis4cats] def _aggregateAndBatch(
      records: NonEmptyList[Record.WithShard]
  ): Option[NonEmptyList[Batch]] = {
    val aggregated =
      records
        .groupByNem(_.record.partitionKey)
        .toNonEmptyList
        .flatTraverse(_aggregatePartitionKey(_))

    aggregated.flatMap(_batch(_))
  }

  /** Aggregate a list of [[kinesis4cats.producer.Record.WithShard records]]
    * that share the same partition key
    *
    * @param records
    *   [[cats.data.NonEmptyList NonEmptyList]] of
    *   [[kinesis4cats.producer.Record.WithShard records]]
    * @param res
    *   Result to return. None when initiated.
    * @return
    *   List of batches
    */
  @annotation.tailrec
  private def _aggregatePartitionKey(
      records: NonEmptyList[Record.WithShard],
      res: Option[NonEmptyList[AggregatedBatch]] = None
  ): Option[NonEmptyList[Record.WithShard]] = {
    val record = records.head

    val newRes: Option[NonEmptyList[AggregatedBatch]] = res.fold(
      NonEmptyList.one(AggregatedBatch.create(record, config)).some
    ) { x =>
      val batch = x.head
      if (batch.canAdd(record)) NonEmptyList(batch.add(record), x.tail).some
      else x.prepend(AggregatedBatch.create(record, config)).some
    }

    NonEmptyList.fromList(records.tail) match {
      case None       => newRes.map(_.map(_.asRecord).reverse)
      case Some(recs) => _aggregatePartitionKey(recs, newRes)
    }
  }

  /** Batch a list of [[kinesis4cats.producer.Record.WithShard records]]
    *
    * @param records
    *   [[cats.data.NonEmptyList NonEmptyList]] of
    *   [[kinesis4cats.producer.Record.WithShard records]]
    * @param res
    *   Result to return. None when initiated.
    * @return
    *   List of batches
    */
  @annotation.tailrec
  private def _batch(
      records: NonEmptyList[Record.WithShard],
      res: Option[NonEmptyList[Batch]] = None
  ): Option[NonEmptyList[Batch]] = {
    val record = records.head

    val newRes: Option[NonEmptyList[Batch]] =
      res.fold(NonEmptyList.one(Batch.create(record, config)).some) { x =>
        val batch = x.head
        if (batch.canAdd(record)) NonEmptyList(batch.add(record), x.tail).some
        else x.prepend(Batch.create(record, config)).some
      }

    NonEmptyList.fromList(records.tail) match {
      case None       => newRes.map(_.map(_.finalizeBatch).reverse)
      case Some(recs) => _batch(recs, newRes)
    }
  }
}

object Batcher {

  private[kinesis4cats] final case class Result(
      invalid: List[Producer.InvalidRecord],
      batches: List[Batch]
  ) {
    val hasInvalid: Boolean = invalid.nonEmpty
    val hasBatches: Boolean = batches.nonEmpty
    val isSuccessful: Boolean = hasBatches && !hasInvalid
    val isPartiallySuccessful: Boolean = hasBatches && hasInvalid
  }

  object Result {
    implicit val batcherResultEq: Eq[Result] =
      Eq.by(x => (x.invalid, x.batches))
  }

  /** Configuration for the Batcher
    *
    * @param maxRecordsPerRequest
    *   Maximum records that can be submitted with a single request
    * @param maxPayloadSizePerRequest
    *   The maximum size that a single request can have
    * @param maxPayloadSizePerRecord
    *   The maximum size of a single record in a put request
    * @param maxPayloadSizePerShardPerSecond
    *   The maximum amount of data that a shard can receive per second
    * @param maxRecordsPerShardPerSecond
    *   The maximum amount of records that a shard can receive per second
    * @param maxPartitionKeySize
    *   The maximum size of a partition key
    * @param minPartitionKeySize
    *   The minimum size of a partition key
    * @param aggregate
    *   If true, records will be aggregated. See the
    *   [https://docs.aws.amazon.com/streams/latest/dev/kinesis-kpl-concepts.html#kinesis-kpl-concepts-aggretation
    *   AWS documentation] for more information
    */
  final case class Config(
      maxRecordsPerRequest: Int,
      maxPayloadSizePerRequest: Int,
      maxPayloadSizePerRecord: Int,
      maxPayloadSizePerShardPerSecond: Int,
      maxRecordsPerShardPerSecond: Int,
      maxPartitionKeySize: Int,
      minPartitionKeySize: Int,
      aggregate: Boolean
  )

  object Config {

    implicit val batcherConfigEq: Eq[Config] = Eq.fromUniversalEquals

    /** A default instance for the Batcher which uses the Kinesis limits
      */
    val default = Config(
      500,
      5 * 1024 * 1024,
      1024 * 1024,
      1024 * 1024,
      1000,
      256,
      1,
      aggregate = true
    )
  }
}
