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

package kinesis4cats.instances

import io.circe.Encoder
import io.circe.syntax._
import software.amazon.kinesis.lifecycle.events.InitializationInput
import software.amazon.kinesis.retrieval.kpl.ExtendedSequenceNumber

import kinesis4cats.syntax.circe._
import kinesis4cats.syntax.bytebuffer._
import io.circe.Json
import software.amazon.kinesis.lifecycle.events.ProcessRecordsInput
import software.amazon.awssdk.services.kinesis.model.ChildShard
import scala.jdk.CollectionConverters._
import software.amazon.awssdk.services.kinesis.model.HashKeyRange
import software.amazon.kinesis.lifecycle.ShutdownInput
import software.amazon.kinesis.lifecycle.ShutdownReason
import software.amazon.kinesis.retrieval.KinesisClientRecord
import software.amazon.awssdk.services.kinesis.model.EncryptionType
import com.amazonaws.services.schemaregistry.common.Schema
import java.nio.ByteBuffer
import retry.RetryDetails
import scala.concurrent.duration.FiniteDuration

object circe {

  implicit val finiteDurationEncoder: Encoder[FiniteDuration] =
    Encoder.forProduct2("length", "unit")(x => (x.length, x.unit.name))

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
        .maybeAdd("parentShards", x.parentShards().asScala.toList)
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
        .maybeAdd("childShards", x.childShards().asScala.toList)
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

  implicit val retryDetailsGivingUpEncoder: Encoder[RetryDetails.GivingUp] =
    Encoder.forProduct6(
      "retriesSoFar",
      "cumulativeDelay",
      "givingUp",
      "upcomingDelay",
      "totalRetries",
      "totalDelay"
    )(x =>
      (
        x.retriesSoFar,
        x.cumulativeDelay,
        x.givingUp,
        x.upcomingDelay,
        x.totalRetries,
        x.totalDelay
      )
    )

  implicit val retryDetailsWillDelayAndRetryEncoder
      : Encoder[RetryDetails.WillDelayAndRetry] =
    Encoder.forProduct5(
      "nextDelay",
      "retriesSoFar",
      "cumulativeDelay",
      "givingUp",
      "upcomingDelay"
    )(x =>
      (
        x.nextDelay,
        x.retriesSoFar,
        x.cumulativeDelay,
        x.givingUp,
        x.upcomingDelay
      )
    )

  implicit val retryDetailsEncoder: Encoder[RetryDetails] = {
    case x: RetryDetails.GivingUp          => x.asJson
    case x: RetryDetails.WillDelayAndRetry => x.asJson
  }

}
