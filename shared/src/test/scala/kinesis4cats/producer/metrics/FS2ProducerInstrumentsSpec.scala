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

import cats.effect.IO
import org.typelevel.otel4s.Attribute
import org.typelevel.otel4s.sdk.testkit.metrics.MetricExpectation
import org.typelevel.otel4s.sdk.testkit.metrics.MetricExpectations
import org.typelevel.otel4s.sdk.testkit.metrics.MetricsTestkit

import kinesis4cats.models.StreamNameOrArn

class FS2ProducerInstrumentsSpec extends munit.CatsEffectSuite {
  val stream = StreamNameOrArn.Name("foo")

  test("fromMeter records buffer instruments, values and attributes") {
    val namespace = "test"

    MetricsTestkit.inMemory[IO]().use { tk =>
      for {
        meter <- tk.meterProvider.get(namespace)
        instr <- FS2ProducerInstruments.fromMeter[IO](meter, namespace)
        _ <- instr.recordEnqueued(stream)
        _ <- instr.recordEnqueued(stream)
        _ <- instr.recordDequeued(20.millis, stream)
        _ <- instr.recordDropped(stream, FS2ProducerInstruments.queueFullReason)
        metrics <- tk.collectMetrics
      } yield {
        val names = metrics.map(_.name).toSet
        assertEquals(
          names,
          Set(
            s"$namespace.buffer.records.pending",
            s"$namespace.buffer.time",
            s"$namespace.buffer.records.dropped"
          )
        )
        val checked = MetricExpectations.checkAll(
          metrics,
          // 2 enqueued - 1 dequeued = 1 pending
          MetricExpectation
            .sum[Long](s"$namespace.buffer.records.pending")
            .value(1L, Attribute("stream.name", "foo")),
          MetricExpectation
            .histogram(s"$namespace.buffer.time")
            .pointCount(1),
          MetricExpectation
            .sum[Long](s"$namespace.buffer.records.dropped")
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

  test("noop records nothing") {
    MetricsTestkit.inMemory[IO]().use { tk =>
      val instr = FS2ProducerInstruments.noop[IO]
      for {
        _ <- instr.recordEnqueued(stream)
        _ <- instr.recordDequeued(20.millis, stream)
        _ <- instr.recordDropped(stream, FS2ProducerInstruments.shutdownReason)
        metrics <- tk.collectMetrics
      } yield assertEquals(metrics, Nil)
    }
  }
}
