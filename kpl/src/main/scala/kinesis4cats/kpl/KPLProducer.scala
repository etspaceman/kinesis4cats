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
package kpl

import scala.concurrent.duration._
import scala.jdk.CollectionConverters._

import java.nio.ByteBuffer

import cats.Eq
import cats.Show
import cats.effect.{Async, Ref, Resource}
import cats.syntax.all._
import com.amazonaws.services.kinesis.producer._
import com.amazonaws.services.schemaregistry.common.Schema
import org.typelevel.log4cats.StructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import kinesis4cats.compat.retry.RetryPolicies._
import kinesis4cats.compat.retry._
import kinesis4cats.kpl.syntax.listenableFuture._
import kinesis4cats.logging.{LogContext, LogEncoder}

/** Scala wrapper for the
  * [[https://github.com/awslabs/amazon-kinesis-producer/blob/master/java/amazon-kinesis-producer/src/main/java/com/amazonaws/services/kinesis/producer/KinesisProducer.java KinesisProducer]]
  * All of the methods in this class are a mirror of the wrapped client.
  *
  * @param client
  *   [[https://github.com/awslabs/amazon-kinesis-producer/blob/master/java/amazon-kinesis-producer/src/main/java/com/amazonaws/services/kinesis/producer/KinesisProducer.java KinesisProducer]]
  * @param F
  *   F with an [[cats.effect.Async Async]] instance
  */
