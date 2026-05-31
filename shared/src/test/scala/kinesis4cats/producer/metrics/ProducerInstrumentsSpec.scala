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
    MetricsTestkit.inMemory[IO]().use { tk =>
      for {
        meter <- tk.meterProvider.get("test")
        instr <- ProducerInstruments.fromMeter[IO](meter)
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
            "kinesis4cats.producer.user_records.received",
            "kinesis4cats.producer.user_records.bytes",
            "kinesis4cats.producer.kinesis_records.put",
            "kinesis4cats.producer.kinesis_records.bytes",
            "kinesis4cats.producer.request.duration",
            "kinesis4cats.producer.retries",
            "kinesis4cats.producer.errors"
          )
        )
        val checked = MetricExpectations.checkAll(
          metrics,
          MetricExpectation
            .sum[Long]("kinesis4cats.producer.user_records.received")
            .value(5L, Attribute("stream.name", "foo")),
          MetricExpectation
            .sum[Long]("kinesis4cats.producer.kinesis_records.put")
            .value(
              3L,
              Attribute("stream.name", "foo"),
              Attribute("shard.id", "shard-1")
            ),
          MetricExpectation
            .sum[Long]("kinesis4cats.producer.errors")
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
