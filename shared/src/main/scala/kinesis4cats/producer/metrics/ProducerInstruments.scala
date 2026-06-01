/*
 * Copyright 2023-2026 etspaceman
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
package metrics

import scala.concurrent.duration._

import cats.Applicative
import cats.Monad
import cats.syntax.all._
import org.typelevel.otel4s.Attribute
import org.typelevel.otel4s.metrics.Counter
import org.typelevel.otel4s.metrics.Histogram
import org.typelevel.otel4s.metrics.Meter

import kinesis4cats.models.ShardId
import kinesis4cats.models.StreamNameOrArn

/** Instruments a [[kinesis4cats.producer.Producer Producer]] with metrics via
  * [[https://opentelemetry.io/ OpenTelemetry]].
  */
private[kinesis4cats] final class ProducerInstruments[F[_]] private (
    userRecordsReceived: Counter[F, Long],
    userRecordsBytes: Counter[F, Long],
    kinesisRecordsPut: Counter[F, Long],
    kinesisRecordsBytes: Counter[F, Long],
    requestDuration: Histogram[F, Double],
    retries: Histogram[F, Long],
    errors: Counter[F, Long]
)(implicit F: Applicative[F]) {
  import ProducerInstruments._

  /** Records what the caller submitted; emit once per `put`, not on retries. */
  def recordReceived(
      records: Long,
      bytes: Long,
      stream: StreamNameOrArn
  ): F[Unit] = {
    val attrs = streamAttrs(stream)
    userRecordsReceived.add(records, attrs) *> userRecordsBytes.add(
      bytes,
      attrs
    )
  }

  /** Records one shard-batch put: record count, byte size, and latency. */
  def recordShardPut(
      count: Long,
      bytes: Long,
      duration: FiniteDuration,
      stream: StreamNameOrArn,
      shard: ShardId
  ): F[Unit] = {
    val attrs = shardAttrs(stream, shard)
    kinesisRecordsPut.add(count, attrs) *>
      kinesisRecordsBytes.add(bytes, attrs) *>
      requestDuration.record(duration.toUnit(SECONDS), attrs)
  }

  /** Records the retries-so-far at a retry step. */
  def recordRetries(retriesSoFar: Long, stream: StreamNameOrArn): F[Unit] =
    retries.record(retriesSoFar, streamAttrs(stream))

  /** Records error counts keyed by error code (empty map records nothing). */
  def recordErrors(
      byCode: Map[String, Long],
      stream: StreamNameOrArn
  ): F[Unit] =
    byCode.toList.traverse_ { case (code, count) =>
      errors.add(count, errorAttrs(stream, code))
    }
}

private[kinesis4cats] object ProducerInstruments {

  /** OTel instrumentation scope name — identifies kinesis4cats as the emitting
    * library, independent of the user-configurable metric name prefix.
    */
  private[kinesis4cats] val instrumentationScope = "kinesis4cats"

  /** Default prefix for metric names; overridable per producer. */
  private[kinesis4cats] val defaultNamespace = "kinesis4cats.producer"

  private def streamValue(stream: StreamNameOrArn): String =
    stream.streamName
      .orElse(stream.streamArn.map(_.streamArn))
      .getOrElse("")

  private def streamAttrs(stream: StreamNameOrArn): List[Attribute[_]] =
    List(Attribute("stream.name", streamValue(stream)))

  private def shardAttrs(
      stream: StreamNameOrArn,
      shard: ShardId
  ): List[Attribute[_]] =
    List(
      Attribute("stream.name", streamValue(stream)),
      Attribute("shard.id", shard.shardId)
    )

  private def errorAttrs(
      stream: StreamNameOrArn,
      code: String
  ): List[Attribute[_]] =
    List(
      Attribute("stream.name", streamValue(stream)),
      Attribute("error.code", code)
    )

  /** Builds the instruments from a
    * [[org.typelevel.otel4s.metrics.Meter Meter]].
    */
  def fromMeter[F[_]: Monad](
      meter: Meter[F],
      namespace: String
  ): F[ProducerInstruments[F]] =
    for {
      userRecordsReceived <- meter
        .counter[Long](s"$namespace.user_records.received")
        .withUnit("{record}")
        .withDescription("User records received by the producer")
        .create
      userRecordsBytes <- meter
        .counter[Long](s"$namespace.user_records.bytes")
        .withUnit("By")
        .withDescription("Payload bytes of user records received")
        .create
      kinesisRecordsPut <- meter
        .counter[Long](s"$namespace.kinesis_records.put")
        .withUnit("{record}")
        .withDescription("Records sent to Kinesis in shard batches")
        .create
      kinesisRecordsBytes <- meter
        .counter[Long](s"$namespace.kinesis_records.bytes")
        .withUnit("By")
        .withDescription("Payload bytes sent to Kinesis in shard batches")
        .create
      requestDuration <- meter
        .histogram[Double](s"$namespace.request.duration")
        .withUnit("s")
        .withDescription("Latency of a single Kinesis put request")
        .create
      retries <- meter
        .histogram[Long](s"$namespace.retries")
        .withUnit("{retry}")
        .withDescription("Retries performed for a put")
        .create
      errors <- meter
        .counter[Long](s"$namespace.errors")
        .withUnit("{error}")
        .withDescription("Failed and invalid records, keyed by error.code")
        .create
    } yield new ProducerInstruments[F](
      userRecordsReceived,
      userRecordsBytes,
      kinesisRecordsPut,
      kinesisRecordsBytes,
      requestDuration,
      retries,
      errors
    )

  /** No-op instruments; records nothing. */
  def noop[F[_]: Applicative]: ProducerInstruments[F] =
    new ProducerInstruments[F](
      Counter.noop[F, Long],
      Counter.noop[F, Long],
      Counter.noop[F, Long],
      Counter.noop[F, Long],
      Histogram.noop[F, Double],
      Histogram.noop[F, Long],
      Counter.noop[F, Long]
    )
}
