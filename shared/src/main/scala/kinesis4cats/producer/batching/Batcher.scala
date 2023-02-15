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

final class Batcher(config: Batcher.Config) {
  @annotation.tailrec
  def batch(
      records: NonEmptyList[Record.WithShard],
      res: Option[IorNel[Record, NonEmptyList[Batch]]] = None
  ): Ior[Producer.Error, NonEmptyList[Batch]] = {
    val record = records.head

    val newRes: Option[IorNel[Record, NonEmptyList[Batch]]] =
      if (!record.isWithinLimits(config.maxPayloadSizePerRecord))
        res.fold(
          Ior.leftNel[Record, NonEmptyList[Batch]](record.record).some
        )(x =>
          x.putLeft(
            x.left.fold(NonEmptyList.one(record.record))(
              _.prepend(record.record)
            )
          ).some
        )
      else
        res
          .fold(
            Ior.right[NonEmptyList[Record], NonEmptyList[Batch]](
              NonEmptyList.one(Batch.create(record, config))
            )
          ) { r =>
            r.putRight(
              r.right.fold(NonEmptyList.one(Batch.create(record, config))) {
                x =>
                  val batch = x.head
                  if (batch.canAdd(record))
                    NonEmptyList(batch.add(record), x.tail)
                  else x.prepend(Batch.create(record, config))
              }
            )
          }
          .some

    NonEmptyList.fromList(records.tail) match {
      case None =>
        newRes.fold(
          Ior.left[Producer.Error, NonEmptyList[Batch]](Producer.Error(None))
        )(r =>
          r.bimap(
            x => Producer.Error.recordsTooLarge(x.reverse),
            _.map(_.finalizeBatch).reverse
          )
        )
      case Some(recs) => batch(recs, newRes)
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
    */
  final case class Config(
      maxRecordsPerRequest: Int,
      maxPayloadSizePerRequest: Int,
      maxPayloadSizePerRecord: Int,
      maxPayloadSizePerShardPerSecond: Int,
      maxRecordsPerShardPerSecond: Int
  )

  object Config {

    /** A default instance for the
      * [[kinesis4cats.producer.batching.Batcher Batcher]] which uses the
      * Kinesis limits
      */
    val default = Config(
      500,
      5 * 1024 * 1024,
      1 * 1024 * 921, // 1 MB TODO actually 90%, to avoid underestimating and getting Kinesis errors
      1 * 1024 * 1024,
      1000
    )
  }
}
