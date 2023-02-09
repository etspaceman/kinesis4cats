package kinesis4cats.producer

import cats.syntax.all._
import cats.effect.syntax.all._
import kinesis4cats.models.StreamArn
import kinesis4cats.producer.batching.Batcher
import org.typelevel.log4cats.StructuredLogger
import cats.effect.Async
import cats.data.NonEmptyList
import cats.data.Ior
import cats.data.Ior.Both
import cats.kernel.Semigroup
import java.security.MessageDigest
import kinesis4cats.logging.LogContext

// format: off
abstract class Producer[F[_], PutRequest, PutResponse, PutNRequest, PutNResponse](implicit F: Async[F]) {
// format: on
  def logger: StructuredLogger[F]
  def shardMapCache: ShardMapCache[F]
  def config: Producer.Config

  def putImpl(req: PutRequest): F[PutResponse]

  def putNImpl(req: PutNRequest): F[PutNResponse]

  def asPutRequest(streamName: String, record: Record): PutRequest

  def asPutRequest(streamArn: StreamArn, record: Record): PutRequest

  def asPutNRequest(
      streamName: String,
      records: NonEmptyList[Record]
  ): PutNRequest

  def asPutNRequest(
      streamArn: StreamArn,
      records: NonEmptyList[Record]
  ): PutNRequest

  def put(streamName: String, record: Record): F[PutResponse] =
    putImpl(asPutRequest(streamName, record))

  def put(streamArn: StreamArn, record: Record): F[PutResponse] =
    putImpl(asPutRequest(streamArn, record))

  def putN(
      streamName: String,
      records: NonEmptyList[Record]
  ): F[Ior[Producer.Error, NonEmptyList[PutNResponse]]] = {
    val ctx = LogContext()
      .addEncoded("streamName", streamName)
      .addEncoded("recordsCount", records.length)

    val digest = Producer.md5Digest

    for {
      withShards <- records.traverse(rec =>
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
      res <- batched.traverse(batches =>
        batches.traverse(batch =>
          batch.shardBatches
            .map(_.records)
            .toList
            .parTraverseN(config.shardParallelism) { shardBatch =>
              putNImpl(asPutNRequest(streamName, shardBatch))
            }
        )
      )
    } yield Ior.left(Producer.Error(None))
  }
  /*
  def putN[A](
      streamArn: StreamArn,
      records: NonEmptyList[Record]
  ): F[Ior[Producer.Error, NonEmptyList[PutNResponse]]] =
    putNImpl(streamArn, records)*/
}

object Producer {
  def md5Digest = MessageDigest.getInstance("MD5")

  final case class Config(
      aggregate: Boolean,
      warnOnShardCacheMisses: Boolean,
      shardParallelism: Int
  )

  object Config {
    val default = Config(true, true, 8)
  }

  final case class Error(
      errors: Option[Ior[NonEmptyList[Record], NonEmptyList[FailedRecord]]]
  ) extends Exception {
    def add(that: Error): Error = Error(errors.combine(that.errors))
    override def getMessage(): String = errors match {
      case Some(Both(a, b)) =>
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
