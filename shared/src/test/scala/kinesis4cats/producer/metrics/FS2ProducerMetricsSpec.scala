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

import _root_.fs2.concurrent.Channel
import cats.effect.IO
import org.typelevel.log4cats.StructuredLogger
import org.typelevel.log4cats.noop.NoOpLogger
import org.typelevel.otel4s.Attribute
import org.typelevel.otel4s.sdk.testkit.metrics.MetricExpectation
import org.typelevel.otel4s.sdk.testkit.metrics.MetricExpectations
import org.typelevel.otel4s.sdk.testkit.metrics.MetricsTestkit

import kinesis4cats.models.StreamNameOrArn
import kinesis4cats.producer.fs2.FS2Producer

class FS2ProducerMetricsSpec extends munit.CatsEffectSuite {

  private final class MockFS2Producer(
      override val logger: StructuredLogger[IO],
      override val config: FS2Producer.Config[IO],
      override protected val channel: Channel[
        IO,
        FS2Producer.Buffered[IO, MockPutResponse]
      ],
      override protected val underlying: MockProducer
  ) extends FS2Producer[IO, MockPutRequest, MockPutResponse]

  private def mkConfig(
      queueSize: Int,
      instruments: FS2ProducerInstruments[IO]
  ): FS2Producer.Config[IO] =
    FS2Producer.Config
      .default[IO](StreamNameOrArn.Name("foo"))
      .copy(
        queueSize = queueSize,
        putMaxChunk = 10,
        putMaxWait = 20.millis,
        gracefulShutdownWait = 2.seconds,
        instruments = instruments
      )

  private def mkProducer(
      config: FS2Producer.Config[IO],
      underlying: MockProducer
  ): IO[MockFS2Producer] =
    Channel
      .bounded[IO, FS2Producer.Buffered[IO, MockPutResponse]](config.queueSize)
      .map(ch => new MockFS2Producer(NoOpLogger[IO], config, ch, underlying))

  def rec(id: String) = Record(Array.fill(10)(1.toByte), id)

  test("buffer.time recorded per record; pending nets to zero after drain") {
    val ns = "test"
    val resources = for {
      tk <- MetricsTestkit.inMemory[IO]()
      underlying <- MockProducer(aggregate = false, raiseOnFailures = false)
    } yield (tk, underlying)

    resources.use { case (tk, underlying) =>
      for {
        meter <- tk.meterProvider.get(ns)
        instr <- FS2ProducerInstruments.fromMeter[IO](meter, ns)
        producer <- mkProducer(mkConfig(10, instr), underlying)
        _ <- producer.resource.use { _ =>
          for {
            f1 <- producer.put(rec("1"))
            f2 <- producer.put(rec("2"))
            _ <- f1
            _ <- f2
          } yield ()
        }
        metrics <- tk.collectMetrics
      } yield {
        val checked = MetricExpectations.checkAll(
          metrics,
          // both records share stream.name=foo, so they aggregate into one
          // data point whose internal count is the number of records (2)
          MetricExpectation
            .histogram(s"$ns.buffer.time")
            .pointCount(1)
            .pointsWhere("two buffering-time samples recorded")(
              _.flatMap(_.stats.toList).map(_.count).sum == 2L
            ),
          MetricExpectation
            .sum[Long](s"$ns.buffer.records.pending")
            .value(0L, Attribute("stream.name", "foo"))
        )
        assert(checked.isRight, checked.toString)
      }
    }
  }

  test("tryPut on a full queue records a queue_full drop") {
    val ns = "test"
    val resources = for {
      tk <- MetricsTestkit.inMemory[IO]()
      underlying <- MockProducer(aggregate = false, raiseOnFailures = false)
    } yield (tk, underlying)

    resources.use { case (tk, underlying) =>
      for {
        meter <- tk.meterProvider.get(ns)
        instr <- FS2ProducerInstruments.fromMeter[IO](meter, ns)
        // queueSize 1, never started → second tryPut sees a full queue
        producer <- mkProducer(mkConfig(1, instr), underlying)
        _ <- producer.tryPut(rec("1"))
        _ <- producer.tryPut(rec("2"))
        metrics <- tk.collectMetrics
      } yield {
        val checked = MetricExpectations.checkAll(
          metrics,
          MetricExpectation
            .sum[Long](s"$ns.buffer.records.dropped")
            .value(
              1L,
              Attribute("stream.name", "foo"),
              Attribute("reason", "queue_full")
            )
        )
        assert(checked.isRight, checked.toString)
      }
    }
  }
}
