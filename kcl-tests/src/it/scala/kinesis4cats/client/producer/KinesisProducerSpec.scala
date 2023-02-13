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

package kinesis4cats.client.producer

import java.util.UUID

import cats.effect.IO
import cats.effect.Resource
import cats.effect.kernel.Async
import software.amazon.awssdk.services.kinesis.model.PutRecordsRequest
import software.amazon.awssdk.services.kinesis.model.PutRecordsResponse

import kinesis4cats.client.logging.instances.show._
import kinesis4cats.client.producer.localstack.LocalstackKinesisProducer
import kinesis4cats.kcl.logging.instances.show._
import kinesis4cats.producer.Producer
import kinesis4cats.producer.ProducerSpec
import kinesis4cats.producer.logging.instances.show._

class KinesisProducerSpec
    extends ProducerSpec[PutRecordsRequest, PutRecordsResponse]() {
  override lazy val streamName: String =
    s"kinesis-client-producer-spec-${UUID.randomUUID().toString()}"
  override def producerResource
      : Resource[IO, Producer[IO, PutRecordsRequest, PutRecordsResponse]] =
    LocalstackKinesisProducer
      .resource[IO](streamName)(Async[IO], CLE, implicitly, implicitly)

}
