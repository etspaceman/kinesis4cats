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

package kinesis4cats.producer.metrics.cloudwatch

import munit.FunSuite

class CloudWatchConventionsSpec extends FunSuite {

  test("cloudWatchUnit maps known otel units to CloudWatch StandardUnit strings") {
    assertEquals(CloudWatchConventions.cloudWatchUnit("By"), "Bytes")
    assertEquals(CloudWatchConventions.cloudWatchUnit("s"), "Seconds")
    assertEquals(CloudWatchConventions.cloudWatchUnit("{record}"), "Count")
    assertEquals(CloudWatchConventions.cloudWatchUnit("{error}"), "Count")
    assertEquals(CloudWatchConventions.cloudWatchUnit("{retry}"), "Count")
  }

  test("cloudWatchUnit maps unknown units to None") {
    assertEquals(CloudWatchConventions.cloudWatchUnit("furlongs"), "None")
    assertEquals(CloudWatchConventions.cloudWatchUnit(""), "None")
  }

  test("defaults are KPL-compatible and batch size is the documented limit") {
    assertEquals(
      CloudWatchConventions.defaultCloudWatchNamespace,
      "KinesisProducerLibrary"
    )
    assertEquals(CloudWatchConventions.maxDatumsPerRequest, 1000)
  }
}
