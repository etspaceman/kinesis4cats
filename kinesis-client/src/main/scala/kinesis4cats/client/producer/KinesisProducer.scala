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

import java.time.Instant

import cats.data.NonEmptyList
import cats.effect.Async
import cats.effect.kernel.Resource
import cats.effect.syntax.all._
import cats.syntax.all._
import org.typelevel.log4cats.StructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient
import software.amazon.awssdk.services.kinesis.model._

import kinesis4cats.models
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
) extends Producer[F, PutRecordsRequest, PutRecordsResponse] {

  override protected def putImpl(
      req: PutRecordsRequest
  ): F[PutRecordsResponse] = underlying.putRecords(req)

  def toEntry(record: Rec): PutRecordsRequestEntry =
    PutRecordsRequestEntry
      .builder()
      .data(SdkBytes.fromByteArray(record.data))
      .partitionKey(record.partitionKey)
      .maybeTransform(record.explicitHashKey)(_.explicitHashKey(_))
      .build()

  override protected def asPutRequest(req: PutRequest): PutRecordsRequest =
    PutRecordsRequest
      .builder()
      .records(req.records.toList.map(toEntry).asJava)
      .maybeTransform(config.streamNameOrArn.streamName)(_.streamName(_))
      .maybeTransform(config.streamNameOrArn.streamArn.map(_.streamArn))(
        _.streamARN(_)
      )
      .build()

  override protected def failedRecords(
      req: PutRequest,
      resp: PutRecordsResponse
  ): Option[NonEmptyList[Producer.FailedRecord]] =
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

object KinesisProducer {

  private def getShardMap[F[_]](
      client: KinesisClient[F],
      streamNameOrArn: models.StreamNameOrArn
  )(implicit
      F: Async[F]
  ): F[Either[ShardMapCache.Error, ShardMap]] = client
    .listShards(
      ListShardsRequest
        .builder()
        .shardFilter(
          ShardFilter.builder().`type`(ShardFilterType.AT_LATEST).build()
        )
        .maybeTransform(streamNameOrArn.streamName)(_.streamName(_))
        .maybeTransform(streamNameOrArn.streamArn.map(_.streamArn))(
          _.streamARN(_)
        )
        .build()
    )
    .attempt
    .map(
      _.bimap(
        ShardMapCache.ListShardsError(_),
        resp =>
          ShardMap(
            resp
              .shards()
              .asScala
              .toList
              .map(x =>
                ShardMapRecord(
                  models.ShardId(
                    x.shardId()
                  ),
                  models.HashKeyRange(
                    BigInt(x.hashKeyRange().endingHashKey()),
                    BigInt(x.hashKeyRange().startingHashKey())
                  )
                )
              ),
            Instant.now()
          )
      )
    )

  def apply[F[_]](config: Producer.Config, _underlying: KinesisAsyncClient)(
      implicit
      F: Async[F],
      LE: Producer.LogEncoders,
      KLE: KinesisClient.LogEncoders,
      SLE: ShardMapCache.LogEncoders
  ): Resource[F, KinesisProducer[F]] = for {
    logger <- Slf4jLogger.create[F].toResource
    underlying <- KinesisClient[F](_underlying)
    shardMapCache <- ShardMapCache[F](
      config.shardMapCacheConfig,
      getShardMap(underlying, config.streamNameOrArn),
      Slf4jLogger.create[F].widen
    )
    producer = new KinesisProducer[F](logger, shardMapCache, config, underlying)
  } yield producer
}
