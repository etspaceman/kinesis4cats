package kinesis4cats.consumer
package fetch

import cats.effect.Async
import cats.effect.Ref
import cats.effect.std.Supervisor
import cats.syntax.all._
import fs2.Chunk
import fs2.Stream
import org.typelevel.log4cats.StructuredLogger

import kinesis4cats.compat.retry._
import kinesis4cats.logging.LogContext
import fs2.concurrent.Channel

abstract class Fetcher[F[_]] private[kinesis4cats] {
  protected def logger: StructuredLogger[F]
  protected def channel: Channel[F, CommittableRecord]
}

object Fetcher {
  abstract class Polling[
      F[_],
      GetRecordsReq,
      GetRecordsRes,
      GetShardIteratorReq,
      GetShardIteratorRes
  ](implicit
      F: Async[F]
  ) extends Fetcher[F] {

    def config: Polling.Config[F]

    protected def getRecords(req: GetRecordsReq): F[GetRecordsRes]

    protected def getShardIterator(
        req: GetShardIteratorReq
    ): F[GetShardIteratorRes]

    protected def initialShardIteratorReq: F[GetShardIteratorReq]

    protected def getRecordsReq(shardIterator: String): GetRecordsReq

    protected def isThrottlingError(e: Throwable): Boolean

    protected def toCommittableRecords(
        x: GetRecordsRes
    ): List[CommittableRecord]

    protected def toShardIterator(x: GetShardIteratorRes): String

    private def poll(
        iteratorCache: Ref[F, String]
    ): F[List[CommittableRecord]] = {
      val ctx = LogContext()
      for {
        shardIterator <- iteratorCache.get
        records <- retryingOnSomeErrors(
          config.throttledRetryPolicy,
          (e: Throwable) => F.delay(isThrottlingError(e)),
          (x: Throwable, _: RetryDetails) =>
            logger.warn(ctx.context, x)(
              "Throttling error getting records, retrying"
            )
        )(getRecords(getRecordsReq(shardIterator)))
      } yield toCommittableRecords(records)
    }

    override protected def channel: Channel[F, CommittableRecord] = {
      val ctx = LogContext()

      for {
        initialRequeset <- Stream.eval(initialShardIteratorReq)
        shardIterator <- Stream.eval(
          retryingOnSomeErrors(
            config.throttledRetryPolicy,
            (e: Throwable) => F.delay(isThrottlingError(e)),
            (x: Throwable, _: RetryDetails) =>
              logger.warn(ctx.context, x)(
                "Throttling error getting shard iterator, retrying"
              )
          )(getShardIterator(initialRequeset))
        )
        iteratorRef <- Stream.eval(Ref.of(toShardIterator(shardIterator)))
        res <- Stream
          .evalUnChunk(poll(iteratorRef).map(Chunk.seq))
          .prefetchN(config.cachedResponses)
      } yield res

      
    }
  }

  object Polling {
    final case class PositionState(
        position: StartingPosition
    )
    final case class Config[F[_]](
        bufferSize: Int,
        cachedResponses: Int,
        maxRecordsPerResponse: Int,
        throttledRetryPolicy: RetryPolicy[F]
    )
  }

  abstract class FanOut[F[_]] extends Fetcher[F] {
    def config: Polling.Config[F]


  }

  object FanOut {
    final case class Config()
  }
}
