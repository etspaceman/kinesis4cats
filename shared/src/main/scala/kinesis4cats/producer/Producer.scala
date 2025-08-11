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

package kinesis4cats
package producer

import scala.concurrent.duration.FiniteDuration

import cats.Applicative
import cats.Eq
import cats.Semigroup
import cats.Show
import cats.data.NonEmptyList
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
  * @param encoders
  *   [[kinesis4cats.producer.Producer.LogEncoders Producer.LogEncoders]]
  * @tparam PutReq
  *   The class that represents a batch put request for the underlying client
  * @tparam PutRes
  *   The class that represents a batch put response for the underlying client
  */
abstract class Producer[F[_], PutReq, PutRes] private[kinesis4cats] (
    encoders: Producer.LogEncoders
)(implicit
    F: Async[F]
) {

  import encoders._

  def logger: StructuredLogger[F]

  def shardMapCache: ShardMapCache[F]

  def config: Producer.Config[F]

  private[kinesis4cats] lazy val batcher: Batcher = new Batcher(
    config.batcherConfig
  )

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
      records: NonEmptyList[Record],
      retrying: Boolean
  ): F[Producer.Result[PutRes]] =
    for {
      ctx <- LogContext.safe[F]
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
      batched = batcher.batch(withShards, retrying)
      res <-
        batched.batches
          .flatTraverse(batch =>
            batch.shardBatches.toList
              .map(_.records)
              .parTraverseN(config.shardParallelism) { shardBatch =>
                putImpl(asPutRequest(shardBatch))
                  .map(resp =>
                    failedRecords(shardBatch, resp)
                      .map(Producer.Result.putFailures[PutRes])
                      .fold(
                        Producer.Result.success(resp)
                      )(e => Producer.Result.success(resp) |+| e)
                  )
              }
          )
          .map(x =>
            x.foldLeft(
              Producer.Result.invalidRecords[PutRes](batched.invalid)
            ) { case (x, y) => x |+| y }
          )
    } yield res

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
    *   Producer.Result
    */
  def put(records: NonEmptyList[Record]): F[Producer.Result[PutRes]] =
    for {
      ctx <- LogContext.safe[F]
      ref <- Ref.of(
        Producer.RetryState[PutRes](records, None, retrying = false)
      )
      finalRes <- retryingOnFailuresAndAllErrors(
        config.retryPolicy,
        (x: Producer.Result[PutRes]) => F.pure(!x.hasFailed),
        (x: Producer.Result[PutRes], details: RetryDetails) =>
          for {
            failed <- F.fromOption(
              NonEmptyList.fromList(x.failed),
              new RuntimeException(
                "Failed records empty, this should never happen"
              )
            )
            _ <- logger.warn(ctx.addEncoded("retryDetails", details).context)(
              s"Failures with ${failed.length} records detected, retrying failed records"
            )
            _ <- ref.update(current =>
              Producer.RetryState(
                failed.map(_.record),
                current.res.fold(Some(x)) { currentResult =>
                  Some(
                    Producer.Result(
                      currentResult.successful ++ x.successful,
                      currentResult.invalid ++ x.invalid,
                      failed.toList // Only use failed from most recent result
                    )
                  )
                },
                retrying = true
              )
            )
          } yield (),
        (e: Throwable, details: RetryDetails) =>
          logger.warn(ctx.addEncoded("retryDetails", details).context, e)(
            "Exception when putting records, retrying"
          )
      )(ref.get.flatMap(x => _put(x.inputRecords, x.retrying)))
      _ <-
        if (finalRes.hasErrors) {
          if (config.raiseOnFailures) {
            finalRes.error.traverse(F.raiseError[Unit]).void
          } else {
            logger
              .warn(ctx.context)(
                "All retries have been exhausted, and the final retry detected errors. " +
                  "If you would like an exception to be raised in this case, set raiseOnFailures to true"
              )
          }
        } else F.unit

      res <- ref.modify { current =>
        val result = current.res.fold(finalRes)(currentResult =>
          Producer.Result(
            currentResult.successful ++ finalRes.successful,
            currentResult.invalid ++ finalRes.invalid,
            finalRes.failed // Only use failed from most recent result
          )
        )
        (
          Producer.RetryState(
            current.inputRecords,
            Some(result),
            current.retrying
          ),
          result
        )
      }
    } yield res
}

