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

package kinesis4cats.smithy4s.client
package producer

import java.time.Instant

import cats.data.NonEmptyList
import cats.effect.Resource
import cats.effect._
import cats.effect.syntax.all._
import cats.syntax.all._
import com.amazonaws.kinesis._
import org.http4s.client.Client
import org.typelevel.log4cats.StructuredLogger
import org.typelevel.log4cats.noop.NoOpLogger
import smithy4s.ByteArray
import smithy4s.aws.AwsCredentialsProvider
import smithy4s.aws.SimpleHttpClient
import smithy4s.aws.kernel.AwsCredentials
import smithy4s.aws.kernel.AwsRegion

import kinesis4cats.models
import kinesis4cats.producer.{Record => Rec, _}

/** A [[kinesis4cats.producer.Producer Producer]] implementation that leverages
  * the [[kinesis4cats.smithy4s.client.KinesisClient KinesisClient]]
  *
  * @param logger
  *   [[org.typelevel.log4cats.StructuredLogger StructuredLogger]] instance, for
  *   logging
  * @param shardMapCache
  *   [[kinesis.producer.ShardMapCache ShardMapCache]]
  * @param config
  *   [[kinesis.producer.Producer.Config Producer.Config]]
  * @param underlying
  *   [[kinesis4cats.smithy4s.client.KinesisClient KinesisClient]]
  * @param F
  *   [[cats.effect.Async Async]]
  * @param LE
  *   [[kinesis4cats.producer.Producer.LogEncoders Producer.LogEncoders]]
  */
final class KinesisProducer[F[_]] private[kinesis4cats] (
    override val logger: StructuredLogger[F],
    override val shardMapCache: ShardMapCache[F],
    override val config: Producer.Config[F],
    underlying: KinesisClient[F],
    encoders: Producer.LogEncoders
)(implicit F: Async[F])
    extends Producer[F, PutRecordsInput, PutRecordsOutput](encoders) {

  override protected def putImpl(
      req: PutRecordsInput
  ): F[PutRecordsOutput] =
    underlying.putRecords(req.records, req.streamName, req.streamARN)

  def toEntry(record: Rec): PutRecordsRequestEntry =
    PutRecordsRequestEntry(
      Data(ByteArray(record.data)),
      PartitionKey(record.partitionKey),
      record.explicitHashKey.map(HashKey(_))
    )

  override protected def asPutRequest(
      records: NonEmptyList[Rec]
  ): PutRecordsInput =
    PutRecordsInput(
      records.toList.map(toEntry),
      config.streamNameOrArn.streamName.map(StreamName(_)),
      config.streamNameOrArn.streamArn.map(x => StreamARN(x.streamArn))
    )

  override protected def failedRecords(
      records: NonEmptyList[Rec],
      resp: PutRecordsOutput
  ): Option[NonEmptyList[Producer.FailedRecord]] =
    NonEmptyList.fromList(
      resp.records.zipWithIndex.zip(records.toList).collect {
        case ((respEntry, index), record)
            if respEntry.errorCode.nonEmpty && respEntry.errorMessage.nonEmpty =>
          Producer.FailedRecord(
            record,
            respEntry.errorCode.get.value,
            respEntry.errorMessage.get.value,
            index
          )
      }
    )
}

object KinesisProducer {

  final class LogEncoders[F[_]](
      val kinesisClientLogEncoders: KinesisClient.LogEncoders[F],
      val producerLogEncoders: Producer.LogEncoders
  )

  object LogEncoders {
    def show[F[_]]: LogEncoders[F] =
      new LogEncoders(KinesisClient.LogEncoders.show, Producer.LogEncoders.show)
  }

  private def getShards[F[_]](
      client: KinesisClient[F],
      streamNameOrArn: models.StreamNameOrArn
  ): F[ListShardsOutput] = client
    .listShards(
      streamName = streamNameOrArn.streamName.map(StreamName(_)),
      streamARN = streamNameOrArn.streamArn.map(x => StreamARN(x.streamArn)),
      shardFilter = Some(ShardFilter(ShardFilterType.AT_LATEST))
    )

