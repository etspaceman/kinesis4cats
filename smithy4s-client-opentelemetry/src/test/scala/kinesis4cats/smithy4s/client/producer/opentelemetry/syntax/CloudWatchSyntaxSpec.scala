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

package kinesis4cats.smithy4s.client.producer.opentelemetry.syntax

import cats.effect.IO
import cats.effect.Resource
import munit.FunSuite
import org.http4s.HttpApp
import org.http4s.Response
import org.http4s.Status
import org.http4s.client.Client
import smithy4s.aws.kernel.AwsRegion

import kinesis4cats.models.StreamNameOrArn
import kinesis4cats.smithy4s.client.producer.KinesisProducer
import kinesis4cats.smithy4s.client.producer.fs2.FS2KinesisProducer
import kinesis4cats.smithy4s.client.producer.opentelemetry.syntax.cloudwatch._

class CloudWatchSyntaxSpec extends FunSuite {

  test("buildWithCloudWatch is available on both smithy4s producer builders") {
    val stream = StreamNameOrArn.Name("test-stream")
    val client = Client.fromHttpApp[IO](
      HttpApp[IO](_ => IO.pure(Response[IO](Status.Ok)))
    )
    val _ : Resource[IO, KinesisProducer[IO]] =
      KinesisProducer.Builder
        .default[IO](stream, client, AwsRegion.US_EAST_1)
        .buildWithCloudWatch()
    val _ : Resource[IO, FS2KinesisProducer[IO]] =
      FS2KinesisProducer.Builder
        .default[IO](stream, client, AwsRegion.US_EAST_1)
        .buildWithCloudWatch()
    assert(true)
  }
}
