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

import kinesis4cats.models.ShardId
import kinesis4cats.models.StreamNameOrArn

class ProducerInstrumentsSpec extends munit.CatsEffectSuite {
  val stream = StreamNameOrArn.Name("foo")

  test("fromMeter records the expected instruments, values and attributes") {
    val namespace = "test"

    MetricsTestkit.inMemory[IO]().use { tk =>
      for {
        meter <- tk.meterProvider.get(namespace)
        instr <- ProducerInstruments.fromMeter[IO](meter, namespace)
        _ <- instr.recordReceived(5L, 250L, stream)
        _ <- instr.recordShardPut(
          3L,
          150L,
          12.millis,
          stream,
          ShardId("shard-1")
        )
        _ <- instr.recordRetries(2L, stream)
        _ <- instr.recordErrors(
          Map("ProvisionedThroughputExceededException" -> 1L),
          stream
        )
        metrics <- tk.collectMetrics
      } yield {
        val names = metrics.map(_.name).toSet
        assertEquals(
          names,
          Set(
            s"$namespace.user_records.received",
            s"$namespace.user_records.bytes",
            s"$namespace.kinesis_records.put",
            s"$namespace.kinesis_records.bytes",
            s"$namespace.request.duration",
            s"$namespace.retries",
            s"$namespace.errors"
          )
        )
        val checked = MetricExpectations.checkAll(
          metrics,
          MetricExpectation
            .sum[Long](s"$namespace.user_records.received")
            .value(5L, Attribute("stream.name", "foo")),
          MetricExpectation
            .sum[Long](s"$namespace.kinesis_records.put")
            .value(
              3L,
              Attribute("stream.name", "foo"),
              Attribute("shard.id", "shard-1")
            ),
          MetricExpectation
            .sum[Long](s"$namespace.errors")
            .value(
              1L,
              Attribute("stream.name", "foo"),
              Attribute("error.code", "ProvisionedThroughputExceededException")
            )
        )
        assert(checked.isRight, checked.toString)
      }
    }
  }

  test("noop records nothing") {
    MetricsTestkit.inMemory[IO]().use { tk =>
      val instr = ProducerInstruments.noop[IO]
      for {
        _ <- instr.recordReceived(5L, 250L, stream)
        _ <- instr.recordShardPut(
          3L,
          150L,
          12.millis,
          stream,
          ShardId("shard-1")
        )
        metrics <- tk.collectMetrics
      } yield assertEquals(metrics, Nil)
    }
  }
}
