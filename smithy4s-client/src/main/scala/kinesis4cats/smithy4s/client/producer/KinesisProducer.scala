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
            respEntry.errorCode.get,
            respEntry.errorMessage.get,
            index
          )
      }
    )
}

object KinesisProducer {

  final case class Builder[F[_]] private (
      config: Producer.Config[F],
      client: Client[F],
      region: AwsRegion,
      logger: StructuredLogger[F],
      credentialsResourceF: SimpleHttpClient[F] => Resource[F, F[
        AwsCredentials
      ]],
      encoders: LogEncoders[F],
      logRequestsResponses: Boolean
  )(implicit F: Async[F]) {
    def withConfig(config: Producer.Config[F]): Builder[F] =
      copy(config = config)
    def transformConfig(f: Producer.Config[F] => Producer.Config[F]) = copy(
      config = f(config)
    )
    def withClient(client: Client[F]): Builder[F] = copy(client = client)
    def withRegion(region: AwsRegion): Builder[F] = copy(region = region)
    def withLogger(logger: StructuredLogger[F]): Builder[F] =
      copy(logger = logger)
    def withCredentials(
        credentialsResourceF: SimpleHttpClient[F] => Resource[F, F[
          AwsCredentials
        ]]
    ): Builder[F] =
      copy(credentialsResourceF = credentialsResourceF)
    def withLogEncoders(encoders: LogEncoders[F]): Builder[F] =
      copy(encoders = encoders)
    def withLogRequestsResponses(logRequestsResponses: Boolean): Builder[F] =
      copy(logRequestsResponses = logRequestsResponses)
    def enableLogging: Builder[F] = withLogRequestsResponses(true)
    def disableLogging: Builder[F] = withLogRequestsResponses(false)

    def build: Resource[F, KinesisProducer[F]] = for {
      client <- KinesisClient.Builder
        .default[F](client, region)
        .withLogger(logger)
        .withCredentials(credentialsResourceF)
        .withLogEncoders(encoders.kinesisClientLogEncoders)
        .withLogRequestsResponses(logRequestsResponses)
        .build
      shardMapCache <- ShardMapCache.Builder
        .default(getShardMap(client, config.streamNameOrArn), logger)
        .withLogEncoders(encoders.producerLogEncoders.shardMapLogEncoders)
        .build
    } yield new KinesisProducer[F](
      logger,
      shardMapCache,
      config,
      client,
      encoders.producerLogEncoders
    )
  }

  object Builder {
    def default[F[_]](
        streamNameOrArn: models.StreamNameOrArn,
        client: Client[F],
        region: AwsRegion
    )(implicit F: Async[F]): Builder[F] = Builder[F](
      Producer.Config.default(streamNameOrArn),
      client,
      region,
      NoOpLogger[F],
      backend => AwsCredentialsProvider.default(backend),
      LogEncoders.show[F],
      true
    )

    @annotation.unused
    private def unapply[F[_]](builder: Builder[F]): Unit = ()
  }

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
}
