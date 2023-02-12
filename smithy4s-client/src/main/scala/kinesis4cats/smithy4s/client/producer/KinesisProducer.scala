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
import cats.effect.Async
import cats.effect.kernel.Resource
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

final class KinesisProducer[F[_]] private[kinesis4cats] (
    override val logger: StructuredLogger[F],
    override val shardMapCache: ShardMapCache[F],
    override val config: Producer.Config,
    underlying: KinesisClient[F]
)(implicit
    F: Async[F],
    LE: Producer.LogEncoders
) extends Producer[F, PutRecordsInput, PutRecordsOutput] {

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

  override protected def asPutRequest(req: PutRequest): PutRecordsInput =
    PutRecordsInput(
      req.records.toList.map(toEntry),
      config.streamNameOrArn.streamName.map(StreamName(_)),
      config.streamNameOrArn.streamArn.map(x => StreamARN(x.streamArn))
    )

  override protected def failedRecords(
      req: PutRequest,
      resp: PutRecordsOutput
  ): Option[NonEmptyList[Producer.FailedRecord]] =
    NonEmptyList.fromList(
      resp.records.zip(req.records.toList).collect {
        case (respEntry, record)
            if respEntry.errorCode.nonEmpty && respEntry.errorMessage.nonEmpty =>
          Producer.FailedRecord(
            record,
            respEntry.errorCode.get.value,
            respEntry.errorMessage.get.value
          )
      }
    )
}

object KinesisProducer {

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
              Instant.now()
            )
        )
      )

  def apply[F[_]](
      config: Producer.Config,
      client: Client[F],
      region: AwsRegion,
      loggerF: Async[F] => F[StructuredLogger[F]] = (f: Async[F]) =>
        f.pure(NoOpLogger[F](f)),
      credsF: (
          SimpleHttpClient[F],
          Async[F]
      ) => Resource[F, F[AwsCredentials]] =
        (x: SimpleHttpClient[F], f: Async[F]) =>
          AwsCredentialsProvider.default[F](x)(f)
  )(implicit
      F: Async[F],
      LE: Producer.LogEncoders,
      KLE: KinesisClient.LogEncoders[F],
      SLE: ShardMapCache.LogEncoders
  ): Resource[F, KinesisProducer[F]] = for {
    logger <- loggerF(F).toResource
    underlying <- KinesisClient[F](client, region, loggerF, credsF)
    shardMapCache <- ShardMapCache[F](
      config.shardMapCacheConfig,
      getShardMap(underlying, config.streamNameOrArn),
      loggerF(F)
    )
    producer = new KinesisProducer[F](logger, shardMapCache, config, underlying)
  } yield producer
}
