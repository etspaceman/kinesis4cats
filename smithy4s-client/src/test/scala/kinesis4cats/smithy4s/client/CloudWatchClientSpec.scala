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

package kinesis4cats.smithy4s.client

import cats.effect.IO
import cats.effect.Ref
import cats.effect.Resource
import fs2.io.compression._
import munit.CatsEffectSuite
import org.http4s.HttpApp
import org.http4s.Request
import org.http4s.Response
import org.http4s.Status
import org.http4s.client.Client
import org.typelevel.ci.CIString
import smithy4s.aws.kernel.AwsCredentials
import smithy4s.aws.kernel.AwsRegion

import kinesis4cats.producer.metrics.cloudwatch.CloudWatchDatum

class CloudWatchClientSpec extends CatsEffectSuite {

  private val testCreds: IO[AwsCredentials] =
    IO.pure(AwsCredentials.Default("AKIDEXAMPLE", "secret", None))

  private def recordingClient(ref: Ref[IO, List[Request[IO]]]): Client[IO] =
    Client.fromHttpApp[IO](
      HttpApp[IO](req =>
        ref
          .update(_ :+ req)
          .as(
            Response[IO](Status.Ok).withEntity(
              "<PutMetricDataResponse xmlns=\"http://monitoring.amazonaws.com/doc/2010-08-01/\"></PutMetricDataResponse>"
            )
          )
      )
    )

  private val datum = CloudWatchDatum(
    metricName = "kinesis4cats.producer.user_records.received",
    value = Left(3.0d),
    unit = "Count",
    dimensions = List("stream.name" -> "test-stream"),
    timestampMs = 1000L
  )

  test("putMetricData signs and POSTs to the monitoring endpoint") {
    for {
      ref <- Ref.of[IO, List[Request[IO]]](Nil)
      client = recordingClient(ref)
      _ <- CloudWatchClient
        .resource[IO](
          client,
          AwsRegion.US_EAST_1,
          _ => Resource.pure(testCreds)
        )
        .use(_.putMetricData("KinesisProducerLibrary", List(datum)))
      reqs <- ref.get
    } yield {
      assert(reqs.nonEmpty, "expected a PutMetricData request")
      val req = reqs.head
      assert(
        req.headers
          .get(CIString("Authorization"))
          .exists(_.head.value.startsWith("AWS4-HMAC-SHA256")),
        "expected a SigV4 Authorization header"
      )
    }
  }
}
