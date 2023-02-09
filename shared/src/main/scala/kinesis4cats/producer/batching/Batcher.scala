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
