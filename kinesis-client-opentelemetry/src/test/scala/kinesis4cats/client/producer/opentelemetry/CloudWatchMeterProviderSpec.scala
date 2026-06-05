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

import munit.CatsEffectSuite
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region

class CloudWatchMeterProviderSpec extends CatsEffectSuite {

  private val creds = StaticCredentialsProvider.create(
    AwsBasicCredentials.create("AKIDEXAMPLE", "secret")
  )

  test("headerSupplier produces SigV4 Authorization and x-amz-date headers") {
    val supplier =
      CloudWatchMeterProvider.headerSupplier(
        creds,
        Region.US_EAST_1
      )
    val headers = supplier.get()
    assert(
      headers.get("Authorization").startsWith("AWS4-HMAC-SHA256"),
      s"unexpected Authorization: ${headers.get("Authorization")}"
    )
    assert(headers.containsKey("X-Amz-Date"))
  }

  test("session token credentials add x-amz-security-token header") {
    val sessionCreds = StaticCredentialsProvider.create(
      AwsSessionCredentials.create("AKIDEXAMPLE", "secret", "token123")
    )
    val supplier =
      CloudWatchMeterProvider.headerSupplier(sessionCreds, Region.US_EAST_1)
    val headers = supplier.get()
    assertEquals(headers.get("X-Amz-Security-Token"), "token123")
  }
}
