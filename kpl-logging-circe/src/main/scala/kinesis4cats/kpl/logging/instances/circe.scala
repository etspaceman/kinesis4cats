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

package kinesis4cats.kpl
package logging.instances

import com.amazonaws.services.kinesis.producer._
import com.amazonaws.services.schemaregistry.common.Schema
import io.circe.{Encoder, Json}

import kinesis4cats.logging.instances.circe._
import kinesis4cats.logging.syntax.circe._

/** KPL [[kinesis4cats.logging.LogEncoder LogEncoder]] instances for JSON
  * encoding of log structures using [[https://circe.github.io/circe/ Circe]]
  */
object circe {
  val kplCirceEncoders: KPLProducer.LogEncoders = {
    implicit val attemptEncoder: Encoder[Attempt] = x => {
      val fields: Map[String, Json] =
        Map
          .empty[String, Json]
          .safeAdd("delay", x.getDelay())
          .safeAdd("duration", x.getDuration())
          .safeAdd("errorCode", x.getErrorCode())
          .safeAdd("errorMessage", x.getErrorMessage())

      Json.obj(fields.toSeq: _*)
    }

    implicit val schemaEncoder: Encoder[Schema] = x => {
      val fields: Map[String, Json] =
        Map
          .empty[String, Json]
          .safeAdd("dataFormat", x.getDataFormat())
          .safeAdd("schemaDefinition", x.getSchemaDefinition())
          .safeAdd("schemaName", x.getSchemaName())

      Json.obj(fields.toSeq: _*)
    }

    implicit val userRecordEncoder: Encoder[UserRecord] = x => {
      val fields: Map[String, Json] =
        Map
          .empty[String, Json]
          .safeAdd("data", x.getData())
          .safeAdd("partitionKey", x.getPartitionKey())
          .safeAdd("explicitHashKey", x.getExplicitHashKey())
          .safeAdd("schema", x.getSchema())
          .safeAdd("streamName", x.getStreamName())

      Json.obj(fields.toSeq: _*)
    }

    implicit val userRecordResultEncoder: Encoder[UserRecordResult] = x => {
      val fields: Map[String, Json] =
        Map
          .empty[String, Json]
          .safeAdd("isSuccessful", x.isSuccessful())
          .safeAdd("attempts", x.getAttempts())
          .safeAdd("sequenceNumber", x.getSequenceNumber())
          .safeAdd("shardId", x.getShardId())

      Json.obj(fields.toSeq: _*)
    }

    new KPLProducer.LogEncoders()
  }

}
