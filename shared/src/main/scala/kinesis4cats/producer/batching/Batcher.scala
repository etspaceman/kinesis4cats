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

object Batcher {
  @annotation.tailrec
  def batch(
      records: NonEmptyList[Record.WithShard],
      res: Option[IorNel[Record, NonEmptyList[Batch]]] = None
  ): Ior[Producer.Error, NonEmptyList[Batch]] = {
    val record = records.head

    val newRes: Option[IorNel[Record, NonEmptyList[Batch]]] =
      if (!record.isWithinLimits)
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
              NonEmptyList.one(Batch.create(record))
            )
          ) { r =>
            r.putRight(r.right.fold(NonEmptyList.one(Batch.create(record))) {
              x =>
                val batch = x.head
                if (batch.canAdd(record))
                  NonEmptyList(batch.add(record), x.tail)
                else x.prepend(Batch.create(record))
            })
          }
          .some

    NonEmptyList.fromList(records.tail) match {
      case None =>
        res.fold(
          Ior.left[Producer.Error, NonEmptyList[Batch]](Producer.Error(None))
        )(r =>
          r.bimap(x => Producer.Error.recordsTooLarge(x.reverse), _.reverse)
        )
      case Some(recs) => batch(recs, newRes)
    }
  }
}
