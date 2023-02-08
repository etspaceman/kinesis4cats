package kinesis4cats.producer

import kinesis4cats.models._

import cats.effect.Ref
import cats.effect.Async
import cats.syntax.all._
import cats.effect.syntax.all._
import java.security.MessageDigest
import java.nio.charset.StandardCharsets
import java.time.Instant
import org.typelevel.log4cats.StructuredLogger
import kinesis4cats.logging.LogContext
import kinesis4cats.logging.LogEncoder
import scala.concurrent.duration.FiniteDuration

class ShardMapCache[F[_]] private (
    config: ShardMapCache.Config,
    logger: StructuredLogger[F],
    shardMapRef: Ref[F, ShardMap],
    shardMapF: F[Either[ShardMapCache.Error, ShardMap]]
)(implicit
    F: Async[F],
    LE: ShardMapCache.LogEncoders
) {
  import LE._
  def shardForPartitionKey(
      digest: MessageDigest,
      partitionKey: String
  ): F[Either[ShardMapCache.Error, ShardId]] =
    shardMapRef.get.map(_.shardForPartitionKey(digest, partitionKey))

  def refresh(): F[Either[ShardMapCache.Error, Unit]] = {
    val ctx = LogContext()
    for {
      newMap <- shardMapF
      res <- newMap.bitraverse(
        e =>
          logger
            .error(ctx.context, e)("Error retrieving newest shard map")
            .as(e),
        x =>
          for {
            _ <- logger.debug(ctx.context)(
              "Successfully retrieved new shard map"
            )
            _ <- logger.trace(ctx.addEncoded("shardMap", x).context)(
              "Logging shard map"
            )
            _ <- shardMapRef.set(x)
          } yield ()
      )
    } yield res
  }

  private def start() = refresh()
    .flatMap(_ => F.sleep(config.refreshInterval))
    .foreverM
    .background

}

object ShardMapCache {
  def apply[F[_]](
      config: Config,
      shardMapF: F[Either[Error, ShardMap]],
      loggerF: F[StructuredLogger[F]]
  )(implicit
      F: Async[F],
      LE: ShardMapCache.LogEncoders
  ) = for {
    logger <- loggerF.toResource
    ref <- Ref.of[F, ShardMap](ShardMap.empty).toResource
    service = new ShardMapCache[F](config, logger, ref, shardMapF)
    _ <- service.start()
  } yield service

  final class LogEncoders(implicit val shardMapLogEncoder: LogEncoder[ShardMap])

  final case class Config(refreshInterval: FiniteDuration)

  sealed abstract class Error(msg: String) extends Exception(msg)
  final case class ShardForPartitionKeyNotFound(partitionKey: String)
      extends Error(s"Could not find shard for partition key ${partitionKey}")

}

final case class ShardMap(shards: List[ShardMapRecord], lastUpdated: Instant) {
  def shardForPartitionKey(
      digest: MessageDigest,
      partitionKey: String
  ): Either[ShardMapCache.Error, ShardId] = {
    val hashBytes = digest.digest(partitionKey.getBytes(StandardCharsets.UTF_8))
    val hashKey = BigInt.apply(1, hashBytes)
    ShardMap.findShard(partitionKey, hashKey, shards)
  }
}

object ShardMap {
  @annotation.tailrec
  def findShard(
      partitionKey: String,
      hashKey: BigInt,
      shards: List[ShardMapRecord]
  ): Either[ShardMapCache.Error, ShardId] = shards match {
    case Nil => Left(ShardMapCache.ShardForPartitionKeyNotFound(partitionKey))
    case h :: t =>
      if (h.hashKeyRange.isBetween(hashKey)) Right(h.shardId)
      else findShard(partitionKey, hashKey, t)
  }

  def empty = ShardMap(List.empty, Instant.now())


}

final case class ShardMapRecord(shardId: ShardId, hashKeyRange: HashKeyRange)
