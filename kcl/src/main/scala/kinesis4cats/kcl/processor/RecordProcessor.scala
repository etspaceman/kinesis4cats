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

package kinesis4cats.kcl
package processor

import scala.jdk.CollectionConverters._

import cats.effect._
import cats.effect.std.Dispatcher
import cats.effect.syntax.all._
import cats.syntax.all._
import org.typelevel.log4cats.StructuredLogger
import retry.RetryPolicies._
import retry._
import software.amazon.kinesis.lifecycle.events._
import software.amazon.kinesis.processor._
import software.amazon.kinesis.retrieval.kpl.ExtendedSequenceNumber

import kinesis4cats.logging.LogContext

/** An implementation of the
  * [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/processor/ShardRecordProcessor.java ShardRecordProcessor]]
  * interface, with some additional logic for raising exceptions, processing +
  * committing records and logging results.
  *
  * @param config
  *   [[kinesis4cats.kcl.processor.RecordProcessorConfig RecordProcessorConfig]]
  *   instance
  * @param dispatcher
  *   [[cats.effect.std.Dispatcher Dispatcher]] instance, for running effects
  * @param lastRecordDeferred
  *   [[cats.effect.Deferred Deferred]] instance, for handling the shard-end
  *   routine
  * @param state
  *   [[cats.effect.Ref Ref]] that tracks the current state, via
  *   [[kinesis4cats.kcl.processor.RecordProcessorState RecordProcessorState]]
  * @param deferredException
  *   [[cats.effect.Deferred Deferred]] instance, for handling exceptions
  * @param logger
  *   [[org.typelevel.log4cats.StructuredLogger StructuredLogger]] instance, for
  *   logging
  * @param raiseOnError
  *   Whether the [[kinesis4cats.kcl.processor.RecordProcessor RecordProcessor]]
  *   should raise exceptions or simply log them.
  * @param cb
  *   Function to process
  *   [[kinesis4cats.kcl.CommittableRecord CommittableRecords]] received from
  *   Kinesis
  * @param F
  *   [[cats.effect.Async Async]] instance
  * @param encoders
  *   [[kinesis4cats.kcl.processor.RecordProcessorLogEncoders RecordProcessorLogEncoders]]
  *   for encoding structured logs
  */
