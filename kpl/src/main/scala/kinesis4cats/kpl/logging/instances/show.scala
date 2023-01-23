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

package kinesis4cats.kpl.logging.instances

import cats.Show
import com.amazonaws.services.kinesis.producer._
import com.amazonaws.services.schemaregistry.common.Schema

import kinesis4cats.ShowBuilder
import kinesis4cats.logging.instances.show._

object show {
  implicit val attemptShow: Show[Attempt] = x =>
    ShowBuilder("Attempt")
      .add("delay", x.getDelay())
      .add("duration", x.getDuration())
      .add("errorCode", x.getErrorCode())
      .add("errorMessage", x.getErrorMessage())
      .build

  implicit val schemaShow: Show[Schema] = x =>
    ShowBuilder("Schema")
      .add("dataFormat", x.getDataFormat())
      .add("schemaDefinition", x.getSchemaDefinition())
      .add("schemaName", x.getSchemaName())
      .build

  implicit val userRecordShow: Show[UserRecord] = x =>
    ShowBuilder("UserRecord")
      .add("data", x.getData())
      .add("partitionKey", x.getPartitionKey())
      .add("explicitHashKey", x.getExplicitHashKey())
      .add("schema", x.getSchema())
      .add("streamName", x.getStreamName())
      .build

  implicit val userRecordResultShow: Show[UserRecordResult] = x =>
    ShowBuilder("UserRecordResult")
      .add("isSuccessful", x.isSuccessful())
      .add("attempts", x.getAttempts())
      .add("sequenceNumber", x.getSequenceNumber())
      .add("shardId", x.getShardId())
      .build
}
