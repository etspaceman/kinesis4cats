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

package kinesis4cats.smithy4s.client.producer

import java.util.UUID

import cats.effect.IO
import cats.effect.Resource
import com.amazonaws.kinesis.PutRecordsInput
import com.amazonaws.kinesis.PutRecordsOutput
import org.http4s.ember.client.EmberClientBuilder

import kinesis4cats.client.logging.instances.show._
import kinesis4cats.kcl.logging.instances.show._
import kinesis4cats.logging.instances.show._
import kinesis4cats.producer.Producer
import kinesis4cats.producer.ProducerSpec
import kinesis4cats.producer.logging.instances.show._
import kinesis4cats.smithy4s.client.logging.instances.show._
import kinesis4cats.smithy4s.client.producer.localstack.LocalstackKinesisProducer

class KinesisProducerSpec
    extends ProducerSpec[PutRecordsInput, PutRecordsOutput]() {
  override lazy val streamName: String =
    s"smithy4s-client-producer-spec-${UUID.randomUUID().toString()}"
  override def producerResource
      : Resource[IO, Producer[IO, PutRecordsInput, PutRecordsOutput]] =
    for {
      underlying <- EmberClientBuilder
        .default[IO]
        .withoutCheckEndpointAuthentication
        .build
      producer <- LocalstackKinesisProducer.resource[IO](
        underlying,
        streamName
      )
    } yield producer

}
