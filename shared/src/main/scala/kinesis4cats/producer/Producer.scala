package kinesis4cats.producer

import cats.syntax.all._
import cats.effect.syntax.all._
import kinesis4cats.producer.batching.Batcher
import org.typelevel.log4cats.StructuredLogger
import cats.effect.Async
import cats.data.NonEmptyList
import cats.data.Ior
import cats.kernel.Semigroup
import java.security.MessageDigest
import kinesis4cats.logging.LogContext
import kinesis4cats.logging.LogEncoder

abstract class Producer[F[_], PutReq, PutRes, PutNReq, PutNRes](implicit
    F: Async[F],
    LE: Producer.LogEncoders
) {

  import LE._
  def logger: StructuredLogger[F]
  def shardMapCache: ShardMapCache[F]
  def config: Producer.Config

  protected def putImpl(req: PutReq): F[PutRes]
  protected def putNImpl(req: PutNReq): F[PutNRes]

  protected def asPutRequest(req: PutRequest): PutReq
  protected def asPutNRequest(req: PutNRequest): PutNReq

  protected def failedRecords(
      req: PutNReq,
      resp: PutNRes
  ): Option[NonEmptyList[Producer.FailedRecord]]

  def put(req: PutRequest): F[PutRes] =
    putImpl(asPutRequest(req))

  def putN(req: PutNRequest): F[Ior[Producer.Error, NonEmptyList[PutNRes]]] = {
    val ctx = LogContext().addEncoded("request", req)

    val digest = Producer.md5Digest

    for {
      withShards <- req.records.traverse(rec =>
        for {
          shardRes <- shardMapCache
            .shardForPartitionKey(digest, rec.partitionKey)
          _ <-
            if (config.warnOnShardCacheMisses)
              shardRes.leftTraverse(e =>
                logger.warn(ctx.context, e)(
                  s"Did not find a shard for Partition Key ${rec.partitionKey}"
                )
              )
            else F.unit
        } yield Record.WithShard.fromOption(rec, shardRes.toOption)
      )
      batched = Batcher.batch(withShards)
      res <- batched
        .traverse(batches =>
          batches.flatTraverse(batch =>
            batch.shardBatches
              .map(_.records)
              .parTraverseN(config.shardParallelism) { shardBatch =>
                val request: PutNReq =
                  asPutNRequest(req.withRecords(shardBatch))
                putNImpl(request).map { resp: PutNRes =>
                  failedRecords(request, resp)
                    .map(Producer.Error.putFailures)
                    .fold(
                      Ior.right[Producer.Error, PutNRes](resp)
                    )(e => Ior.both[Producer.Error, PutNRes](e, resp))
                }
              }
          )
        )
        .map(_.flatMap(_.sequence))
      _ <- res.leftTraverse { e =>
        if (config.raiseOnFailures) F.raiseError[Unit](e)
        else if (config.warnOnBatchFailures)
          logger.warn(ctx.context, e)(
            "Some records had errors when processing batch(es)"
          )
        else F.unit
      }
    } yield res
  }
}

object Producer {
  final class LogEncoders(implicit
      val putReqeustLogEncoder: LogEncoder[PutRequest],
      val putNRequestLogEncoder: LogEncoder[PutNRequest]
  )

  def md5Digest = MessageDigest.getInstance("MD5")

  final case class Config(
      warnOnShardCacheMisses: Boolean,
      warnOnBatchFailures: Boolean,
      shardParallelism: Int,
      raiseOnFailures: Boolean
  )

  object Config {
    val default = Config(true, true, 8, false)
  }

  final case class Error(
      errors: Option[Ior[NonEmptyList[Record], NonEmptyList[FailedRecord]]]
  ) extends Exception {
    def add(that: Error): Error = Error(errors.combine(that.errors))
    override def getMessage(): String = errors match {
      case Some(Ior.Both(a, b)) =>
        Error.tooLargeMessage(a) + "\n\nAND\n\n" + Error.putFailuresMessage(b)
      case Some(Ior.Left(a))  => Error.tooLargeMessage(a)
      case Some(Ior.Right(b)) => Error.putFailuresMessage(b)
      case None => s"Batcher returned no results at all, this is unexpected"
    }
  }

  object Error {
    implicit val producerErrorSemigroup: Semigroup[Error] =
      new Semigroup[Error] {
        override def combine(x: Error, y: Error): Error =
          Error(x.errors.combine(y.errors))
      }
    private def tooLargeMessage(records: NonEmptyList[Record]): String =
      s"${records.length} records were too large to be put in Kinesis."

    private def putFailuresMessage(failures: NonEmptyList[FailedRecord]) =
      s"${failures.length} records received failures when producing to Kinesis.\n\t" +
        failures.toList
          .map(x =>
            s"Error Code: ${x.errorCode}, Error Message: ${x.erorrMessage}"
          )
          .mkString("\n\t")

    final case class RecordsTooLarge(records: NonEmptyList[Record])
    final case class PutFailures(failures: NonEmptyList[FailedRecord])

    def recordsTooLarge(records: NonEmptyList[Record]): Error = Error(
      Some(Ior.left(records))
    )

    def putFailures(records: NonEmptyList[FailedRecord]): Error = Error(
      Some(Ior.right(records))
    )
  }

  final case class FailedRecord(
      record: Record,
      errorCode: String,
      erorrMessage: String
  )
}
