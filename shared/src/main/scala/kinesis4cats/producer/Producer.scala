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

import scala.concurrent.duration.FiniteDuration

import cats.Applicative
import cats.Semigroup
import cats.data.{Ior, NonEmptyList}
import cats.effect.Async
import cats.effect.kernel.Ref
import cats.effect.syntax.all._
import cats.syntax.all._
import org.typelevel.log4cats.StructuredLogger

import kinesis4cats.compat.retry._
import kinesis4cats.logging.{LogContext, LogEncoder}
import kinesis4cats.models.StreamNameOrArn
import kinesis4cats.producer.batching.Batcher

/** An interface that gives users the ability to efficiently batch and produce
  * records. A producer has a ShardMapCache, and uses it to predict the shard
  * that a record will be produced to. Knowing this, we can batch records
  * against both shard and stream-level limits for requests. There should be 1
  * instance of a [[kinesis4cats.producer.Producer Producer]] per Kinesis stream
  * (as a ShardMapCache will only consider a single stream)
  *
  * @param F
  *   [[cats.effect.Async Async]]
  * @param LE
  *   [[kinesis4cats.producer.Producer.LogEncoders Producer.LogEncoders]]
  * @tparam PutReq
  *   The class that represents a batch put request for the underlying client
  * @tparam PutRes
  *   The class that represents a batch put response for the underlying client
  */
