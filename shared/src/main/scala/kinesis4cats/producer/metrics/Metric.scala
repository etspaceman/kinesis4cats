package kinesis4cats.producer.metrics

import scala.concurrent.duration.FiniteDuration

import java.time.Instant

import cats.effect.Async
import cats.syntax.all._

import kinesis4cats.models.ShardId
import kinesis4cats.models.StreamArn

final case class Metric(
    name: String,
    dimensions: List[Dimension],
    timestamp: Instant,
    value: Double,
    unit: StandardUnit
)

object Metric {
  private def getDimensions(
      granularity: Granularity,
      streamArn: StreamArn,
      shardId: ShardId,
      includeShard: Boolean = true
  ): List[Dimension] = granularity match {
    case Granularity.Global => Nil
    case Granularity.Stream => List(Dimension.Stream(streamArn))
    case Granularity.Shard =>
      if (includeShard)
        List(Dimension.Stream(streamArn), Dimension.Shard(shardId))
      else List(Dimension.Stream(streamArn))
  }

  def userRecordsReceived[F[_]](
      x: Int,
      granularity: Granularity,
      level: Level,
      streamArn: StreamArn,
      shardId: ShardId
  )(implicit F: Async[F]): F[Option[Metric]] =
    if (level.isDetailed)
      F.realTime
        .map(d => Instant.EPOCH.plusNanos(d.toNanos))
        .map(now =>
          Some(
            Metric(
              "UserRecordsReceived",
              getDimensions(granularity, streamArn, shardId, false),
              now,
              x.toDouble,
              StandardUnit.Count
            )
          )
        )
    else F.pure(None)

  def userRecordsPending[F[_]](
      x: Int,
      granularity: Granularity,
      level: Level,
      streamArn: StreamArn,
      shardId: ShardId
  )(implicit F: Async[F]): F[Option[Metric]] =
    if (level.isDetailed)
      F.realTime
        .map(d => Instant.EPOCH.plusNanos(d.toNanos))
        .map(now =>
          Some(
            Metric(
              "UserRecordsPending",
              getDimensions(granularity, streamArn, shardId, false),
              now,
              x.toDouble,
              StandardUnit.Count
            )
          )
        )
    else F.pure(None)

  def userRecordsPut[F[_]](
      x: Int,
      granularity: Granularity,
      level: Level,
      streamArn: StreamArn,
      shardId: ShardId
  )(implicit F: Async[F]): F[Option[Metric]] =
    if (level.isSummary)
      F.realTime
        .map(d => Instant.EPOCH.plusNanos(d.toNanos))
        .map(now =>
          Some(
            Metric(
              "UserRecordsPut",
              getDimensions(granularity, streamArn, shardId),
              now,
              x.toDouble,
              StandardUnit.Count
            )
          )
        )
    else F.pure(None)

  def userRecordsDataPut[F[_]](
      x: Int,
      granularity: Granularity,
      level: Level,
      streamArn: StreamArn,
      shardId: ShardId
  )(implicit F: Async[F]): F[Option[Metric]] =
    if (level.isDetailed)
      F.realTime
        .map(d => Instant.EPOCH.plusNanos(d.toNanos))
        .map(now =>
          Some(
            Metric(
              "UserRecordsDataPut",
              getDimensions(granularity, streamArn, shardId),
              now,
              x.toDouble,
              StandardUnit.Bytes
            )
          )
        )
    else F.pure(None)

  def kinesisRecordsPut[F[_]](
      x: Int,
      granularity: Granularity,
      level: Level,
      streamArn: StreamArn,
      shardId: ShardId
  )(implicit F: Async[F]): F[Option[Metric]] =
    if (level.isSummary)
      F.realTime
        .map(d => Instant.EPOCH.plusNanos(d.toNanos))
        .map(now =>
          Some(
            Metric(
              "KinesisRecordsPut",
              getDimensions(granularity, streamArn, shardId),
              now,
              x.toDouble,
              StandardUnit.Count
            )
          )
        )
    else F.pure(None)

  def kinesisRecordsDataPut[F[_]](
      x: Int,
      granularity: Granularity,
      level: Level,
      streamArn: StreamArn,
      shardId: ShardId
  )(implicit F: Async[F]): F[Option[Metric]] =
    if (level.isDetailed)
      F.realTime
        .map(d => Instant.EPOCH.plusNanos(d.toNanos))
        .map(now =>
          Some(
            Metric(
              "KinesisRecordsDataPut",
              getDimensions(granularity, streamArn, shardId),
              now,
              x.toDouble,
              StandardUnit.Bytes
            )
          )
        )
    else F.pure(None)

