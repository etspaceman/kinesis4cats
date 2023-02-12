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

import java.security.MessageDigest

import cats.data.{Ior, NonEmptyList}
import cats.effect.Async
import cats.effect.syntax.all._
import cats.kernel.Semigroup
import cats.syntax.all._
import org.typelevel.log4cats.StructuredLogger

import kinesis4cats.logging.{LogContext, LogEncoder}
import kinesis4cats.models.StreamNameOrArn
import kinesis4cats.producer.batching.Batcher

abstract class Producer[F[_], PutReq, PutRes](implicit
    F: Async[F],
    LE: Producer.LogEncoders
) {

  import LE._
  def logger: StructuredLogger[F]
  def shardMapCache: ShardMapCache[F]
  def config: Producer.Config

  protected def putImpl(req: PutReq): F[PutRes]
  protected def asPutRequest(req: PutRequest): PutReq

  protected def failedRecords(
      req: PutRequest,
      resp: PutRes
  ): Option[NonEmptyList[Producer.FailedRecord]]

  def put(req: PutRequest): F[Ior[Producer.Error, NonEmptyList[PutRes]]] = {
    val ctx = LogContext().addEncoded("request", req)

    val digest = Producer.md5Digest

    for {
      withShards <- req.records.traverse(rec =>
        for {
          shardRes <- shardMapCache
            .shardForPartitionKey(digest, rec.partitionKey)
          _ <-
            if (config.warnOnShardCacheMisses)
              shardRes
                .leftTraverse(e =>
                  logger.warn(ctx.context, e)(
                    s"Did not find a shard for Partition Key ${rec.partitionKey}"
                  )
                )
                .void
            else F.unit
        } yield Record.WithShard.fromOption(rec, shardRes.toOption)
      )
      batched = Batcher.batch(withShards)
      res <- batched
        .traverse(batches =>
          batches.flatTraverse(batch =>
            batch.shardBatches.toNonEmptyList
              .map(_.records)
              .parTraverseN(config.shardParallelism) { shardBatch =>
                val request = req.copy(records = shardBatch)
                for {
                  resp <- putImpl(asPutRequest(request))
                  res = failedRecords(request, resp)
                    .map(Producer.Error.putFailures)
                    .fold(
                      Ior.right[Producer.Error, PutRes](resp)
                    )(e => Ior.both[Producer.Error, PutRes](e, resp))
                  _ <- res.leftTraverse { e =>
                    if (config.raiseOnFailures) F.raiseError[Unit](e)
                    else if (config.warnOnBatchFailures)
                      logger.warn(ctx.context, e)(
                        "Some records had errors when processing a batch"
                      )
                    else F.unit
                  }
                } yield res
              }
          )
        )
        .map(_.flatMap(_.sequence))
    } yield res
  }
}

object Producer {
  final class LogEncoders(implicit
      val putReqeustLogEncoder: LogEncoder[PutRequest]
  )

  def md5Digest = MessageDigest.getInstance("MD5")

  final case class Config(
      warnOnShardCacheMisses: Boolean,
      warnOnBatchFailures: Boolean,
      shardParallelism: Int,
      raiseOnFailures: Boolean,
      shardMapCacheConfig: ShardMapCache.Config,
      streamNameOrArn: StreamNameOrArn
  )

  object Config {
    def default(streamNameOrArn: StreamNameOrArn) = Config(
      true,
      true,
      8,
      false,
      ShardMapCache.Config.default,
      streamNameOrArn
    )
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