abstract class Producer[F[_], PutReq, PutRes](implicit
    F: Async[F],
    LE: Producer.LogEncoders
) {

  import LE._

  def logger: StructuredLogger[F]

  def shardMapCache: ShardMapCache[F]

  def config: Producer.Config[F]

  private lazy val batcher: Batcher = new Batcher(config.batcherConfig)

  /** Underlying implementation for putting a batch request to Kinesis
    *
    * @param req
    *   the underlying put request
    * @return
    *   the underlying put response
    */
  protected def putImpl(req: PutReq): F[PutRes]

  /** Transforms a a [[cats.data.NonEmptyList NonEmptyList]] of
    * [[kinesis4cats.producer.Record Records]] into the underlying put request
    *
    * @param records
    *   a [[cats.data.NonEmptyList NonEmptyList]] of
    *   [[kinesis4cats.producer.Record Records]]
    * @return
    *   the underlying put request
    */
  protected def asPutRequest(records: NonEmptyList[Record]): PutReq

  /** Matches the a [[cats.data.NonEmptyList NonEmptyList]] of
    * [[kinesis4cats.producer.Record Records]] with the underlying response
    * records and returns any errored records. Useful for retrying any records
    * that failed
    *
    * @param records
    *   a [[cats.data.NonEmptyList NonEmptyList]] of
    *   [[kinesis4cats.producer.Record Records]]
    * @param resp
    *   the underlying put response
    * @return
    *   A list of
    *   [[kinesis4cats.producer.Producer.FailedRecord Producer.FailedRecord]]
    */
  protected def failedRecords(
      records: NonEmptyList[Record],
      resp: PutRes
  ): Option[NonEmptyList[Producer.FailedRecord]]

  private def _put(
      records: NonEmptyList[Record]
  ): F[Producer.Res[PutRes]] = {
    val ctx = LogContext()

    for {
      withShards <- records.traverse(rec =>
        for {
          shardRes <- shardMapCache
            .shardForPartitionKey(rec.partitionKey)
          _ <-
            if (config.warnOnShardCacheMisses)
              shardRes
                .leftTraverse(e =>
                  for {
                    _ <- logger.warn(ctx.context, e)(
                      s"Did not find a shard for Partition Key ${rec.partitionKey}"
                    )
                    _ <- logger.trace(ctx.addEncoded("record", rec).context)(
                      "Logging record"
                    )
                  } yield ()
                )
                .void
            else F.unit
        } yield Record.WithShard.fromOption(rec, shardRes.toOption)
      )
      batched = batcher.batch(withShards)
      res <- batched
        .traverse(batches =>
          batches.flatTraverse(batch =>
            batch.shardBatches.toNonEmptyList
              .map(_.records)
              .parTraverseN(config.shardParallelism) { shardBatch =>
                for {
                  resp <- putImpl(asPutRequest(shardBatch))
                  res = failedRecords(records, resp)
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

  /** This function is responsible for:
    *   - Predicting the shard that a record will land on
    *   - Batching records against Kinesis limits for shards / streams
    *   - Putting batches to Kinesis
    *   - Retrying failures per the configured RetryPolicy
    *
    * @param records
    *   a [[cats.data.NonEmptyList NonEmptyList]] of
    *   [[kinesis4cats.producer.Record Records]]
    * @return
    *   [[cats.data.Ior Ior]] with the following:
    *   - left: a [[kinesis4cats.producer.Producer.Error Producer.Error]], which
    *     represents records that were too large to be put on kinesis as well as
    *     records that were not produced successfully in the batch request.
    *   - right: a [[cats.data.NonEmptyList NonEmptyList]] of the underlying put
    *     responses
    */
  def put(records: NonEmptyList[Record]): F[Producer.Res[PutRes]] = {
    val ctx = LogContext()

    for {
      ref <- Ref.of(Producer.RetryState[PutRes](records, None))
      finalRes <- retryingOnFailuresAndAllErrors(
        config.retryPolicy,
        (x: Producer.Res[PutRes]) =>
          F.pure(x.isRight || x.left.exists(e => e.errors.exists(_.isLeft))),
        (x: Producer.Res[PutRes], details: RetryDetails) =>
          for {
            failedRecords <- F.fromOption(
              x.left.flatMap { e =>
                e.errors.flatMap(errors => errors.right)
              },
              new RuntimeException(
                "Failed records empty, this should never happen"
              )
            )
            _ <- logger.debug(ctx.addEncoded("retryDetails", details).context)(
              s"Failures with ${failedRecords.size} records detected, retrying failed records"
            )
            _ <- ref.update(current =>
              Producer.RetryState(
                failedRecords.map(_.record),
                current.res.fold(Some(x))(y => Some(y.combine(x)))
              )
            )
          } yield (),
        (e: Throwable, details: RetryDetails) =>
          logger.error(ctx.addEncoded("retryDetails", details).context, e)(
            "Exception when putting records, retrying"
          )
      )(ref.get.flatMap(x => _put(x.inputRecords)))
      _ <-
        if (finalRes.left.exists(e => e.errors.exists(_.right.nonEmpty))) {
          if (config.raiseOnExhaustedRetries)
            finalRes.leftTraverse(F.raiseError[Unit]).void
          else {
            logger
              .warn(ctx.context)(
                "All retries have been exhausted, and the final retry detected errors"
              )
          }
        } else F.unit
      res <- ref.modify { current =>
        val result = current.res.fold(finalRes)(x => x.combine(finalRes))
        (
          Producer.RetryState(
            current.inputRecords,
            Some(result)
          ),
          result
        )
      }
    } yield res
  }
}

object Producer {

  final private case class RetryState[A](
      inputRecords: NonEmptyList[Record],
      res: Option[Res[A]]
  )

  type Res[A] = Ior[Producer.Error, NonEmptyList[A]]
  type Errs = Ior[
    NonEmptyList[InvalidRecord],
    NonEmptyList[FailedRecord]
  ]

  /** [[kinesis4cats.logging.LogEncoder LogEncoder]] instances for the
    * [[kinesis4cats.producer.Producer]]
    *
    * @param recordLogEncoder
    * @param finiteDurationEncoder
    */
  final class LogEncoders(implicit
      val recordLogEncoder: LogEncoder[Record],
      val finiteDurationEncoder: LogEncoder[FiniteDuration],
      val retryDetailsEncoder: LogEncoder[RetryDetails]
  )

  /** Configuration for the [[kinesis4cats.producer.Producer Producer]]
    *
    * @param warnOnShardCacheMisses
    *   If true, a warning message will appear if a record was not matched with
    *   a shard in the cache
    * @param warnOnBatchFailures
    *   If true, a warning message will appear if the producer failed to produce
    *   some records in a batch
    * @param shardParallelism
    *   Determines how many shards to concurrently put batches of data to
    * @param raiseOnFailures
    *   If true, an exception will be raised if a
    *   [[kinesis4cats.producer.Producer.Error Producer.Error]] is detected in
    *   one of the batches
    * @param shardMapCacheConfig
    *   [[kinesis4cats.producer.ShardMapCache.Config ShardMapCache.Config]]
    * @param streamNameOrArn
    *   [[kinesis4cats.models.StreamNameOrArn StreamNameOrArn]] either a stream
    *   name or a stream ARN for the producer.
    * @param raiseOnExhaustedRetries
    *   If true, an exception will be raised if a
    *   [[kinesis4cats.producer.Producer.Error Producer.Error]] is detected in
    *   the final batch of retried put requests
    * @param retryPolicy
    *   [[https://github.com/etspaceman/kinesis4cats/blob/main/compat/src/main/scala/kinesis4cats/compat/retry/RetryPolicy.scala RetryPolicy]]
    *   for retrying put requests
    */
  final case class Config[F[_]](
      warnOnShardCacheMisses: Boolean,
      warnOnBatchFailures: Boolean,
      shardParallelism: Int,
      raiseOnFailures: Boolean,
      shardMapCacheConfig: ShardMapCache.Config,
      batcherConfig: Batcher.Config,
      streamNameOrArn: StreamNameOrArn,
      raiseOnExhaustedRetries: Boolean,
      retryPolicy: RetryPolicy[F]
  )

  object Config {

    /** Default configuration for the
      * [[kinesis4cats.producer.Producer.Config Producer.Config]]
      *
      * @param streamNameOrArn
      *   [[kinesis4cats.models.StreamNameOrArn StreamNameOrArn]] either a
      *   stream name or a stream ARN for the producer.
      * @return
      *   [[kinesis4cats.producer.Producer.Config Producer.Config]]
      */
    def default[F[_]](
        streamNameOrArn: StreamNameOrArn
    )(implicit F: Applicative[F]): Config[F] = Config[F](
      warnOnShardCacheMisses = true,
      warnOnBatchFailures = true,
      shardParallelism = 8,
      raiseOnFailures = false,
      shardMapCacheConfig = ShardMapCache.Config.default,
      batcherConfig = Batcher.Config.default,
      streamNameOrArn = streamNameOrArn,
      raiseOnExhaustedRetries = false,
      retryPolicy = RetryPolicies.alwaysGiveUp[F]
    )
  }

  /** Represents errors encountered when processing records for Kinesis
    *
    * @param errors
    *   [[cats.data.Ior Ior]] with the following:
    *   - left: a [[cats.data.NonEmptyList NonEmptyList]] of
    *     [[kinesis4cats.producer.Record Records]] that were too large to fit
    *     into a single Kinesis request
    *   - right: a [[cats.data.NonEmptyList NonEmptyList]] of
    *     [[kinesis4cats.producer.Producer.FailedRecord Producer.FailedRecords]],
    *     which represent records that failed to produce to Kinesis within a
    *     given batch
    */
  final case class Error(errors: Option[Errs]) extends Exception {
    private[kinesis4cats] def add(that: Error): Error = Error(
      errors.combine(that.errors)
    )

    override def getMessage: String = errors match {
      case Some(Ior.Both(a, b)) =>
        Error.invalidRecordsMessage(a) + "\n\nAND\n\n" + Error
          .putFailuresMessage(b)
      case Some(Ior.Left(a))  => Error.invalidRecordsMessage(a)
      case Some(Ior.Right(b)) => Error.putFailuresMessage(b)
      case None => s"Batcher returned no results at all, this is unexpected"
    }
  }

  object Error {
    implicit val producerErrorSemigroup: Semigroup[Error] =
      (x: Error, y: Error) => x.add(y)

    private def invalidRecordsMessage(
        records: NonEmptyList[InvalidRecord]
    ): String = {
      val prefix = s"${records.length} records were invalid."
      val recordsTooLarge = NonEmptyList
        .fromList(records.filter {
          case _: InvalidRecord.RecordTooLarge => true
          case _                               => false
        })
        .fold("")(x => s" Records too large: ${x.length}")
      val invalidPartitionKeys = NonEmptyList
        .fromList(records.filter {
          case _: InvalidRecord.InvalidPartitionKey => true
          case _                                    => false
        })
        .fold("")(x => s" Invalid partition keys: ${x.length}")
      val invalidExplicitHashKeys = NonEmptyList
        .fromList(records.filter {
          case _: InvalidRecord.InvalidExplicitHashKey => true
          case _                                       => false
        })
        .fold("")(x => s" Invalid explicit hash keys: ${x.length}")

      prefix + recordsTooLarge + invalidPartitionKeys + invalidExplicitHashKeys
    }

    private def putFailuresMessage(failures: NonEmptyList[FailedRecord]) =
      s"${failures.length} records received failures when producing to Kinesis.\n\t" +
        failures.toList
          .map(x =>
            s"Error Code: ${x.errorCode}, Error Message: ${x.erorrMessage}"
          )
          .mkString("\n\t")

    /** Create a [[kinesis4cats.producer.Producer.Error Producer.Error]] with
      * records that were too large to fit into Kinesis
      *
      * @param records
      *   a [[cats.data.NonEmptyList NonEmptyList]] of
      *   [[kinesis4cats.producer.Record Records]] that were too large to fit
      *   into a single Kinesis request
      * @return
      *   [[kinesis4cats.producer.Producer.Error Producer.Error]]
      */
    def invalidRecords(records: NonEmptyList[InvalidRecord]): Error = Error(
      Some(Ior.left(records))
    )

    /** Create a [[kinesis4cats.producer.Producer.Error Producer.Error]] with
      * records that failed during a batch put to Kinesis.
      *
      * @param records
      *   a [[cats.data.NonEmptyList NonEmptyList]] of
      *   [[kinesis4cats.producer.Producer.FailedRecord Producer.FailedRecords]],
      *   which represent records that failed to produce to Kinesis within a
      *   given batch
      * @return
      *   [[kinesis4cats.producer.Producer.Error Producer.Error]]
      */
    def putFailures(records: NonEmptyList[FailedRecord]): Error = Error(
      Some(Ior.right(records))
    )
  }

  /** Represents a record that failed to produce to Kinesis in a batch, with the
    * error code and message for the failure
    *
    * @param record
    *   [[kinesis4cats.producer.Record Record]] in the request that failed
    * @param errorCode
    *   The error code of the failure
    * @param erorrMessage
    *   The error message of the failure
    * @param requestIndex
    *   Index of record in the overarching request
    */
  final case class FailedRecord(
      record: Record,
      errorCode: String,
      erorrMessage: String,
      requestIndex: Int
  )

  /** Represents a record that was invalid per the Kinesis limits
    */
  sealed trait InvalidRecord extends Product with Serializable

  object InvalidRecord {

    /** Represents a record that was too large to put into Kinesis
      *
      * @param record
      *   Invalid [[kinesis4cats.producer.Record Record]]
      */
    final case class RecordTooLarge(record: Record) extends InvalidRecord

    /** Represents a partition key that was not within the Kinesis limits
      *
      * @param partitionKey
      *   Invalid partition key
      */
    final case class InvalidPartitionKey(partitionKey: String)
        extends InvalidRecord

    /** Represents an explicit hash key that is in an invalid format
      *
      * @param explicitHashKey
      *   Invalid hash key
      */
    final case class InvalidExplicitHashKey(explicitHashKey: Option[String])
        extends InvalidRecord
  }
}
