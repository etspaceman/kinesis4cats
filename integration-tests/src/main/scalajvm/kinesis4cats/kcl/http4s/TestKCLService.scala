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

package kinesis4cats.kcl
package http4s

import cats.effect.{IO, Resource, ResourceApp}
import com.comcast.ip4s._
import software.amazon.kinesis.processor.SingleStreamTracker

import kinesis4cats.Utils
import kinesis4cats.ciris.CirisReader
import kinesis4cats.kcl.localstack.LocalstackKCLConsumer

// $COVERAGE-OFF$
object TestKCLService extends ResourceApp.Forever {
  override def run(args: List[String]): Resource[IO, Unit] = for {
    streamName <- CirisReader.read[String](List("test", "stream")).resource[IO]
    configAndResults <- LocalstackKCLConsumer.kclConfigWithResults[IO](
      new SingleStreamTracker(streamName),
      s"test-kcl-service-spec-${Utils.randomUUIDString}"
    )((_: List[CommittableRecord[IO]]) => IO.unit)
    consumer = new KCLConsumer[IO](configAndResults.kclConfig)
    _ <- KCLService.server[IO](consumer, port"8080", host"0.0.0.0")
  } yield ()

}
// $COVERAGE-ON$