class KPLProducer[F[_]] private (
    client: KinesisProducer,
    logger: StructuredLogger[F],
    val state: Ref[F, KPLProducer.State],
    LE: KPLProducer.LogEncoders
)(implicit
    F: Async[F]
) {

  import LE._

  /** Put data onto a Kinesis stream
    *
    * @param userRecord
    *   [[https://github.com/awslabs/amazon-kinesis-producer/blob/master/java/amazon-kinesis-producer/src/main/java/com/amazonaws/services/kinesis/producer/UserRecord.java UserRecord]]
    * @return
    *   F of
    *   [[https://github.com/awslabs/amazon-kinesis-producer/blob/master/java/amazon-kinesis-producer/src/main/java/com/amazonaws/services/kinesis/producer/UserRecordResult.java UserRecordResult]]
    */
  def put(userRecord: UserRecord): F[UserRecordResult] = {
    val ctx = LogContext()
    for {
      s <- state.get
      _ <-
        if (s === KPLProducer.State.ShuttingDown)
          F.raiseError(ShuttingDownException)
        else F.unit
      _ <- logger.debug(ctx.context)("Received put request")
      _ <- logger.trace(ctx.addEncoded("userRecord", userRecord).context)(
        "Logging put request parameters"
      )
      result <- F.fromListenableFuture(
        F.delay(client.addUserRecord(userRecord))
      )
      _ <- logger.debug(ctx.context)("Successfully processed put request")
      _ <- logger.trace(ctx.addEncoded("userRecordResult", result).context)(
        "Logging put result"
      )
    } yield result
  }

  /** Put data onto a Kinesis stream
    *
    * @param stream
    *   Stream name to produce to
    * @param partitionKey
    *   Key to partition the data by. All data for the same partition key is
    *   sent to the same shard.
    * @param explicitHashKey
    *   Optional hash key, used to explicitly set the shard and override the
    *   partitionKey hash.
    * @param data
    *   [[java.nio.ByteBuffer]] representing the data to be produced
    * @return
    *   F of
    *   [[https://github.com/awslabs/amazon-kinesis-producer/blob/master/java/amazon-kinesis-producer/src/main/java/com/amazonaws/services/kinesis/producer/UserRecordResult.java UserRecordResult]]
    */
  def put(
      stream: String,
      partitionKey: String,
      explicitHashKey: Option[String],
      data: ByteBuffer
  ): F[UserRecordResult] = {
    val ctx = LogContext()
    for {
      s <- state.get
      _ <-
        if (s === KPLProducer.State.ShuttingDown)
          F.raiseError(ShuttingDownException)
        else F.unit
      _ <- logger.debug(ctx.context)("Received put request")
      _ <- logger.trace(
        ctx
          .addEncoded("stream", stream)
          .addEncoded("partitionKey", partitionKey)
          .addEncoded("explicitHashKey", explicitHashKey.orNull)
          .addEncoded("data", data)
          .context
      )(
        "Logging put request parameters"
      )
      result <- F.fromListenableFuture(
        F.delay(
          client.addUserRecord(
            stream,
            partitionKey,
            explicitHashKey.orNull,
            data
          )
        )
      )
      _ <- logger.debug(ctx.context)("Successfully processed put request")
      _ <- logger.trace(ctx.addEncoded("userRecordResult", result).context)(
        "Logging put result"
      )
    } yield result
  }

  /** Put data onto a Kinesis stream
    *
    * @param stream
    *   Stream name to produce to
    * @param partitionKey
    *   Key to partition the data by. All data for the same partition key is
    *   sent to the same shard.
    * @param explicitHashKey
    *   Optional hash key, used to explicitly set the shard and override the
    *   partitionKey hash.
    * @param data
    *   [[java.nio.ByteBuffer]] representing the data to be produced
    * @param schema
    *   [[https://github.com/awslabs/aws-glue-schema-registry/blob/master/common/src/main/java/com/amazonaws/services/schemaregistry/common/Schema.java Schema]]
    *   representing a glue schema for this event
    * @return
    *   F of
    *   [[https://github.com/awslabs/amazon-kinesis-producer/blob/master/java/amazon-kinesis-producer/src/main/java/com/amazonaws/services/kinesis/producer/UserRecordResult.java UserRecordResult]]
    */
  def put(
      stream: String,
      partitionKey: String,
      explicitHashKey: Option[String],
      data: ByteBuffer,
      schema: Schema
  ): F[UserRecordResult] = {
    val ctx = LogContext()
    for {
      s <- state.get
      _ <-
        if (s === KPLProducer.State.ShuttingDown)
          F.raiseError(ShuttingDownException)
        else F.unit
      _ <- logger.debug(ctx.context)("Received put request")
      _ <- logger.trace(
        ctx
          .addEncoded("stream", stream)
          .addEncoded("partitionKey", partitionKey)
          .addEncoded("explicitHashKey", explicitHashKey.orNull)
          .addEncoded("data", data)
          .addEncoded("schema", schema)
          .context
      )(
        "Logging put request parameters"
      )
      result <- F.fromListenableFuture(
        F.delay(
          client.addUserRecord(
            stream,
            partitionKey,
            explicitHashKey.orNull,
            data,
            schema
          )
        )
      )
      _ <- logger.debug(ctx.context)("Successfully processed put request")
      _ <- logger.trace(ctx.addEncoded("userRecordResult", result).context)(
        "Logging put result"
      )
    } yield result
  }

  /** Get the number or records pending production.
    *
    * @return
    *   F of pending record count
    */
  def getOutstandingRecordsCount(): F[Int] = {
    val ctx = LogContext()
    for {
      _ <- logger.debug(ctx.context)(
        "Received getOutstandingRecordsCount request"
      )
      result <- F.delay(client.getOutstandingRecordsCount())
      _ <- logger.debug(ctx.context)(
        "Successfully processed getOutstandingRecordsCount request"
      )
    } yield result
  }

  /** Get the time in millis for the oldest record currently pending production.
    *
    * @return
    *   F of millis
    */
  def getOldestRecordTimeInMillis(): F[Long] = {
    val ctx = LogContext()
    for {
      _ <- logger.debug(ctx.context)(
        "Received getOldestRecordTimeInMillis request"
      )
      result <- F.delay(client.getOldestRecordTimeInMillis())
      _ <- logger.debug(ctx.context)(
        "Successfully processed getOldestRecordTimeInMillis request"
      )
    } yield result
  }

  /** Get the time in millis for the oldest record currently pending production.
    * This is a blocking operation.
    *
    * @param metricName
    *   Name of metric
    * @param windowSeconds
    *   Fetch data from the last N seconds.
    * @return
    *   F of
    *   [[https://github.com/awslabs/amazon-kinesis-producer/blob/master/java/amazon-kinesis-producer/src/main/java/com/amazonaws/services/kinesis/producer/Metric.java Metric]]
    */
  def getMetrics(metricName: String, windowSeconds: Int): F[List[Metric]] = {
    val ctx = LogContext()
      .addEncoded("metricName", metricName)
      .addEncoded("windowSeconds", windowSeconds)

    for {
      _ <- logger.debug(ctx.context)(
        "Received getMetrics request"
      )
      result <- F
        .interruptibleMany(client.getMetrics(metricName, windowSeconds))
        .map(_.asScala.toList)
      _ <- logger.debug(ctx.context)(
        "Successfully processed getMetrics request"
      )
    } yield result
  }

  /** Get the time in millis for the oldest record currently pending production.
    * This is a blocking operation.
    *
    * @param metricName
    *   Name of metric
    * @return
    *   F of a List of
    *   [[https://github.com/awslabs/amazon-kinesis-producer/blob/master/java/amazon-kinesis-producer/src/main/java/com/amazonaws/services/kinesis/producer/Metric.java Metric]]
    */
  def getMetrics(metricName: String): F[List[Metric]] = {
    val ctx = LogContext()
      .addEncoded("metricName", metricName)

    for {
      _ <- logger.debug(ctx.context)(
        "Received getMetrics request"
      )
      result <- F
        .interruptibleMany(client.getMetrics(metricName))
        .map(_.asScala.toList)
      _ <- logger.debug(ctx.context)(
        "Successfully processed getMetrics request"
      )
    } yield result
  }

  /** Get the time in millis for the oldest record currently pending production.
    * This is a blocking operation.
    *
    * @param windowSeconds
    *   Fetch data from the last N seconds.
    * @return
    *   F of a List of
    *   [[https://github.com/awslabs/amazon-kinesis-producer/blob/master/java/amazon-kinesis-producer/src/main/java/com/amazonaws/services/kinesis/producer/Metric.java Metric]]
    */
  def getMetrics(windowSeconds: Int): F[List[Metric]] = {
    val ctx = LogContext()
      .addEncoded("windowSeconds", windowSeconds)

    for {
      _ <- logger.debug(ctx.context)(
        "Received getMetrics request"
      )
      result <- F
        .interruptibleMany(client.getMetrics(windowSeconds))
        .map(_.asScala.toList)
      _ <- logger.debug(ctx.context)(
        "Successfully processed getMetrics request"
      )
    } yield result
  }

  /** Get the time in millis for the oldest record currently pending production.
    * This is a blocking operation.
    *
    * @return
    *   F of a List of
    *   [[https://github.com/awslabs/amazon-kinesis-producer/blob/master/java/amazon-kinesis-producer/src/main/java/com/amazonaws/services/kinesis/producer/Metric.java Metric]]
    */
  def getMetrics(): F[List[Metric]] = {
    val ctx = LogContext()

    for {
      _ <- logger.debug(ctx.context)(
        "Received getMetrics request"
      )
      result <- F
        .interruptibleMany(client.getMetrics())
        .map(_.asScala.toList)
      _ <- logger.debug(ctx.context)(
        "Successfully processed getMetrics request"
      )
    } yield result
  }

  /** Instruct the producer to send pending records. Returns immediately without
    * blocking.
    *
    * @param stream
    *   Stream name to flush records for
    * @return
    *   Unit
    */
  def flush(stream: String): F[Unit] = {
    val ctx = LogContext().addEncoded("stream", stream)

    for {
      _ <- logger.debug(ctx.context)(
        "Received flush request"
      )
      result <- F.delay(client.flush(stream))
      _ <- logger.debug(ctx.context)(
        "Successfully processed flush request"
      )
    } yield result
  }

  /** Instruct the producer to send pending records. Returns immediately without
    * blocking.
    *
    * @return
    *   Unit
    */
  def flush(): F[Unit] = {
    val ctx = LogContext()

    for {
      _ <- logger.debug(ctx.context)(
        "Received flush request"
      )
      result <- F.delay(client.flush())
      _ <- logger.debug(ctx.context)(
        "Successfully processed flush request"
      )
    } yield result
  }

  /** Instruct the producer to send pending records. Blocks until all pending
    * data is flushed. Interruptible.
    *
    * @return
    *   Unit
    */
  def flushSync(): F[Unit] = {
    val ctx = LogContext()

    for {
      _ <- logger.debug(ctx.context)(
        "Received flushSync request"
      )
      result <- F.interruptibleMany(client.flushSync())
      _ <- logger.debug(ctx.context)(
        "Successfully processed flushSync request"
      )
    } yield result
  }

  /** Destroys the producer. Blocking and uncancellable.
    *
    * @return
    *   Unit
    */
  private[kinesis4cats] def destroy(): F[Unit] = {
    val ctx = LogContext()

    for {
      _ <- logger.debug(ctx.context)(
        "Received destroy request"
      )
      result <- F.blocking(client.destroy())
      _ <- logger.debug(ctx.context)(
        "Successfully processed destroy request"
      )
    } yield result
  }

  private[kinesis4cats] def gracefulShutdown(
      flushAttempts: Int,
      flushInterval: FiniteDuration
  ): F[Unit] = {
    val policy = constantDelay(flushInterval).join(limitRetries(flushAttempts))
    val ctx = LogContext()
    for {
      _ <- logger.info(ctx.context)("Starting graceful KPL shutdown")
      _ <- retryingOnFailures(
        policy,
        (_: Unit) => getOutstandingRecordsCount().map(_ === 0),
        (_: Unit, details: RetryDetails) =>
          logger.info(ctx.addEncoded("retryDetails", details).context)(
            "Still outstanding KPL records."
          )
      )(flush())
      outstanding <- getOutstandingRecordsCount()
      _ <-
        if (outstanding =!= 0)
          logger.warn(ctx.addEncoded("outstandingRecord", outstanding).context)(
            "Still outstanding KPL records after all retries are exhausted. Killing KPL, some records will not be produced."
          )
        else logger.info(ctx.context)("Graceful shutdown completed")
      _ <- destroy()
    } yield ()

  }
}

