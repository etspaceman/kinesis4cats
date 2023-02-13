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

package kinesis4cats.client.producer.localstack

import cats.effect._

import kinesis4cats.client.KinesisClient
import kinesis4cats.client.producer.KinesisProducer
import kinesis4cats.localstack.LocalstackConfig
import kinesis4cats.localstack.aws.v2.AwsClients
import kinesis4cats.models.StreamNameOrArn
import kinesis4cats.producer.Producer
import kinesis4cats.producer.ShardMapCache

object LocalstackKinesisProducer {
  def resource[F[_]](
      producerConfig: Producer.Config,
      config: LocalstackConfig
  )(implicit
      F: Async[F],
      LE: KinesisClient.LogEncoders,
      SLE: ShardMapCache.LogEncoders,
      PLE: Producer.LogEncoders
  ): Resource[F, KinesisProducer[F]] = AwsClients
    .kinesisClientResource[F](config)
    .flatMap(underlying =>
      KinesisProducer[F](
        producerConfig,
        underlying
      )
    )

  def resource[F[_]](
      streamName: String,
      prefix: Option[String] = None,
      producerConfig: String => Producer.Config = (streamName: String) =>
        Producer.Config.default(StreamNameOrArn.Name(streamName))
  )(implicit
      F: Async[F],
      LE: KinesisClient.LogEncoders,
      SLE: ShardMapCache.LogEncoders,
      PLE: Producer.LogEncoders
  ): Resource[F, KinesisProducer[F]] = LocalstackConfig
    .resource[F](prefix)
    .flatMap(resource[F](producerConfig(streamName), _))
}
