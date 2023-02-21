package kinesis4cats.client.producer
package fs2
package localstack

import cats.effect._
import software.amazon.awssdk.services.kinesis.model.PutRecordsResponse

import kinesis4cats.client.KinesisClient
import kinesis4cats.localstack.LocalstackConfig
import kinesis4cats.localstack.aws.v2.AwsClients
import kinesis4cats.models.StreamNameOrArn
import kinesis4cats.producer.Producer
import kinesis4cats.producer.ShardMapCache
import kinesis4cats.producer.fs2.FS2Producer

object LocalstackFS2KinesisProducer {

  /** Builds a [[kinesis4cats.client.producer.KinesisProducer KinesisProducer]]
    * that is compliant for Localstack usage. Lifecycle is managed as a
    * [[cats.effect.Resource Resource]].
    *
    * @param producerConfig
    *   [[kinesis4cats.producer.fs2.FS2Producer.Config FS2Producer.Config]]
    * @param config
    *   [[kinesis4cats.localstack.LocalstackConfig LocalstackConfig]]
    * @param callback
    *   Function that can be run after each of the put results from the
    *   underlying
    * @param F
    *   F with an [[cats.effect.Async Async]] instance
    * @param LE
    *   [[kinesis4cats.client.KinesisClient.LogEncoders LogEncoders]]
    * @param SLE
    *   [[kinesis4cats.producer.ShardMapCache.LogEncoders ShardMapCache.LogEncoders]]
    * @param PLE
    *   [[kinesis4cats.producer.Producer.LogEncoders Producer.LogEncoders]]
    * @return
    *   [[cats.effect.Resource Resource]] of
    *   [[kinesis4cats.client.producer.fs2.FS2KinesisProducer FS2KinesisProducer]]
    */
  def resource[F[_]](
      producerConfig: FS2Producer.Config,
      config: LocalstackConfig,
      callback: (Producer.Res[PutRecordsResponse], Async[F]) => F[Unit]
  )(implicit
      F: Async[F],
      LE: KinesisClient.LogEncoders,
      SLE: ShardMapCache.LogEncoders,
      PLE: Producer.LogEncoders
  ): Resource[F, FS2KinesisProducer[F]] = AwsClients
    .kinesisClientResource[F](config)
    .flatMap(underlying =>
      FS2KinesisProducer[F](
        producerConfig,
        underlying,
        callback
      )
    )

  /** Builds a [[kinesis4cats.client.producer.KinesisProducer KinesisProducer]]
    * that is compliant for Localstack usage. Lifecycle is managed as a
    * [[cats.effect.Resource Resource]].
    *
    * @param streamName
    *   Name of stream for the producer to produce to
    * @param prefix
    *   Optional prefix for parsing configuration. Default to None
    * @param producerConfig
    *   String =>
    *   [[kinesis4cats.producer.fs2.FS2Producer.Config FS2Producer.Config]]
    *   function that creates configuration given a stream name. Defaults to
    *   Producer.Config.default
    * @param callback
    *   Function that can be run after each of the put results from the
    *   underlying. Defaults to F.unit.
    * @param F
    *   F with an [[cats.effect.Async Async]] instance
    * @param LE
    *   [[kinesis4cats.client.KinesisClient.LogEncoders LogEncoders]]
    * @param SLE
    *   [[kinesis4cats.producer.ShardMapCache.LogEncoders ShardMapCache.LogEncoders]]
    * @param PLE
    *   [[kinesis4cats.producer.Producer.LogEncoders Producer.LogEncoders]]
    * @return
    *   [[cats.effect.Resource Resource]] of
    *   [[kinesis4cats.client.producer.fs2.FS2KinesisProducer FS2KinesisProducer]]
    */
  def resource[F[_]](
      streamName: String,
      prefix: Option[String] = None,
      producerConfig: String => FS2Producer.Config = (streamName: String) =>
        FS2Producer.Config.default(StreamNameOrArn.Name(streamName)),
      callback: (Producer.Res[PutRecordsResponse], Async[F]) => F[Unit] =
        (_: Producer.Res[PutRecordsResponse], f: Async[F]) => f.unit
  )(implicit
      F: Async[F],
      LE: KinesisClient.LogEncoders,
      SLE: ShardMapCache.LogEncoders,
      PLE: Producer.LogEncoders
  ): Resource[F, FS2KinesisProducer[F]] = LocalstackConfig
    .resource[F](prefix)
    .flatMap(resource[F](producerConfig(streamName), _, callback))
}
