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
package client

import cats.effect.kernel.Clock
import cats.effect.{IO, SyncIO}
import software.amazon.awssdk.services.cloudwatch.model._

import kinesis4cats.Utils
import kinesis4cats.client.localstack.LocalstackCloudWatchClient

class CloudWatchClientSpec extends munit.CatsEffectSuite {
  def fixture: SyncIO[FunFixture[CloudWatchClient[IO]]] =
    ResourceFunFixture(
      LocalstackCloudWatchClient.Builder
        .default[IO]()
        .toResource
        .flatMap(_.build)
    )

  val tableName = s"cloudwatch-client-spec-${Utils.randomUUIDString}"

  fixture.test("It should work through all commands") { client =>
    for {
      now <- Clock[IO].realTimeInstant
      _ <- client.putMetricData(
        PutMetricDataRequest
          .builder()
          .namespace("foo")
          .metricData(
            MetricDatum
              .builder()
              .metricName("metric")
              .value(5.0)
              .timestamp(now)
              .dimensions(
                Dimension
                  .builder()
                  .name("dimension")
                  .value("dimensionValue")
                  .build()
              )
              .unit(StandardUnit.COUNT)
              .build()
          )
          .build()
      )
    } yield assert(true)
  }

}
