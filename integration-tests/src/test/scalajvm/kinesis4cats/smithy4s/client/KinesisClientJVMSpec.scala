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



import cats.effect.IO
import cats.effect.SyncIO
import cats.effect.kernel.Async
import com.amazonaws.kinesis.Kinesis
import org.http4s.blaze.client.BlazeClientBuilder

import kinesis4cats.SSL
import kinesis4cats.logging.ConsoleLogger
import kinesis4cats.logging.instances.show._
import kinesis4cats.smithy4s.client.localstack.LocalstackKinesisClient
import kinesis4cats.smithy4s.client.logging.instances.show._

class KinesisClientJVMSpec extends KinesisClientSpec {
  override def fixture: SyncIO[FunFixture[Kinesis[IO]]] =
    ResourceFunFixture(
      for {
        underlying <- BlazeClientBuilder[IO]
          .withSslContext(SSL.context)
          .resource
        client <- LocalstackKinesisClient.clientResource[IO](
          underlying,
          IO.pure(region),
          loggerF = (f: Async[IO]) => f.pure(new ConsoleLogger[IO])
        )
      } yield client
    )
}