object Producer {

  final private case class RetryState[A](
      inputRecords: NonEmptyList[Record],
      res: Option[Result[A]],
      retrying: Boolean
  )

  private[kinesis4cats] final case class Result[A](
      val successful: List[A],
      val invalid: List[InvalidRecord],
      val failed: List[FailedRecord]
  ) {
    def add(that: Result[A]): Result[A] = Result(
      successful ++ that.successful,
      invalid ++ that.invalid,
      failed ++ that.failed
    )

    val hasSuccessful: Boolean = successful.nonEmpty
    val hasInvalid: Boolean = invalid.nonEmpty
    val hasFailed: Boolean = failed.nonEmpty
    val hasErrors: Boolean = hasInvalid || hasFailed
    val isSuccessful: Boolean = hasSuccessful && !hasErrors
    val isPartiallySuccessful: Boolean = hasSuccessful && hasErrors
    val error: Option[Error] =
      if (isSuccessful) None else Some(Error(invalid, failed))
  }

  object Result {

    implicit def resultEq[A](implicit eqA: Eq[A]): Eq[Result[A]] =
      Eq.by(x => (x.successful, x.invalid, x.failed))

    implicit def producerResultSemigroup[A]: Semigroup[Result[A]] =
      (x: Result[A], y: Result[A]) => x.add(y)

    /** Create a Producer.Result with records that were too large to fit into
      * Kinesis
      *
      * @param records
      *   a List of [[kinesis4cats.producer.Record Records]] that were too large
      *   to fit into a single Kinesis request
      * @return
      *   Producer.Result
      */
    def invalidRecords[A](records: List[InvalidRecord]): Result[A] =
      Result(
        Nil,
        records,
        Nil
      )

    /** Create a Producer.Result with records that failed during a batch put to
      * Kinesis.
      *
      * @param records
      *   a [[cats.data.NonEmptyList NonEmptyList]] of
      *   [[kinesis4cats.producer.Producer.FailedRecord Producer.FailedRecords]],
      *   which represent records that failed to produce to Kinesis within a
      *   given batch
      * @return
      *   Producer.Result
      */
    def putFailures[A](records: NonEmptyList[FailedRecord]): Result[A] = Result(
      Nil,
      Nil,
      records.toList
    )

    /** Create a Producer.Result with records that were successfully produced to
      * kinesis.
      *
      * @param record
      *   A, which represent a successful put result
      * @return
      *   Producer.Result
      */
    def success[A](record: A): Result[A] =
      Result(List(record), Nil, Nil)
  }

  /** [[kinesis4cats.logging.LogEncoder LogEncoder]] instances for the
    * [[kinesis4cats.producer.Producer]]
    *
    * @param recordLogEncoder
    * @param finiteDurationEncoder
    */
  final class LogEncoders(val shardMapLogEncoders: ShardMapCache.LogEncoders)(
      implicit
      val recordLogEncoder: LogEncoder[Record],
      val finiteDurationEncoder: LogEncoder[FiniteDuration],
      val retryDetailsEncoder: LogEncoder[RetryDetails]
  )

  object LogEncoders {
    val show = {
      import kinesis4cats.logging.instances.show._

      implicit val recordShow: Show[Record] = x =>
        ShowBuilder("Record")
          .add("data", x.data)
          .add("partitionKey", x.partitionKey)
          .add("explicitHashKey", x.explicitHashKey)
          .build

      new LogEncoders(ShardMapCache.LogEncoders.show)

    }
  }

