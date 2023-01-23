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

package kinesis4cats.kcl.logging.instances

import com.amazonaws.services.schemaregistry.common.Schema
import io.circe.{Encoder, Json}
import software.amazon.awssdk.services.kinesis.model._
import software.amazon.kinesis.lifecycle.events._
import software.amazon.kinesis.lifecycle.{ShutdownInput, ShutdownReason}
import software.amazon.kinesis.retrieval.KinesisClientRecord
import software.amazon.kinesis.retrieval.kpl.ExtendedSequenceNumber

import kinesis4cats.kcl.processor.RecordProcessorLogEncoders
import kinesis4cats.logging.instances.circe._
import kinesis4cats.logging.syntax.circe._

/** KCL [[kinesis4cats.logging.LogEncoder LogEncoder]] instances for JSON
  * encoding of log structures using [[https://circe.github.io/circe/ Circe]]
  */
object circe {

  implicit val encryptionTypeEncoder: Encoder[EncryptionType] =
    Encoder[String].contramap(_.name)

  implicit val schemaEncoder: Encoder[Schema] = x => {
    val fields: Map[String, Json] =
      Map
        .empty[String, Json]
        .safeAdd("dataFormat", x.getDataFormat())
        .safeAdd("schemaDefinition", x.getSchemaDefinition())
        .safeAdd("schemaName", x.getSchemaName())

    Json.obj(fields.toSeq: _*)
  }

  implicit val hashKeyRangeEncoder: Encoder[HashKeyRange] = x => {
    val fields: Map[String, Json] =
      Map
        .empty[String, Json]
        .safeAdd("endingHashKey", x.endingHashKey())
        .safeAdd("startingHashKey", x.startingHashKey())

    Json.obj(fields.toSeq: _*)
  }

  implicit val childShardEncoder: Encoder[ChildShard] = x => {
    val fields: Map[String, Json] =
      Map
        .empty[String, Json]
        .safeAdd("hasParentShards", x.hasParentShards())
        .safeAdd("hashKeyRange", x.hashKeyRange())
        .safeAdd("parentShards", x.parentShards())
        .safeAdd("shardId", x.shardId())

    Json.obj(fields.toSeq: _*)
  }

  implicit val extendedSequenceNumberEncoder: Encoder[ExtendedSequenceNumber] =
    x => {
      val fields: Map[String, Json] =
        Map
          .empty[String, Json]
          .safeAdd("sequenceNumber", x.sequenceNumber())
          .safeAdd("subSequenceNumber", x.subSequenceNumber())

      Json.obj(fields.toSeq: _*)
    }

  implicit val shutdownReasonEncoder: Encoder[ShutdownReason] =
    Encoder[String].contramap(_.name)

  implicit val initializationInputEncoder: Encoder[InitializationInput] = x => {
    val fields: Map[String, Json] =
      Map
        .empty[String, Json]
        .safeAdd("shardId", x.shardId())
        .safeAdd("extendedSequenceNumber", x.extendedSequenceNumber())
        .safeAdd(
          "pendingCheckpointSequenceNumber",
          x.pendingCheckpointSequenceNumber()
        )

    Json.obj(fields.toSeq: _*)
  }

  implicit val processRecordsInputEncoder: Encoder[ProcessRecordsInput] = x => {
    val fields: Map[String, Json] =
      Map
        .empty[String, Json]
        .safeAdd("cacheEntryTime", x.cacheEntryTime())
        .safeAdd("cacheExitTime", x.cacheExitTime())
        .safeAdd("childShards", x.childShards())
        .safeAdd("isAtShardEnd", x.isAtShardEnd())
        .safeAdd("millisBehindLatest", x.millisBehindLatest())
        .safeAdd("timeSpentInCache", x.timeSpentInCache())

    Json.obj(fields.toSeq: _*)
  }

  implicit val shutdownInputEncoder: Encoder[ShutdownInput] = x => {
    val fields: Map[String, Json] =
      Map
        .empty[String, Json]
        .safeAdd("shutdownReason", x.shutdownReason())

    Json.obj(fields.toSeq: _*)
  }

  implicit val kinesisClientRecordEncoder: Encoder[KinesisClientRecord] = x => {
    val fields: Map[String, Json] =
      Map
        .empty[String, Json]
        .safeAdd("aggregated", x.aggregated())
        .safeAdd(
          "approximateArrivalTimestamp",
          x.approximateArrivalTimestamp()
        )
        .safeAdd("encryptionType", x.encryptionType())
        .safeAdd("explicitHashKey", x.explicitHashKey())
        .safeAdd("partitionKey", x.partitionKey())
        .safeAdd("schema", x.schema())
        .safeAdd("sequenceNumber", x.sequenceNumber())
        .safeAdd("subSequenceNumber", x.subSequenceNumber())
        .safeAdd("data", x.data())

    Json.obj(fields.toSeq: _*)
  }

  implicit val recordProcessorLogEncoders: RecordProcessorLogEncoders =
    new RecordProcessorLogEncoders

}
