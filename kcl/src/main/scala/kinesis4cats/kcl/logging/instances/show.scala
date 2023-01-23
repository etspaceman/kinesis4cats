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
package kcl.logging.instances

import cats.Show
import com.amazonaws.services.schemaregistry.common.Schema
import software.amazon.awssdk.services.kinesis.model._
import software.amazon.kinesis.lifecycle.events._
import software.amazon.kinesis.retrieval.KinesisClientRecord
import software.amazon.kinesis.retrieval.kpl.ExtendedSequenceNumber

import kinesis4cats.ShowBuilder
import kinesis4cats.kcl.processor.RecordProcessorLogEncoders
import kinesis4cats.logging.instances.show._

/** KCL [[kinesis4cats.logging.LogEncoder LogEncoder]] instances for string
  * encoding of log structures using [[cats.Show Show]]
  */
object show {
  implicit val hashKeyRangeShow: Show[HashKeyRange] = x =>
    ShowBuilder("HashKeyRange")
      .add("endingHashKey", x.endingHashKey())
      .add("startingHashKey", x.startingHashKey())
      .build

  implicit val childShardShow: Show[ChildShard] = x =>
    ShowBuilder("ChildShard")
      .add("hasParentShards", x.hasParentShards())
      .add("hashKeyRange", x.hashKeyRange())
      .add("parentShards", x.parentShards())
      .add("shardId", x.shardId())
      .build

  implicit val encryptionTypeShow: Show[EncryptionType] = x =>
    Show[String].show(x.name)

  implicit val schemaShow: Show[Schema] = x =>
    ShowBuilder("Schema")
      .add("dataFormat", x.getDataFormat())
      .add("schemaDefinition", x.getSchemaDefinition())
      .add("schemaName", x.getSchemaName())
      .build

  implicit val extendedSequenceNumberShow: Show[ExtendedSequenceNumber] = x =>
    ShowBuilder("ExtendedSequenceNumber")
      .add("sequenceNumber", x.sequenceNumber())
      .add("subSequenceNumber", x.subSequenceNumber())
      .build

  implicit val initializationInputShow: Show[InitializationInput] = x =>
    ShowBuilder("InitializationInput")
      .add("shardId", x.shardId())
      .add("extendedSequenceNumber", x.extendedSequenceNumber())
      .add(
        "pendingCheckpointSequenceNumber",
        x.pendingCheckpointSequenceNumber()
      )
      .build

  implicit val processRecordsInputShow: Show[ProcessRecordsInput] = x =>
    ShowBuilder("ProcessRecordsInput")
      .add("cacheEntryTime", x.cacheEntryTime())
      .add("cacheExitTime", x.cacheExitTime())
      .add("childShards", x.childShards())
      .add("isAtShardEnd", x.isAtShardEnd())
      .add("millisBehindLatest", x.millisBehindLatest())
      .add("timeSpentInCache", x.timeSpentInCache())
      .build

  implicit val kinesisClientRecordShow: Show[KinesisClientRecord] = x =>
    ShowBuilder("KinesisClientRecord")
      .add("aggregated", x.aggregated())
      .add("approximateArrivalTimestamp", x.approximateArrivalTimestamp())
      .add("encryptionType", x.encryptionType())
      .add("explicitHashKey", x.explicitHashKey())
      .add("partitionKey", x.partitionKey())
      .add("schema", x.schema())
      .add("sequenceNumber", x.sequenceNumber())
      .add("subSequenceNumber", x.subSequenceNumber())
      .add("data", x.data())
      .build

  implicit val recordProcessorLogEncoders: RecordProcessorLogEncoders =
    new RecordProcessorLogEncoders
}
