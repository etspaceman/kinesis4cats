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

package kinesis4cats.client.producer.opentelemetry

import cats.effect.IO
import munit.CatsEffectSuite
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region

class PutMetricDataMeterProviderSpec extends CatsEffectSuite {

  private val creds = StaticCredentialsProvider.create(
    AwsBasicCredentials.create("AKIDEXAMPLE", "secret")
  )

  test("resource allocates and releases without error") {
    PutMetricDataMeterProvider
      .resource[IO](region = Some(Region.US_EAST_1), credentials = creds)
      .use(_ => IO.unit)
  }
}
