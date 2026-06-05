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

package kinesis4cats.smithy4s.client.producer.opentelemetry

import cats.effect.IO
import cats.effect.Ref
import munit.CatsEffectSuite
import org.http4s.HttpApp
import org.http4s.Request
import org.http4s.Response
import org.http4s.Status
import org.http4s.client.Client
import smithy4s.aws.kernel.AwsCredentials
import smithy4s.aws.kernel.AwsRegion

class CloudWatchMeterProviderSpec extends CatsEffectSuite {

  private val testCreds: IO[AwsCredentials] =
    IO.pure(AwsCredentials.Default("AKIDEXAMPLE", "secret", None))

  /** Records every request the exporter sends, returns 200. */
  private def recordingClient(
      ref: Ref[IO, List[Request[IO]]]
  ): Client[IO] =
    Client.fromHttpApp[IO](
      HttpApp[IO](req => ref.update(_ :+ req).as(Response[IO](Status.Ok)))
    )

  test("exporter signs requests and targets /v1/metrics") {
    for {
      ref <- Ref.of[IO, List[Request[IO]]](Nil)
      client = recordingClient(ref)
      _ <- CloudWatchMeterProvider
        .resource[IO](
          AwsRegion.US_EAST_1,
          client,
          _ => cats.effect.Resource.pure(testCreds)
        )
        .use { mp =>
          for {
            meter <- mp.get("smoke")
            counter <- meter.counter[Long]("smoke.counter").create
            _ <- counter.inc()
          } yield ()
        }
      // Allocation + release force-flushes at least one export.
      reqs <- ref.get
    } yield {
      assert(reqs.nonEmpty, "expected the exporter to flush at least one request")
      val req = reqs.head
      assertEquals(req.uri.path.renderString, "/v1/metrics")
      val auth = req.headers
        .get(org.typelevel.ci.CIString("Authorization"))
        .map(_.head.value)
      assert(
        auth.exists(_.startsWith(s"AWS4-HMAC-SHA256 Credential=AKIDEXAMPLE/")),
        s"unexpected Authorization header: $auth"
      )
      assert(
        auth.exists(_.contains("/monitoring/aws4_request")),
        s"expected monitoring credential scope: $auth"
      )
      assertEquals(
        req.headers
          .get(org.typelevel.ci.CIString("X-Amz-Content-Sha256"))
          .map(_.head.value),
        Some("UNSIGNED-PAYLOAD")
      )
    }
  }

  test("resource allocates and releases without error") {
    val client = Client.fromHttpApp[IO](
      HttpApp[IO](_ => IO.pure(Response[IO](Status.Ok)))
    )
    CloudWatchMeterProvider
      .resource[IO](
        AwsRegion.US_EAST_1,
        client,
        _ => cats.effect.Resource.pure(testCreds)
      )
      .use(_ => IO.unit)
      .void
  }
}