class RecordProcessor[F[_]] private[kinesis4cats] (
    config: RecordProcessorConfig,
    dispatcher: Dispatcher[F],
    val lastRecordDeferred: Deferred[F, Unit],
    val state: Ref[F, RecordProcessorState],
    val deferredException: Deferred[F, Throwable],
    logger: StructuredLogger[F],
    raiseOnError: Boolean
)(cb: List[CommittableRecord[F]] => F[Unit])(implicit
    F: Async[F],
    encoders: RecordProcessorLogEncoders
) extends ShardRecordProcessor {

  import encoders._

  private var shardId: String = _ // scalafix:ok
  private var extendedSequenceNumber: ExtendedSequenceNumber = _ // scalafix:ok

  def getShardId: String = shardId
  def getExtendedSequenceNumber: ExtendedSequenceNumber = extendedSequenceNumber

  override def initialize(initializationInput: InitializationInput): Unit = {
    val ctx = LogContext().addEncoded(
      "initializationInput",
      initializationInput
    )
    dispatcher.unsafeRunSync(
      for {
        _ <- logger.info(ctx.context)("Initializing RecordProcessor")
        _ <- F.delay(this.shardId = initializationInput.shardId())
        _ <- F.delay(this.extendedSequenceNumber =
          initializationInput.extendedSequenceNumber()
        )
        _ <- state.set(RecordProcessorState.Initialized)
        _ <- logger.info(ctx.context)("Initialization complete")
      } yield ()
    )
  }

  override def processRecords(
      processRecordsInput: ProcessRecordsInput
  ): Unit = {

    def logCommitError(
        error: Throwable,
        details: RetryDetails,
        ctx: LogContext
    ): F[Unit] =
      logger.error(ctx.addEncoded("retryDetails", details).context, error)(
        "Error checkpointing, retrying."
      )

    val ctx = LogContext().addEncoded(
      "processRecordsInput",
      processRecordsInput
    ) + ("shardId" -> shardId)

    dispatcher.unsafeRunSync(
      F.attempt(
        for {
          _ <- logger.debug(ctx.context)("Received records to process")
          _ <- logger.trace(
            ctx
              .addEncoded(
                "records",
                processRecordsInput.records().asScala.toList
              )
              .context
          )("Logging received data records")
          _ <- state.set(RecordProcessorState.Processing)
          batch = processRecordsInput
            .records()
            .asScala
            .toList
            .map(x =>
              CommittableRecord(
                shardId,
                extendedSequenceNumber,
                processRecordsInput.millisBehindLatest(),
                x,
                this,
                processRecordsInput.checkpointer(),
                lastRecordDeferred
              )
            )
          records =
            if (processRecordsInput.isAtShardEnd)
              batch match {
                case head :+ last => head :+ last.copy(isLastInShard = true)
                case _            => Nil
              }
            else batch
          _ <- logger.debug(ctx.context)("Starting user-defined callback")
          _ <- cb(records)
          _ <- logger.debug(ctx.context)("Callback complete, checkpointing")
          _ <- retryingOnAllErrors(
            limitRetries(config.checkpointRetries)
              .join(constantDelay(config.checkpointRetryInterval)),
            (err, details) => logCommitError(err, details, ctx)
          )(records.max.checkpoint)
        } yield ()
      ).flatMap {
        case Left(error) if (raiseOnError) =>
          logger.error(ctx.context, error)(
            "Exception raised in processRecords. Error will be raised and the consumer will be shutdown."
          ) >>
            deferredException.complete(error).void
        case Left(error) =>
          logger.error(ctx.context, error)(
            "Exception raised in processRecords and raiseOnError is set to false. " +
              "The behavior of the KCL in these instances is to continue processing records. This may result in data loss. If this " +
              "is not desired, set raiseOnError to true"
          )
        case Right(_) =>
          logger.debug(ctx.context)("Records were successfully processed")
      }
    )
  }

  override def leaseLost(leaseLostInput: LeaseLostInput): Unit = {
    val ctx = LogContext() + ("shardId" -> shardId)
    dispatcher.unsafeRunSync(
      for {
        _ <- logger.warn(ctx.context)("Received lease-lost event")
        _ <- state.set(RecordProcessorState.LeaseLost)
      } yield ()
    )
  }

  override def shardEnded(shardEndedInput: ShardEndedInput): Unit = {
    val ctx = LogContext() + ("shardId" -> shardId)
    def logCommitError(
        error: Throwable,
        details: RetryDetails,
        ctx: LogContext
    ): F[Unit] =
      logger.error(ctx.addEncoded("retryDetails", details).context, error)(
        "Error checkpointing, retrying."
      )

    dispatcher.unsafeRunSync(
      for {
        _ <- logger.info(ctx.context)(
          "Received shard-ended event. Waiting for all data in the shard to be processed and committed."
        )
        _ <- state.set(RecordProcessorState.ShardEnded)
        _ <- config.shardEndTimeout.fold(lastRecordDeferred.get)(x =>
          lastRecordDeferred.get.timeout(x).attempt.flatMap {
            case Left(error) if raiseOnError =>
              logger.error(ctx.context, error)(
                "Error waiting for all data in the shard to be processed and committed. " +
                  "Error will be raised and the consumer will be shutdown."
              ) >> deferredException.complete(error).void
            case Left(error) =>
              logger.error(ctx.context, error)(
                "Error waiting for all data in the shard to be processed and committed, and raiseOnError " +
                  "set to false. This can result in data loss. If this is not desireable, do not set the shardEndTimeout value " +
                  "or set raiseOnError to true"
              )
            case _ => F.unit
          }
        )
        _ <-
          if (config.autoCommit)
            retryingOnAllErrors(
              limitRetries(config.checkpointRetries)
                .join(constantDelay(config.checkpointRetryInterval)),
              (error, details) => logCommitError(error, details, ctx)
            )(F.interruptibleMany(shardEndedInput.checkpointer().checkpoint()))
          else F.unit
      } yield ()
    )
  }

  override def shutdownRequested(
      shutdownRequestedInput: ShutdownRequestedInput
  ): Unit = {
    val ctx = LogContext() + ("shardId" -> shardId)
    dispatcher.unsafeRunSync(
      for {
        _ <- logger.warn(ctx.context)("Received shutdown request")
        _ <- state.set(RecordProcessorState.Shutdown)
      } yield ()
    )
  }
}