  /** Configuration for the [[kinesis4cats.producer.Producer Producer]]
    *
    * @param warnOnShardCacheMisses
    *   If true, a warning message will appear if a record was not matched with
    *   a shard in the cache
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
    * @param retryPolicy
    *   [[https://github.com/etspaceman/kinesis4cats/blob/main/compat/src/main/scala/kinesis4cats/compat/retry/RetryPolicy.scala RetryPolicy]]
    *   for retrying put requests
    */
  final case class Config[F[_]](
      warnOnShardCacheMisses: Boolean,
      shardParallelism: Int,
      raiseOnFailures: Boolean,
      shardMapCacheConfig: ShardMapCache.Config,
      batcherConfig: Batcher.Config,
      streamNameOrArn: StreamNameOrArn,
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
      shardParallelism = 8,
      raiseOnFailures = false,
      shardMapCacheConfig = ShardMapCache.Config.default,
      batcherConfig = Batcher.Config.default,
      streamNameOrArn = streamNameOrArn,
      retryPolicy = RetryPolicies.alwaysGiveUp[F]
    )
  }

  /** Represents errors encountered when processing records for Kinesis
    *
    * @param invalid
    *   List of [[kinesis4cats.producer.Producer.InvalidRecord InvalidRecords]]
    * @param failed
    *   List of [[kinesis4cats.producer.Producer.FailedRecord FailedRecords]]
    */
  final case class Error(
      invalid: List[InvalidRecord],
      failed: List[FailedRecord]
  ) extends Exception {
    private[kinesis4cats] def add(that: Error): Error = Error(
      invalid ++ that.invalid,
      failed ++ that.failed
    )

    override def getMessage: String = (invalid, failed) match {
      case (Nil, Nil) =>
        s"Error captured but no invalid or failed records found. This is unexpected"
      case (i, Nil) => Error.invalidRecordsMessage(i)
      case (Nil, f) => Error.putFailuresMessage(f)
      case (i, f) =>
        Error.invalidRecordsMessage(i) +
          "\n\nAND\n\n" +
          Error.putFailuresMessage(f)
    }
  }

  object Error {
    implicit val producerErrorEq: Eq[Error] =
      Eq.by(x => (x.invalid, x.failed))

    private def invalidRecordsMessage(
        records: List[InvalidRecord]
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

    private def putFailuresMessage(failures: List[FailedRecord]) =
      s"${failures.length} records received failures when producing to Kinesis.\n\t" +
        failures
          .map(x =>
            s"Error Code: ${x.errorCode}, Error Message: ${x.errorMessage}"
          )
          .mkString("\n\t")
  }

  /** Represents a record that failed to produce to Kinesis in a batch, with the
    * error code and message for the failure
    *
    * @param record
    *   [[kinesis4cats.producer.Record Record]] in the request that failed
    * @param errorCode
    *   The error code of the failure
    * @param errorMessage
    *   The error message of the failure
    * @param requestIndex
    *   Index of record in the overarching request
    */
  final case class FailedRecord(
      record: Record,
      errorCode: String,
      errorMessage: String,
      requestIndex: Int
  )

  object FailedRecord {
    implicit val producerFailedRecordEq: Eq[FailedRecord] =
      Eq.by(x => (x.record, x.errorCode, x.errorMessage, x.requestIndex))
  }

  /** Represents a record that was invalid per the Kinesis limits
    */
  sealed trait InvalidRecord extends Product with Serializable

  object InvalidRecord {
    implicit val producerInvalidRecordEq: Eq[InvalidRecord] = {
      case (x: RecordTooLarge, y: RecordTooLarge)                 => x === y
      case (x: InvalidPartitionKey, y: InvalidPartitionKey)       => x === y
      case (x: InvalidExplicitHashKey, y: InvalidExplicitHashKey) => x === y
      case _                                                      => false
    }

    /** Represents a record that was too large to put into Kinesis
      *
      * @param record
      *   Invalid [[kinesis4cats.producer.Record Record]]
      */
    final case class RecordTooLarge(record: Record) extends InvalidRecord

    object RecordTooLarge {
      implicit val producerRecordTooLargeEq: Eq[RecordTooLarge] =
        Eq.by(_.record)
    }

    /** Represents a partition key that was not within the Kinesis limits
      *
      * @param partitionKey
      *   Invalid partition key
      */
    final case class InvalidPartitionKey(partitionKey: String)
        extends InvalidRecord

    object InvalidPartitionKey {
      implicit val producerInvalidPartitionKeyEq: Eq[InvalidPartitionKey] =
        Eq.by(_.partitionKey)
    }

    /** Represents an explicit hash key that is in an invalid format
      *
      * @param explicitHashKey
      *   Invalid hash key
      */
    final case class InvalidExplicitHashKey(explicitHashKey: Option[String])
        extends InvalidRecord

    object InvalidExplicitHashKey {
      implicit val producerInvalidExplicitHashKeyEq
          : Eq[InvalidExplicitHashKey] =
        Eq.by(_.explicitHashKey)
    }
  }
}
