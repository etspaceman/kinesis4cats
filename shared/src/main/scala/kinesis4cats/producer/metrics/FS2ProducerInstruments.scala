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
import org.typelevel.otel4s.metrics.Counter
import org.typelevel.otel4s.metrics.Histogram
import org.typelevel.otel4s.metrics.Meter
import org.typelevel.otel4s.metrics.UpDownCounter

import kinesis4cats.models.StreamNameOrArn

/** Instruments the FS2 buffering path of a
  * [[kinesis4cats.producer.fs2.FS2Producer FS2Producer]] with metrics via
  * [[https://opentelemetry.io/ OpenTelemetry]].
  */
private[kinesis4cats] final class FS2ProducerInstruments[F[_]] private (
    bufferPending: UpDownCounter[F, Long],
    bufferTime: Histogram[F, Double],
    bufferDropped: Counter[F, Long]
)(implicit F: Applicative[F]) {
  import MetricAttributes._

  /** A record entered the buffer; bumps the outstanding count. */
  def recordEnqueued(stream: StreamNameOrArn): F[Unit] =
    bufferPending.inc(streamAttrs(stream))

  /** A record left the buffer; drops the outstanding count and records how long
    * it waited.
    */
  def recordDequeued(
      waited: FiniteDuration,
      stream: StreamNameOrArn
  ): F[Unit] = {
    val attrs = streamAttrs(stream)
    bufferPending.dec(attrs) *> bufferTime.record(waited.toUnit(SECONDS), attrs)
  }

  /** A record was rejected by the buffer (queue full or post-shutdown). */
  def recordDropped(stream: StreamNameOrArn, reason: String): F[Unit] =
    bufferDropped.add(1L, droppedAttrs(stream, reason))
}

private[kinesis4cats] object FS2ProducerInstruments {

  /** `reason` attribute value for a record dropped because the queue was full. */
  val queueFullReason = "queue_full"

  /** `reason` attribute value for a record dropped after shutdown. */
  val shutdownReason = "shutdown"

  /** Builds the instruments from a
    * [[org.typelevel.otel4s.metrics.Meter Meter]].
    */
  def fromMeter[F[_]: Monad](
      meter: Meter[F],
      namespace: String
  ): F[FS2ProducerInstruments[F]] =
    for {
      bufferPending <- meter
        .upDownCounter[Long](s"$namespace.buffer.records.pending")
        .withUnit("{record}")
        .withDescription("Records buffered but not yet handed to a put")
        .create
      bufferTime <- meter
        .histogram[Double](s"$namespace.buffer.time")
        .withUnit("s")
        .withDescription("Time a record waited in the buffer before being put")
        .create
      bufferDropped <- meter
        .counter[Long](s"$namespace.buffer.records.dropped")
        .withUnit("{record}")
        .withDescription("Records rejected by the buffer, keyed by reason")
        .create
    } yield new FS2ProducerInstruments[F](
      bufferPending,
      bufferTime,
      bufferDropped
    )

  /** No-op instruments; records nothing. */
  def noop[F[_]: Applicative]: FS2ProducerInstruments[F] =
    new FS2ProducerInstruments[F](
      UpDownCounter.noop[F, Long],
      Histogram.noop[F, Double],
      Counter.noop[F, Long]
    )
}