  def errorsByCode[F[_]](
      x: Int,
      code: String,
      granularity: Granularity,
      level: Level,
      streamArn: StreamArn,
      shardId: ShardId
  )(implicit F: Async[F]): F[Option[Metric]] =
    if (level.isSummary)
      F.realTime
        .map(d => Instant.EPOCH.plusNanos(d.toNanos))
        .map(now =>
          Some(
            Metric(
              "ErrorsByCode",
              Dimension
                .ErrorCode(code) +: getDimensions(
                granularity,
                streamArn,
                shardId
              ),
              now,
              x.toDouble,
              StandardUnit.Count
            )
          )
        )
    else F.pure(None)

  def allErrors[F[_]](
      x: Int,
      granularity: Granularity,
      level: Level,
      streamArn: StreamArn,
      shardId: ShardId
  )(implicit F: Async[F]): F[Option[Metric]] =
    if (level.isSummary)
      F.realTime
        .map(d => Instant.EPOCH.plusNanos(d.toNanos))
        .map(now =>
          Some(
            Metric(
              "AllErrors",
              getDimensions(granularity, streamArn, shardId),
              now,
              x.toDouble,
              StandardUnit.Count
            )
          )
        )
    else F.pure(None)

  def retriesPerRecord[F[_]](
      x: Int,
      granularity: Granularity,
      level: Level,
      streamArn: StreamArn,
      shardId: ShardId
  )(implicit F: Async[F]): F[Option[Metric]] =
    if (level.isDetailed)
      F.realTime
        .map(d => Instant.EPOCH.plusNanos(d.toNanos))
        .map(now =>
          Some(
            Metric(
              "RetriesPerRecord",
              getDimensions(granularity, streamArn, shardId),
              now,
              x.toDouble,
              StandardUnit.Count
            )
          )
        )
    else F.pure(None)

  def bufferingTime[F[_]](
      x: FiniteDuration,
      granularity: Granularity,
      level: Level,
      streamArn: StreamArn,
      shardId: ShardId
  )(implicit F: Async[F]): F[Option[Metric]] =
    if (level.isSummary)
      F.realTime
        .map(d => Instant.EPOCH.plusNanos(d.toNanos))
        .map(now =>
          Some(
            Metric(
              "BufferingTime",
              getDimensions(granularity, streamArn, shardId),
              now,
              x.toMillis.toDouble,
              StandardUnit.Milliseconds
            )
          )
        )
    else F.pure(None)

  def requestTime[F[_]](
      x: FiniteDuration,
      granularity: Granularity,
      level: Level,
      streamArn: StreamArn,
      shardId: ShardId
  )(implicit F: Async[F]): F[Option[Metric]] =
    if (level.isDetailed)
      F.realTime
        .map(d => Instant.EPOCH.plusNanos(d.toNanos))
        .map(now =>
          Some(
            Metric(
              "Request Time",
              getDimensions(granularity, streamArn, shardId),
              now,
              x.toMillis.toDouble,
              StandardUnit.Milliseconds
            )
          )
        )
    else F.pure(None)

  def userRecordsPerKinesisRecord[F[_]](
      x: Int,
      granularity: Granularity,
      level: Level,
      streamArn: StreamArn,
      shardId: ShardId
  )(implicit F: Async[F]): F[Option[Metric]] =
    if (level.isDetailed)
      F.realTime
        .map(d => Instant.EPOCH.plusNanos(d.toNanos))
        .map(now =>
          Some(
            Metric(
              "User Records per Kinesis Record",
              getDimensions(granularity, streamArn, shardId),
              now,
              x.toDouble,
              StandardUnit.Count
            )
          )
        )
    else F.pure(None)

  def amazonKinesisRecordsPerPutRecordsRequest[F[_]](
      x: Int,
      granularity: Granularity,
      level: Level,
      streamArn: StreamArn,
      shardId: ShardId
  )(implicit F: Async[F]): F[Option[Metric]] =
    if (level.isDetailed)
      F.realTime
        .map(d => Instant.EPOCH.plusNanos(d.toNanos))
        .map(now =>
          Some(
            Metric(
              "Amazon Kinesis Records per PutRecordsRequest",
              getDimensions(granularity, streamArn, shardId, false),
              now,
              x.toDouble,
              StandardUnit.Count
            )
          )
        )
    else F.pure(None)

  def userRecordsPerPutRecordsRequest[F[_]](
      x: Int,
      granularity: Granularity,
      level: Level,
      streamArn: StreamArn,
      shardId: ShardId
  )(implicit F: Async[F]): F[Option[Metric]] =
    if (level.isDetailed)
      F.realTime
        .map(d => Instant.EPOCH.plusNanos(d.toNanos))
        .map(now =>
          Some(
            Metric(
              "User Records per PutRecordsRequest",
              getDimensions(granularity, streamArn, shardId, false),
              now,
              x.toDouble,
              StandardUnit.Count
            )
          )
        )
    else F.pure(None)
}
