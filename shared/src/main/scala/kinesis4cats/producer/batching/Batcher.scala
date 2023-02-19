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

import cats.data._
import cats.syntax.all._

import kinesis4cats.producer.Producer.InvalidRecord
import kinesis4cats.syntax.id._

/** A batcher of records against configured limits.
  *
  * @param config
  *   [[kinesis4cats.producer.batching.Batcher.Config Batcher.Config]]
  */
final class Batcher(config: Batcher.Config) {

  /** Batch a list of [[kinesis4cats.producer.Record.WithShard records]]
    *
    * @param records
    *   [[cats.data.NonEmptyList NonEmptyList]] of
    *   [[kinesis4cats.producer.Record.WithShard records]]
    * @return
    *   [[cats.data.Ior Ior]] with the following:
    *   - left: a [[kinesis4cats.producer.Producer.Error Producer.Error]], which
    *     represents records that were invalid for Kinesis puts.
    *   - right: a [[cats.data.NonEmptyList NonEmptyList]] of compliant Kinesis
    *     [[kinesis4cats.producer.batching.Batch batches]]
    */
  def batch(
      records: NonEmptyList[Record.WithShard]
  ): Ior[Producer.Error, NonEmptyList[Batch]] = {
    val errors = NonEmptyList.fromList(records.collect {
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
    })

    val valid = NonEmptyList.fromList(
      records.filter(
        _.record.isValid(
          config.maxPayloadSizePerRecord,
          config.minPartitionKeySize,
          config.maxPartitionKeySize
        )
      )
    )

    (errors, valid) match {
      case (None, None) => Ior.left(Producer.Error(None))
      case (Some(es), None) =>
        Ior.left(Producer.Error.invalidRecords(es))
      case (es, Some(recs)) =>
        val batchRes: Option[NonEmptyList[Batch]] =
          if (config.aggregate) _aggregateAndBatch(recs) else _batch(recs)

        (es, batchRes) match {
          case (None, None) =>
            Ior.left[Producer.Error, NonEmptyList[Batch]](Producer.Error(None))
          case (Some(ers), None) =>
            Ior.left[Producer.Error, NonEmptyList[Batch]](
              Producer.Error.invalidRecords(ers)
            )
          case (ers, Some(x)) =>
            Ior
              .right[Producer.Error, NonEmptyList[Batch]](x)
              .maybeTransform(ers) { case (a, b) =>
                a.putLeft(Producer.Error.invalidRecords(b))

              }
        }
    }
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
  private def _aggregateAndBatch(
      records: NonEmptyList[Record.WithShard]
  ): Option[NonEmptyList[Batch]] = {
    val aggregated =
      records
        .groupByNem(_.predictedShard)
        .toNonEmptyList
        .flatTraverse(_aggregateShard(_))

    aggregated.flatMap(_batch(_))
  }

  /** Aggregate a list of [[kinesis4cats.producer.Record.WithShard records]]
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
  private def _aggregateShard(
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
      case Some(recs) => _aggregateShard(recs, newRes)
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

  /** Configuration for the [[kinesis4cats.producer.batching.Batcher Batcher]]
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

    /** A default instance for the
      * [[kinesis4cats.producer.batching.Batcher Batcher]] which uses the
      * Kinesis limits
      */
    val default = Config(
      500,
      5 * 1024 * 1024,
      1024 * 1024,
      1024 * 1024,
      1000,
      256,
      1,
      true
    )
  }
}
