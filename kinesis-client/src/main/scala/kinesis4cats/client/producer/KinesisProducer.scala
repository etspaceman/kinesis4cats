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
import cats.effect.Resource
import cats.effect._
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

/** A [[kinesis4cats.producer.Producer Producer]] implementation that leverages
  * the [[kinesis4cats.client.KinesisClient KinesisClient]]
  *
  * @param logger
  *   [[org.typelevel.log4cats.StructuredLogger StructuredLogger]] instance, for
  *   logging
  * @param shardMapCache
  *   [[kinesis.producer.ShardMapCache ShardMapCache]]
  * @param config
  *   [[kinesis.producer.Producer.Config Producer.Config]]
  * @param underlying
  *   [[kinesis4cats.client.KinesisClient KinesisClient]]
  * @param F
  *   [[cats.effect.Async Async]]
  * @param LE
  *   [[kinesis4cats.producer.Producer.LogEncoders Producer.LogEncoders]]
  */
final class KinesisProducer[F[_]] private[kinesis4cats] (
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

  override protected def asPutRequest(
      records: NonEmptyList[Rec]
  ): PutRecordsRequest =
    PutRecordsRequest
      .builder()
      .records(records.toList.map(toEntry).asJava)
      .maybeTransform(config.streamNameOrArn.streamName)(_.streamName(_))
      .maybeTransform(config.streamNameOrArn.streamArn.map(_.streamArn))(
        _.streamARN(_)
      )
      .build()

  override protected def failedRecords(
      records: NonEmptyList[Rec],
      resp: PutRecordsResponse
  ): Option[NonEmptyList[Producer.FailedRecord]] =
    NonEmptyList.fromList(
      resp.records().asScala.toList.zipWithIndex.zip(records.toList).collect {
        case ((respEntry, respIndex), record)
            if Option(respEntry.errorCode()).nonEmpty =>
          Producer.FailedRecord(
            record,
            respEntry.errorCode(),
            respEntry.errorMessage(),
            respIndex
          )
      }
    )
}

object KinesisProducer {

  private[kinesis4cats] def getShardMap[F[_]](
      client: KinesisClient[F],
      streamNameOrArn: models.StreamNameOrArn
  )(implicit
      F: Async[F]
  ): F[Either[ShardMapCache.Error, ShardMap]] =
    F.realTime
      .map(d => Instant.EPOCH.plusNanos(d.toNanos))
      .flatMap(now =>
        client
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
                  now
                )
            )
          )
      )

  /** Basic constructor for the
    * [[kinesis4cats.client.producer.KinesisProducer KinesisProducer]]
    *
    * @param config
    *   [[kinesis4cats.producer.Producer.Config Producer.Config]]
    * @param _underlying
    *   [[https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/kinesis/KinesisAsyncClient.html KinesisAsyncClient]]
    *   instance
    * @param F
    *   [[cats.effect.Async Async]]
    * @param LE
    *   [[kinesis4cats.producer.Producer.LogEncoders Producer.LogEncoders]]
    * @param KLE
    *   [[kinesis4cats.client.KinesisClient.LogEncoders KinesisClient.LogEncoders]]
    * @param SLE
    *   [[kinesis4cats.producer.ShardMapCache.LogEncoders ShardMapCache.LogEncoders]]
    * @return
    *   [[cats.effect.Resource Resource]] of
    *   [[kinesis4cats.client.producer.KinesisProducer KinesisProducer]]
    */
  def apply[F[_]](config: Producer.Config, _underlying: KinesisAsyncClient)(
      implicit
      F: Async[F],
      LE: Producer.LogEncoders,
      KLE: KinesisClient.LogEncoders,
      SLE: ShardMapCache.LogEncoders
  ): Resource[F, KinesisProducer[F]] =
    KinesisClient[F](_underlying).flatMap(apply(config, _))

  /** Basic constructor for the
    * [[kinesis4cats.client.producer.KinesisProducer KinesisProducer]]
    *
    * @param config
    *   [[kinesis4cats.producer.Producer.Config Producer.Config]]
    * @param underlying
    *   [[kinesis4cats.client.KinesisClient KinesisClient]] instance
    * @param F
    *   [[cats.effect.Async Async]]
    * @param LE
    *   [[kinesis4cats.producer.Producer.LogEncoders Producer.LogEncoders]]
    * @param SLE
    *   [[kinesis4cats.producer.ShardMapCache.LogEncoders ShardMapCache.LogEncoders]]
    * @return
    *   [[cats.effect.Resource Resource]] of
    *   [[kinesis4cats.client.producer.KinesisProducer KinesisProducer]]
    */
  def apply[F[_]](config: Producer.Config, underlying: KinesisClient[F])(
      implicit
      F: Async[F],
      LE: Producer.LogEncoders,
      SLE: ShardMapCache.LogEncoders
  ): Resource[F, KinesisProducer[F]] = for {
    logger <- Slf4jLogger.create[F].toResource
    shardMapCache <- ShardMapCache[F](
      config.shardMapCacheConfig,
      getShardMap(underlying, config.streamNameOrArn),
      Slf4jLogger.create[F].widen
    )
    producer = new KinesisProducer[F](logger, shardMapCache, config, underlying)
  } yield producer
}