object KPLProducer {

  /** Constructor for the [[kinesis4cats.kpl.KPLProducer KPLProducer]]
    *
    * @param config
    *   [[https://github.com/awslabs/amazon-kinesis-producer/blob/master/java/amazon-kinesis-producer/src/main/java/com/amazonaws/services/kinesis/producer/KinesisProducerConfiguration.java KinesisProducerConfiguration]]
    * @param gracefulShutdownFlushAttempts
    *   How many times to execute flush() and wait for the KPL's buffer to clear
    *   before shutting down
    * @param gracefulShutdownFlushInterval
    *   Duration between flush() attempts during the graceful shutdown
    * @param encoders
    *   [[kinesis4cats.kpl.KPLProducer.LogEncoders KPLProducer.LogEncoders]].
    *   Default to show instances
    * @param F
    *   [[cats.effect.Async Async]]
    * @return
    *   [[cats.effect.Resource Resource]] containing the
    *   [[kinesis4cats.kpl.KPLProducer KPLProducer]]
    */
  def apply[F[_]](
      config: KinesisProducerConfiguration = new KinesisProducerConfiguration(),
      gracefulShutdownFlushAttempts: Int = 5,
      gracefulShutdownFlushInterval: FiniteDuration = 500.millis,
      encoders: LogEncoders = LogEncoders.show
  )(implicit
      F: Async[F]
  ): Resource[F, KPLProducer[F]] =
    Resource.make[F, KPLProducer[F]](
      for {
        client <- F.delay(new KinesisProducer(config))
        logger <- Slf4jLogger.create[F]
        state <- Ref.of[F, State](State.Up)
      } yield new KPLProducer(client, logger, state, encoders)
    ) { x =>
      for {
        _ <- x.state.set(State.ShuttingDown)
        _ <- x.gracefulShutdown(
          gracefulShutdownFlushAttempts,
          gracefulShutdownFlushInterval
        )
      } yield ()
    }

