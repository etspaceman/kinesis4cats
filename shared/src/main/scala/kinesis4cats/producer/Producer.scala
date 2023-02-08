package kinesis4cats.producer

import cats.syntax.all._
import kinesis4cats.models.StreamArn
import org.typelevel.log4cats.StructuredLogger
import cats.effect.Async


abstract class Producer[F[_]](implicit F: Async[F]) {
  def logger: StructuredLogger[F]
  def shardMapCache: ShardMapCache[F]

  def putImpl[A](streamName: String, record: Record): F[A]
  def putImpl[A](streamArn: StreamArn, record: Record): F[A]
  def putNImpl[A](streamName: String, records: List[Record]): F[A]
  def putNImpl[A](streamArn: StreamArn, records: List[Record]): F[A]

  def put[A](streamName: String, record: Record): F[A] =
    putImpl(streamName, record)

  def put[A](streamArn: StreamArn, record: Record): F[A] =
    putImpl(streamArn, record)

  def putN[A](
      streamName: String,
      records: List[Record],
      aggregate: Boolean
  ): F[A] =
    putNImpl(streamName, records)

  def putN[A](
      streamArn: StreamArn,
      records: List[Record],
      aggregate: Boolean
  ): F[A] =
    putNImpl(streamArn, records)
}
