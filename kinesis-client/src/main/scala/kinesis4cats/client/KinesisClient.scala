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

import java.util.concurrent.CompletableFuture

import cats.effect.syntax.all._
import cats.effect.{Async, Resource}
import cats.syntax.all._
import org.typelevel.log4cats.StructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient
import software.amazon.awssdk.services.kinesis.model._
import software.amazon.awssdk.services.kinesis.paginators._

import kinesis4cats.logging.{LogContext, LogEncoder}

/** Wrapper class for the
  * [[https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/kinesis/KinesisAsyncClient.html KinesisAsyncClient]],
  * returning F as [[cats.effect.Async Async]] results (instead of
  * CompletableFuture)
  *
  * @param client
  *   [[https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/kinesis/KinesisAsyncClient.html KinesisAsyncClient]]
  * @param logger
  *   [[org.typelevel.log4cats.StructuredLogger StructuredLogger]] in use
  * @param F
  *   F with a [[cats.effect.Async Async]] instance
  * @param LE
  *   [[kinesis4cats.client.KinesisClientLogEncoders KinesisClientLogEncoders]]
  */
class KinesisClient[F[_]] private (
    client: KinesisAsyncClient,
    logger: StructuredLogger[F]
)(implicit
    F: Async[F],
    LE: KinesisClientLogEncoders
) {

  private def requestLogs[A: LogEncoder](
      method: String,
      request: A,
      ctx: LogContext
  ): F[Unit] =
    for {
      _ <- logger.debug(ctx.context)(s"Received $method request")
      _ <- logger.trace(ctx.addEncoded("request", request).context)(
        s"Logging $method request"
      )
    } yield ()

  private def responseLogs[A: LogEncoder](
      method: String,
      response: A,
      ctx: LogContext
  ): F[Unit] = for {
    _ <- logger.debug(ctx.context)(s"Completed $method request")
    _ <- logger.trace(ctx.addEncoded("response", response).context)(
      s"Logging $method response"
    )
  } yield ()

  private def runRequest[A: LogEncoder, B: LogEncoder](
      method: String,
      request: A
  )(fn: (KinesisAsyncClient, A) => CompletableFuture[B]): F[B] = {
    val ctx = LogContext()
    for {
      _ <- requestLogs(method, request, ctx)
      response <- F.fromCompletableFuture(
        F.delay(fn(client, request))
      )
      _ <- responseLogs(method, response, ctx)
    } yield response
  }

  private def runVoidRequest[A: LogEncoder](
      method: String,
      request: A
  )(fn: (KinesisAsyncClient, A) => CompletableFuture[Void]): F[Unit] = {
    val ctx = LogContext()
    for {
      _ <- requestLogs(method, request, ctx)
      response <- F
        .fromCompletableFuture(
          F.delay(fn(client, request))
        )
        .void
      _ <- responseLogs(method, "no response object", ctx)
    } yield response
  }

  private def runRequest[A: LogEncoder](
      method: String
  )(fn: KinesisAsyncClient => CompletableFuture[A]): F[A] = {
    val ctx = LogContext()
    for {
      _ <- requestLogs(method, "no request", ctx)
      response <- F.fromCompletableFuture(
        F.delay(fn(client))
      )
      _ <- responseLogs(method, response, ctx)
    } yield response
  }

  private def runPaginatedRequest[A: LogEncoder, B](
      method: String,
      request: A
  )(fn: (KinesisAsyncClient, A) => B): F[B] = {
    val ctx = LogContext()
    for {
      _ <- requestLogs(method, request, ctx)
      response <- F.delay(fn(client, request))
      _ <- responseLogs(method, "paginated response object", ctx)
    } yield response
  }

  private def runPaginatedRequest[A](
      method: String
  )(fn: KinesisAsyncClient => A): F[A] = {
    val ctx = LogContext()
    for {
      _ <- requestLogs(method, "no request", ctx)
      response <- F.delay(fn(client))
      _ <- responseLogs(method, "paginated response object", ctx)
    } yield response
  }

  import LE._

  def addTagsToStream(
      request: AddTagsToStreamRequest
  ): F[AddTagsToStreamResponse] =
    runRequest("addTagsToStream", request)(_.addTagsToStream(_))

  def createStream(request: CreateStreamRequest): F[CreateStreamResponse] =
    runRequest("createStream", request)(_.createStream(_))

  def decreaseStreamRetentionPeriod(
      request: DecreaseStreamRetentionPeriodRequest
  ): F[DecreaseStreamRetentionPeriodResponse] =
    runRequest("decreaseStreamRetentionPeriod", request)(
      _.decreaseStreamRetentionPeriod(_)
    )

  def deleteStream(request: DeleteStreamRequest): F[DeleteStreamResponse] =
    runRequest("deleteStream", request)(_.deleteStream(_))

  def deregisterStreamConsumer(
      request: DeregisterStreamConsumerRequest
  ): F[DeregisterStreamConsumerResponse] =
    runRequest("deregisterStreamConsumer", request)(
      _.deregisterStreamConsumer(_)
    )

  def describeLimits(
      request: DescribeLimitsRequest
  ): F[DescribeLimitsResponse] =
    runRequest("describeLimits", request)(_.describeLimits(_))

  def describeLimits(): F[DescribeLimitsResponse] =
    runRequest("describeLimits")(_.describeLimits())

  def describeStream(
      request: DescribeStreamRequest
  ): F[DescribeStreamResponse] =
    runRequest("describeStream", request)(_.describeStream(_))

  def describeStreamConsumer(
      request: DescribeStreamConsumerRequest
  ): F[DescribeStreamConsumerResponse] =
    runRequest("describeStreamConsumer", request)(_.describeStreamConsumer(_))

  def describeStreamSummary(
      request: DescribeStreamSummaryRequest
  ): F[DescribeStreamSummaryResponse] =
    runRequest("describeStreamSummary", request)(_.describeStreamSummary(_))

  def disableEnhancedMonitoring(
      request: DisableEnhancedMonitoringRequest
  ): F[DisableEnhancedMonitoringResponse] =
    runRequest("disableEnhancedMonitoring", request)(
      _.disableEnhancedMonitoring(_)
    )

  def enableEnhancedMonitoring(
      request: EnableEnhancedMonitoringRequest
  ): F[EnableEnhancedMonitoringResponse] =
    runRequest("enableEnhancedMonitoring", request)(
      _.enableEnhancedMonitoring(_)
    )

  def getRecords(request: GetRecordsRequest): F[GetRecordsResponse] =
    runRequest("getRecords", request)(_.getRecords(_))

  def getShardIterator(
      request: GetShardIteratorRequest
  ): F[GetShardIteratorResponse] =
    runRequest("getShardIterator", request)(_.getShardIterator(_))

  def increaseStreamRetentionPeriod(
      request: IncreaseStreamRetentionPeriodRequest
  ): F[IncreaseStreamRetentionPeriodResponse] =
    runRequest("increaseStreamRetentionPeriod", request)(
      _.increaseStreamRetentionPeriod(_)
    )

  def listShards(request: ListShardsRequest): F[ListShardsResponse] =
    runRequest("listShards", request)(_.listShards(_))

  def listStreamConsumers(
      request: ListStreamConsumersRequest
  ): F[ListStreamConsumersResponse] =
    runRequest("listStreamConsumers", request)(_.listStreamConsumers(_))

  def listStreamConsumersPaginator(
      request: ListStreamConsumersRequest
  ): F[ListStreamConsumersPublisher] =
    runPaginatedRequest("listStreamConsumersPaginator", request)(
      _.listStreamConsumersPaginator(_)
    )

  def listStreams(request: ListStreamsRequest): F[ListStreamsResponse] =
    runRequest("listStreams", request)(_.listStreams(_))

  def listStreams(): F[ListStreamsResponse] =
    runRequest("listStreams")(_.listStreams())

  def listStreamsPaginator(
      request: ListStreamsRequest
  ): F[ListStreamsPublisher] =
    runPaginatedRequest("listStreamsPaginator", request)(
      _.listStreamsPaginator(_)
    )

  def listStreamsPaginator(): F[ListStreamsPublisher] =
    runPaginatedRequest("listStreamsPaginator")(
      _.listStreamsPaginator()
    )

  def listTagsForStream(
      request: ListTagsForStreamRequest
  ): F[ListTagsForStreamResponse] =
    runRequest("listTagsForStream", request)(_.listTagsForStream(_))

  def mergeShards(request: MergeShardsRequest): F[MergeShardsResponse] =
    runRequest("mergeShards", request)(_.mergeShards(_))

  def putRecord(request: PutRecordRequest): F[PutRecordResponse] =
    runRequest("putRecord", request)(_.putRecord(_))

  def putRecords(request: PutRecordsRequest): F[PutRecordsResponse] =
    runRequest("putRecords", request)(_.putRecords(_))

  def registerStreamConsumer(
      request: RegisterStreamConsumerRequest
  ): F[RegisterStreamConsumerResponse] =
    runRequest("registerStreamConsumer", request)(_.registerStreamConsumer(_))

  def removeTagsFromStream(
      request: RemoveTagsFromStreamRequest
  ): F[RemoveTagsFromStreamResponse] =
    runRequest("removeTagsFromStream", request)(_.removeTagsFromStream(_))

  def splitShard(request: SplitShardRequest): F[SplitShardResponse] =
    runRequest("splitShard", request)(_.splitShard(_))

  def startStreamEncryption(
      request: StartStreamEncryptionRequest
  ): F[StartStreamEncryptionResponse] =
    runRequest("startStreamEncryption", request)(_.startStreamEncryption(_))

  def stopStreamEncryption(
      request: StopStreamEncryptionRequest
  ): F[StopStreamEncryptionResponse] =
    runRequest("stopStreamEncryption", request)(_.stopStreamEncryption(_))

  def subscribeToShard(
      request: SubscribeToShardRequest,
      responseHandler: SubscribeToShardResponseHandler
  ): F[Unit] =
    runVoidRequest("subscribeToShard", request)(
      _.subscribeToShard(_, responseHandler)
    )

  def updateShardCount(
      request: UpdateShardCountRequest
  ): F[UpdateShardCountResponse] =
    runRequest("updateShardCount", request)(_.updateShardCount(_))

  def updateStreamMode(
      request: UpdateStreamModeRequest
  ): F[UpdateStreamModeResponse] =
    runRequest("updateStreamMode", request)(_.updateStreamMode(_))
}

object KinesisClient {

  /** Constructor for the KinesisClient, as a managed
    * [[cats.effect.Resource Resource]]
    *
    * @param client
    *   [[https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/kinesis/KinesisAsyncClient.html KinesisAsyncClient]]
    * @param F
    *   F with an [[cats.effect.Async Async]] instance
    * @param LE
    *   [[kinesis4cats.client.KinesisClientLogEncoders KinesisClientLogEncoders]]
    * @return
    *   [[cats.effect.Resource Resource]] containing a
    *   [[kinesis4cats.client.KinesisClient]]
    */
  def apply[F[_]](
      client: KinesisAsyncClient
  )(implicit
      F: Async[F],
      LE: KinesisClientLogEncoders
  ): Resource[F, KinesisClient[F]] = for {
    clientResource <- Resource.fromAutoCloseable(F.pure(client))
    logger <- Slf4jLogger.create[F].toResource
  } yield new KinesisClient[F](clientResource, logger)
}
