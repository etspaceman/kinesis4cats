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

import cats.data.NonEmptyList
import cats.effect.IO
import org.typelevel.otel4s.Attribute
import org.typelevel.otel4s.sdk.testkit.metrics.MetricExpectation
import org.typelevel.otel4s.sdk.testkit.metrics.MetricExpectations
import org.typelevel.otel4s.sdk.testkit.metrics.MetricsTestkit

class ProducerMetricsSpec extends munit.CatsEffectSuite {
  test("put records producer metrics; received counted once across retries") {
    val namespace = "test"
    MetricsTestkit.inMemory[IO]().use { tk =>
      for {
        meter <- tk.meterProvider.get(namespace)
        instruments <- ProducerInstruments.fromMeter[IO](meter, namespace)
        _ <- MockProducer(
          aggregate = false,
          raiseOnFailures = true,
          instruments = instruments
        ).use { producer =>
          producer
            .put(
              NonEmptyList.of(
                Record(Array.fill(50)(1), "1"),
                Record(Array.fill(50)(1), "2"),
                Record(Array.fill(50)(1), "3"),
                Record(Array.fill(50)(1), "4"),
                Record(Array.fill(50)(1), "5")
              )
            )
            .void
        }
        metrics <- tk.collectMetrics
      } yield {
        val names = metrics.map(_.name).toSet
        assert(
          names.contains(s"$namespace.user_records.received"),
          names.toString
        )
        assert(
          names.contains(s"$namespace.kinesis_records.put"),
          names.toString
        )
        assert(
          names.contains(s"$namespace.errors"),
          names.toString
        )
        // received is recorded once (value 5), not once-per-retry (would be >5)
        val checked = MetricExpectations.checkAll(
          metrics,
          MetricExpectation
            .sum[Long](s"$namespace.user_records.received")
            .value(5L, Attribute("stream.name", "foo"))
        )
        assert(checked.isRight, checked.toString)
      }
    }
  }
}
