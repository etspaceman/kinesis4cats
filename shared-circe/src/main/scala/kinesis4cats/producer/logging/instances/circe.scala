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

package kinesis4cats
package producer
package logging.instances

import io.circe.Encoder
import io.circe.syntax._

import kinesis4cats.logging.instances.circe._
import kinesis4cats.models._

/** [[kinesis4cats.producer.Producer]]
  * [[kinesis4cats.logging.LogEncoder LogEncoder]] instances for string encoding
  * of log structures using [[https://circe.github.io/circe/ Circe]]
  */
object circe {
  implicit val shardIdEncoder: Encoder[ShardId] =
    Encoder[String].contramap(_.shardId)

  implicit val recordEncoder: Encoder[Record] =
    Encoder.forProduct3("data", "partitionKey", "explicitHashKey")(x =>
      (x.data, x.partitionKey, x.explicitHashKey)
    )

  implicit val hashKeyRangeEncoder: Encoder[HashKeyRange] =
    Encoder.forProduct2("endingHashKey", "startingHashKey")(x =>
      (x.endingHashKey, x.startingHashKey)
    )

  implicit val shardMapRecordEncoder: Encoder[ShardMapRecord] =
    Encoder.forProduct2("shardId", "hashKeyRange")(x =>
      (x.shardId, x.hashKeyRange)
    )

  implicit val shardMapEncoder: Encoder[ShardMap] =
    Encoder.forProduct2("lastUpdated", "shards")(x => (x.lastUpdated, x.shards))

  implicit val streamNameOrArnEncoder: Encoder[StreamNameOrArn] = {
    case StreamNameOrArn.Name(streamName) => streamName.asJson
    case StreamNameOrArn.Arn(arn)         => arn.streamArn.asJson
  }

  implicit val shardMapCacheLogEncoders: ShardMapCache.LogEncoders =
    new ShardMapCache.LogEncoders()

  implicit val producerLogEncoders: Producer.LogEncoders =
    new Producer.LogEncoders()
}
