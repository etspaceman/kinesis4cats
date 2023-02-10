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

package kinesis4cats.client
package producer

import scala.jdk.CollectionConverters._

import cats.data.NonEmptyList
import cats.effect.kernel.Async
import org.typelevel.log4cats.StructuredLogger
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.kinesis.model._

import kinesis4cats.producer.{Record => Rec, _}
import kinesis4cats.syntax.id._

final class KinesisProducer[F[_]] private (
    override val logger: StructuredLogger[F],
    override val shardMapCache: ShardMapCache[F],
    override val config: Producer.Config,
    underlying: KinesisClient[F]
)(implicit
    F: Async[F],
    LE: Producer.LogEncoders
) extends Producer[
      F,
      PutRecordRequest,
      PutRecordResponse,
      PutRecordsRequest,
      PutRecordsResponse
    ] {

  override protected def putImpl(req: PutRecordRequest): F[PutRecordResponse] =
    underlying.putRecord(req)

  override protected def putNImpl(
      req: PutRecordsRequest
  ): F[PutRecordsResponse] = underlying.putRecords(req)

  override protected def asPutRequest(req: PutRequest): PutRecordRequest =
    req match {
      case PutRequest.Arn(streamArn, record) =>
        PutRecordRequest
          .builder()
          .streamARN(streamArn.streamArn)
          .data(SdkBytes.fromByteArray(record.data))
          .partitionKey(req.record.partitionKey)
          .maybeTransform(req.record.explicitHashKey)(_.explicitHashKey(_))
          .build()

      case PutRequest.Name(streamName, record) =>
        PutRecordRequest
          .builder()
          .streamName(streamName)
          .data(SdkBytes.fromByteArray(record.data))
          .partitionKey(req.record.partitionKey)
          .maybeTransform(req.record.explicitHashKey)(_.explicitHashKey(_))
          .build()
    }

  def toEntry(record: Rec): PutRecordsRequestEntry =
    PutRecordsRequestEntry
      .builder()
      .data(SdkBytes.fromByteArray(record.data))
      .partitionKey(record.partitionKey)
      .maybeTransform(record.explicitHashKey)(_.explicitHashKey(_))
      .build()

  override protected def asPutNRequest(req: PutNRequest): PutRecordsRequest =
    req match {
      case PutNRequest.Arn(streamArn, records) =>
        PutRecordsRequest
          .builder()
          .streamARN(streamArn.streamArn)
          .records(records.toList.map(toEntry).asJava)
          .build()

      case PutNRequest.Name(streamName, records) =>
        PutRecordsRequest
          .builder()
          .streamName(streamName)
          .records(records.toList.map(toEntry).asJava)
          .build()
    }

  override protected def failedRecords(
      req: PutNRequest,
      resp: PutRecordsResponse
  ): Option[NonEmptyList[Producer.FailedRecord]] = {
    NonEmptyList.fromList(
      resp.records().asScala.toList.zip(req.records.toList).collect {
        case (respEntry, record) if Option(respEntry.errorCode()).nonEmpty =>
          Producer.FailedRecord(
            record,
            respEntry.errorCode(),
            respEntry.errorMessage()
          )
      }
    )
  }
}
