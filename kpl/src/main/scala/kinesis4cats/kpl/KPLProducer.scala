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

package kinesis4cats.kpl

import scala.jdk.CollectionConverters._

import java.nio.ByteBuffer

import cats.Eq
import cats.effect.{Async, Ref, Resource}
import cats.syntax.all._
import com.amazonaws.services.kinesis.producer._
import com.amazonaws.services.schemaregistry.common.Schema
import org.typelevel.log4cats.StructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

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
    val state: Ref[F, KPLProducer.State]
)(implicit
    F: Async[F],
    LE: KPLProducer.LogEncoders
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
    * data is flushed. Uncancellable.
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
      result <- F.blocking(client.flushSync())
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
      result <- F.delay(client.flush())
      _ <- logger.debug(ctx.context)(
        "Successfully processed destroy request"
      )
    } yield result
  }
}

object KPLProducer {

  /** Constructor for the [[kinesis4cats.kpl.KPLProducer KPLProducer]]
    *
    * @param config
    *   [[https://github.com/awslabs/amazon-kinesis-producer/blob/master/java/amazon-kinesis-producer/src/main/java/com/amazonaws/services/kinesis/producer/KinesisProducerConfiguration.java KinesisProducerConfiguration]]
    * @param F
    *   [[cats.effect.Async Async]]
    * @return
    *   [[cats.effect.Resource Resource]] containing the
    *   [[kinesis4cats.kpl.KPLProducer KPLProducer]]
    */
  def apply[F[_]](
      config: KinesisProducerConfiguration = new KinesisProducerConfiguration()
  )(implicit
      F: Async[F],
      LE: LogEncoders
  ): Resource[F, KPLProducer[F]] =
    Resource.make[F, KPLProducer[F]](
      for {
        client <- F.delay(new KinesisProducer(config))
        logger <- Slf4jLogger.create[F]
        state <- Ref.of[F, State](State.Up)
      } yield new KPLProducer(client, logger, state)
    ) { x =>
      for {
        _ <- x.state.set(State.ShuttingDown)
        _ <- x.flushSync()
        _ <- x.destroy()
      } yield ()
    }

  /** Helper class containing required
    * [[kinesis4cats.logging.LogEncoder LogEncoders]] for the
    * [[kinesis4cats.kpl.KPLProducer KPLProducer]]
    */
  final class LogEncoders(implicit
      val userRecordLogEncoder: LogEncoder[UserRecord],
      val userRecordResultLogEncoder: LogEncoder[UserRecordResult],
      val schemaLogEncoder: LogEncoder[Schema]
  )

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
