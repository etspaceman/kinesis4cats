package kinesis4cats.client

import software.amazon.awssdk.services.kinesis.KinesisAsyncClient
import cats.effect.Async
import software.amazon.awssdk.services.kinesis.model._
import cats.effect.Resource
import software.amazon.awssdk.services.kinesis.paginators._
import cats.syntax.all._

class KinesisClient[F[_]] private (client: KinesisAsyncClient)(implicit
    F: Async[F]
) {
  def addTagsToStream(
      request: AddTagsToStreamRequest
  ): F[AddTagsToStreamResponse] =
    F.fromCompletableFuture(F.delay(client.addTagsToStream(request)))

  def createStream(request: CreateStreamRequest): F[CreateStreamResponse] =
    F.fromCompletableFuture(F.delay(client.createStream(request)))

  def decreaseStreamRetentionPeriod(
      request: DecreaseStreamRetentionPeriodRequest
  ): F[DecreaseStreamRetentionPeriodResponse] =
    F.fromCompletableFuture(
      F.delay(client.decreaseStreamRetentionPeriod(request))
    )

  def deleteStream(request: DeleteStreamRequest): F[DeleteStreamResponse] =
    F.fromCompletableFuture(F.delay(client.deleteStream(request)))

  def deregisterStreamConsumer(
      request: DeregisterStreamConsumerRequest
  ): F[DeregisterStreamConsumerResponse] =
    F.fromCompletableFuture(F.delay(client.deregisterStreamConsumer(request)))

  def describeLimits(
      request: DescribeLimitsRequest
  ): F[DescribeLimitsResponse] =
    F.fromCompletableFuture(F.delay(client.describeLimits(request)))

  def describeLimits(): F[DescribeLimitsResponse] =
    F.fromCompletableFuture(F.delay(client.describeLimits()))

  def describeStream(
      request: DescribeStreamRequest
  ): F[DescribeStreamResponse] =
    F.fromCompletableFuture(F.delay(client.describeStream(request)))

  def describeStreamConsumer(
      request: DescribeStreamConsumerRequest
  ): F[DescribeStreamConsumerResponse] =
    F.fromCompletableFuture(F.delay(client.describeStreamConsumer(request)))

  def describeStreamSummary(
      request: DescribeStreamSummaryRequest
  ): F[DescribeStreamSummaryResponse] =
    F.fromCompletableFuture(F.delay(client.describeStreamSummary(request)))

  def disableEnhancedMonitoring(
      request: DisableEnhancedMonitoringRequest
  ): F[DisableEnhancedMonitoringResponse] =
    F.fromCompletableFuture(F.delay(client.disableEnhancedMonitoring(request)))

  def enableEnhancedMonitoring(
      request: EnableEnhancedMonitoringRequest
  ): F[EnableEnhancedMonitoringResponse] =
    F.fromCompletableFuture(F.delay(client.enableEnhancedMonitoring(request)))

  def getRecords(request: GetRecordsRequest): F[GetRecordsResponse] =
    F.fromCompletableFuture(F.delay(client.getRecords(request)))

  def getShardIterator(
      request: GetShardIteratorRequest
  ): F[GetShardIteratorResponse] =
    F.fromCompletableFuture(F.delay(client.getShardIterator(request)))

  def increaseStreamRetentionPeriod(
      request: IncreaseStreamRetentionPeriodRequest
  ): F[IncreaseStreamRetentionPeriodResponse] =
    F.fromCompletableFuture(
      F.delay(client.increaseStreamRetentionPeriod(request))
    )

  def listShards(request: ListShardsRequest): F[ListShardsResponse] =
    F.fromCompletableFuture(F.delay(client.listShards(request)))

  def listStreamConsumers(
      request: ListStreamConsumersRequest
  ): F[ListStreamConsumersResponse] =
    F.fromCompletableFuture(F.delay(client.listStreamConsumers(request)))

  def listStreamConsumersPaginator(
      request: ListStreamConsumersRequest
  ): ListStreamConsumersPublisher = client.listStreamConsumersPaginator(request)

  def listStreams(request: ListStreamsRequest): F[ListStreamsResponse] =
    F.fromCompletableFuture(F.delay(client.listStreams(request)))

  def listStreams(): F[ListStreamsResponse] =
    F.fromCompletableFuture(F.delay(client.listStreams()))

  def listStreamsPaginator(request: ListStreamsRequest): ListStreamsPublisher =
    client.listStreamsPaginator(request)

  def listStreamsPaginator(): ListStreamsPublisher =
    client.listStreamsPaginator()

  def listTagsForStream(
      request: ListTagsForStreamRequest
  ): F[ListTagsForStreamResponse] =
    F.fromCompletableFuture(F.delay(client.listTagsForStream(request)))

  def mergeShards(request: MergeShardsRequest): F[MergeShardsResponse] =
    F.fromCompletableFuture(F.delay(client.mergeShards(request)))

  def putRecord(request: PutRecordRequest): F[PutRecordResponse] =
    F.fromCompletableFuture(F.delay(client.putRecord(request)))

  def putRecords(request: PutRecordsRequest): F[PutRecordsResponse] =
    F.fromCompletableFuture(F.delay(client.putRecords(request)))

  def registerStreamConsumer(
      request: RegisterStreamConsumerRequest
  ): F[RegisterStreamConsumerResponse] =
    F.fromCompletableFuture(F.delay(client.registerStreamConsumer(request)))

  def removeTagsFromStream(
      request: RemoveTagsFromStreamRequest
  ): F[RemoveTagsFromStreamResponse] =
    F.fromCompletableFuture(F.delay(client.removeTagsFromStream(request)))

  def splitShard(request: SplitShardRequest): F[SplitShardResponse] =
    F.fromCompletableFuture(F.delay(client.splitShard(request)))

  def startStreamEncryption(
      request: StartStreamEncryptionRequest
  ): F[StartStreamEncryptionResponse] =
    F.fromCompletableFuture(F.delay(client.startStreamEncryption(request)))

  def stopStreamEncryption(
      request: StopStreamEncryptionRequest
  ): F[StopStreamEncryptionResponse] =
    F.fromCompletableFuture(F.delay(client.stopStreamEncryption(request)))

  def subscribeToShard(
      request: SubscribeToShardRequest,
      responseHandler: SubscribeToShardResponseHandler
  ): F[Unit] =
    F.fromCompletableFuture(
      F.delay(client.subscribeToShard(request, responseHandler))
    ).void

  def updateShardCount(
      request: UpdateShardCountRequest
  ): F[UpdateShardCountResponse] =
    F.fromCompletableFuture(F.delay(client.updateShardCount(request)))

  def updateStreamMode(
      request: UpdateStreamModeRequest
  ): F[UpdateStreamModeResponse] =
    F.fromCompletableFuture(F.delay(client.updateStreamMode(request)))
}

object KinesisClient {
  def apply[F[_]](
      client: KinesisAsyncClient
  )(implicit F: Async[F]): Resource[F, KinesisClient[F]] =
    Resource.fromAutoCloseable(F.pure(client)).map(x => new KinesisClient[F](x))
}