  /** Helper class containing required
    * [[kinesis4cats.logging.LogEncoder LogEncoders]] for the
    * [[kinesis4cats.kpl.KPLProducer KPLProducer]]
    */
  final class LogEncoders(implicit
      val userRecordLogEncoder: LogEncoder[UserRecord],
      val userRecordResultLogEncoder: LogEncoder[UserRecordResult],
      val schemaLogEncoder: LogEncoder[Schema],
      val retryDetailsLogEncoder: LogEncoder[RetryDetails]
  )

  object LogEncoders {
    val show = {
      import kinesis4cats.logging.instances.show._

      implicit val attemptShow: Show[Attempt] = x =>
        ShowBuilder("Attempt")
          .add("delay", x.getDelay())
          .add("duration", x.getDuration())
          .add("errorCode", x.getErrorCode())
          .add("errorMessage", x.getErrorMessage())
          .build

      implicit val schemaShow: Show[Schema] = x =>
        ShowBuilder("Schema")
          .add("dataFormat", x.getDataFormat())
          .add("schemaDefinition", x.getSchemaDefinition())
          .add("schemaName", x.getSchemaName())
          .build

      implicit val userRecordShow: Show[UserRecord] = x =>
        ShowBuilder("UserRecord")
          .add("data", x.getData())
          .add("partitionKey", x.getPartitionKey())
          .add("explicitHashKey", x.getExplicitHashKey())
          .add("schema", x.getSchema())
          .add("streamName", x.getStreamName())
          .build

      implicit val userRecordResultShow: Show[UserRecordResult] = x =>
        ShowBuilder("UserRecordResult")
          .add("isSuccessful", x.isSuccessful())
          .add("attempts", x.getAttempts())
          .add("sequenceNumber", x.getSequenceNumber())
          .add("shardId", x.getShardId())
          .build

      new LogEncoders()
    }
  }

  /** Tracks the [[kinesis4cats.kpl.KPLProducer KPLProducer]] current state.
    */
  sealed trait State

  object State {
    case object Up extends State
    case object ShuttingDown extends State

    implicit val kplProducerStateEq: Eq[State] =
      Eq.fromUniversalEquals
  }
}
