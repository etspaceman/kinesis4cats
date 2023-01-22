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

import java.nio.ByteBuffer

import com.amazonaws.services.schemaregistry.common.Schema
import io.circe.{Encoder, Json}
import software.amazon.awssdk.services.kinesis.model._
import software.amazon.kinesis.lifecycle.events._
import software.amazon.kinesis.lifecycle.{ShutdownInput, ShutdownReason}
import software.amazon.kinesis.retrieval.KinesisClientRecord
import software.amazon.kinesis.retrieval.kpl.ExtendedSequenceNumber

import kinesis4cats.syntax.bytebuffer._
import kinesis4cats.syntax.circe._
import kinesis4cats.instances.circe._
import kinesis4cats.kcl.processor.RecordProcessorLogEncoders

object circe {

  implicit val encryptionTypeEncoder: Encoder[EncryptionType] =
    Encoder[String].contramap(_.name)

  implicit val schemaEncoder: Encoder[Schema] = x => {
    val fields: Map[String, Json] =
      Map
        .empty[String, Json]
        .maybeAdd("dataFormat", x.getDataFormat())
        .maybeAdd("schemaDefinition", x.getSchemaDefinition())
        .maybeAdd("schemaName", x.getSchemaName())

    Json.obj(fields.toSeq: _*)
  }

  implicit val hashKeyRangeEncoder: Encoder[HashKeyRange] = x => {
    val fields: Map[String, Json] =
      Map
        .empty[String, Json]
        .maybeAdd("endingHashKey", x.endingHashKey())
        .maybeAdd("startingHashKey", x.startingHashKey())

    Json.obj(fields.toSeq: _*)
  }

  implicit val childShardEncoder: Encoder[ChildShard] = x => {
    val fields: Map[String, Json] =
      Map
        .empty[String, Json]
        .maybeAdd("hasParentShards", x.hasParentShards())
        .maybeAdd("hashKeyRange", x.hashKeyRange())
        .maybeAdd("parentShards", x.parentShards())
        .maybeAdd("shardId", x.shardId())

    Json.obj(fields.toSeq: _*)
  }

  implicit val extendedSequenceNumberEncoder: Encoder[ExtendedSequenceNumber] =
    x => {
      val fields: Map[String, Json] =
        Map
          .empty[String, Json]
          .maybeAdd("sequenceNumber", x.sequenceNumber())
          .maybeAdd("subSequenceNumber", x.subSequenceNumber())

      Json.obj(fields.toSeq: _*)
    }

  implicit val shutdownReasonEncoder: Encoder[ShutdownReason] =
    Encoder[String].contramap(_.name)

  implicit val initializationInputEncoder: Encoder[InitializationInput] = x => {
    val fields: Map[String, Json] =
      Map
        .empty[String, Json]
        .maybeAdd("shardId", x.shardId())
        .maybeAdd("extendedSequenceNumber", x.extendedSequenceNumber())
        .maybeAdd(
          "pendingCheckpointSequenceNumber",
          x.pendingCheckpointSequenceNumber()
        )

    Json.obj(fields.toSeq: _*)
  }

  implicit val processRecordsInputEncoder: Encoder[ProcessRecordsInput] = x => {
    val fields: Map[String, Json] =
      Map
        .empty[String, Json]
        .maybeAdd("cacheEntryTime", x.cacheEntryTime())
        .maybeAdd("cacheExitTime", x.cacheExitTime())
        .maybeAdd("childShards", x.childShards())
        .maybeAdd("isAtShardEnd", x.isAtShardEnd())
        .maybeAdd("millisBehindLatest", x.millisBehindLatest())
        .maybeAdd("timeSpentInCache", x.timeSpentInCache())

    Json.obj(fields.toSeq: _*)
  }

  implicit val shutdownInputEncoder: Encoder[ShutdownInput] = x => {
    val fields: Map[String, Json] =
      Map
        .empty[String, Json]
        .maybeAdd("shutdownReason", x.shutdownReason())

    Json.obj(fields.toSeq: _*)
  }

  implicit val kinesisClientRecordEncoder: Encoder[KinesisClientRecord] = x => {
    implicit val byteBufferEncoder: Encoder[ByteBuffer] =
      Encoder[String].contramap(x => x.asBase64String)

    val fields: Map[String, Json] =
      Map
        .empty[String, Json]
        .maybeAdd("aggregated", x.aggregated())
        .maybeAdd(
          "approximateArrivalTimestamp",
          x.approximateArrivalTimestamp()
        )
        .maybeAdd("encryptionType", x.encryptionType())
        .maybeAdd("explicitHashKey", x.explicitHashKey())
        .maybeAdd("partitionKey", x.partitionKey())
        .maybeAdd("schema", x.schema())
        .maybeAdd("sequenceNumber", x.sequenceNumber())
        .maybeAdd("subSequenceNumber", x.subSequenceNumber())
        .maybeAdd("data", x.data())

    Json.obj(fields.toSeq: _*)
  }

  implicit val recordProcessorLogEncoders: RecordProcessorLogEncoders =
    new RecordProcessorLogEncoders

}