  private[kinesis4cats] def getShardMap[F[_]](
      client: KinesisClient[F],
      streamNameOrArn: models.StreamNameOrArn
  )(implicit
      F: Async[F]
  ): F[Either[ShardMapCache.Error, ShardMap]] =
    F.realTime
      .map(d => Instant.EPOCH.plusNanos(d.toNanos))
      .flatMap(now =>
        getShards(client, streamNameOrArn).attempt
          .map(
            _.bimap(
              ShardMapCache.ListShardsError(_),
              resp =>
                ShardMap(
                  resp.shards
                    .map(
                      _.map(x =>
                        ShardMapRecord(
                          models.ShardId(
                            x.shardId.value
                          ),
                          models.HashKeyRange(
                            BigInt(x.hashKeyRange.endingHashKey.value),
                            BigInt(x.hashKeyRange.startingHashKey.value)
                          )
                        )
                      )
                    )
                    .getOrElse(List.empty),
                  now
                )
            )
          )
      )

  /** Basic constructor for the
    * [[kinesis4cats.smithy4s.client.producer.KinesisProducer KinesisProducer]]
    *
    * @param config
    *   [[kinesis4cats.producer.Producer.Config Producer.Config]]
    * @param client
    *   [[org.http4s.client.Client Client]] instance
    * @param region
    *   [[https://github.com/disneystreaming/smithy4s/blob/series/0.17/modules/aws-kernel/src/smithy4s/aws/AwsRegion.scala AwsRegion]]
    * @param loggerF
    *   [[cats.effect.Async Async]] => F of
    *   [[org.typelevel.log4cats.StructuredLogger StructuredLogger]]. Default
    *   uses [[org.typelevel.log4cats.noop.NoOpLogger NoOpLogger]]
    * @param credsF
    *   (
    *   [[https://github.com/disneystreaming/smithy4s/blob/series/0.17/modules/aws/src/smithy4s/aws/SimpleHttpClient.scala SimpleHttpClient]],
    *   [[cats.effect.Async Async]]) => F of
    *   [[https://github.com/disneystreaming/smithy4s/blob/series/0.17/modules/aws-kernel/src/smithy4s/aws/AwsCredentials.scala AwsCredentials]]
    *   Default uses
    *   [[https://github.com/disneystreaming/smithy4s/blob/series/0.17/modules/aws/src/smithy4s/aws/AwsCredentialsProvider.scala AwsCredentialsProvider.default]]
    * @param F
    *   [[cats.effect.Async Async]]
    * @param encoders
    *   [[kinesis4cats.smiithy4s.client.producer.KinesisProducer.LogEncoders KinesisProducer.LogEncoders]].
    *   Default to show instances
    * @return
    *   [[cats.effect.Resource Resource]] of
    *   [[kinesis4cats.smithy4s.client.producer.KinesisProducer KinesisProducer]]
    */
  def apply[F[_]](
      config: Producer.Config[F],
      client: Client[F],
      region: F[AwsRegion],
      loggerF: Async[F] => F[StructuredLogger[F]] = (f: Async[F]) =>
        f.pure(NoOpLogger[F](f)),
      credsF: (
          SimpleHttpClient[F],
          Async[F]
      ) => Resource[F, F[AwsCredentials]] =
        (x: SimpleHttpClient[F], f: Async[F]) =>
          AwsCredentialsProvider.default[F](x)(f),
      encoders: KinesisProducer.LogEncoders[F] =
        KinesisProducer.LogEncoders.show[F]
  )(implicit
      F: Async[F]
  ): Resource[F, KinesisProducer[F]] = for {
    logger <- loggerF(F).toResource
    underlying <- KinesisClient[F](
      client,
      region,
      loggerF,
      credsF,
      encoders = encoders.kinesisClientLogEncoders
    )
    shardMapCache <- ShardMapCache[F](
      config.shardMapCacheConfig,
      getShardMap(underlying, config.streamNameOrArn),
      loggerF(F),
      encoders.producerLogEncoders.shardMapLogEncoders
    )
    producer = new KinesisProducer[F](
      logger,
      shardMapCache,
      config,
      underlying,
      encoders.producerLogEncoders
    )
  } yield producer
}
