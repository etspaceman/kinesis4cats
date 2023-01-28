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

package kinesis4cats.kcl
package fs2
package localstack

import scala.concurrent.duration._

import java.util.UUID

import cats.Parallel
import cats.effect.std.Queue
import cats.effect.syntax.all._
import cats.effect.{Async, Resource}
import cats.syntax.all._
import software.amazon.kinesis.common._

import kinesis4cats.kcl.localstack.LocalstackKCLConsumer
import kinesis4cats.localstack.LocalstackConfig

/** Helpers for constructing and leveraging the KCL with Localstack via FS2.
  */
object LocalstackKCLConsumerFS2 {

  /** Creates a
    * [[kinesis4cats.kcl.fs2.KCLConsumerFS2.Config KCLConsumerFS2.Config]] that
    * is compliant with Localstack.
    *
    * @param config
    *   [[kinesis4cats.localstack.LocalstackConfig LocalstackConfig]]
    * @param streamName
    *   Name of stream to consume
    * @param appName
    *   Application name for the consumer. Used for the dynamodb table name as
    *   well as the metrics namespace.
    * @param workerId
    *   Unique identifier for the worker. Typically a UUID.
    * @param position
    *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/common/InitialPositionInStreamExtended.java InitialPositionInStreamExtended]]
    * @param recordProcessorConfig
    *   [[kinesis4cats.kcl.RecordProcessor.Config RecordProcessor.Config]].
    * @param F
    *   [[cats.effect.Async Async]]
    * @param LE
    *   [[kinesis4cats.kcl.RecordProcessor.LogEncoders RecordProcessor.LogEncoders]]
    * @return
    *   [[kinesis4cats.kcl.fs2.KCLConsumerFS2.Config KCLConsumerFS2.Config]]
    */
  def kclConfig[F[_]](
      config: LocalstackConfig,
      streamName: String,
      appName: String,
      workerId: String,
      position: InitialPositionInStreamExtended,
      recordProcessorConfig: RecordProcessor.Config
  )(implicit
      F: Async[F],
      LE: RecordProcessor.LogEncoders
  ): Resource[F, KCLConsumerFS2.Config[F]] = for {
    queue <- Queue.bounded[F, CommittableRecord[F]](100).toResource
    underlying <- LocalstackKCLConsumer.kclConfig(
      config,
      streamName,
      appName,
      workerId,
      position,
      recordProcessorConfig
    )(KCLConsumerFS2.callback(queue))
  } yield KCLConsumerFS2.Config[F](underlying, queue, 5, 1.second, 5, 0.seconds)

  /** Creates a
    * [[kinesis4cats.kcl.fs2.KCLConsumerFS2.Config KCLConsumerFS2.Config]] that
    * is compliant with Localstack.
    *
    * @param streamName
    *   Name of stream to consume
    * @param appName
    *   Application name for the consumer. Used for the dynamodb table name as
    *   well as the metrics namespace.
    * @param prefix
    *   Optional prefix for parsing configuration. Default to None
    * @param workerId
    *   Unique identifier for the worker. Default is a random UUID
    * @param position
    *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/common/InitialPositionInStreamExtended.java InitialPositionInStreamExtended]]
    *   Default is TRIM_HORIZON
    * @param recordProcessorConfig
    *   [[kinesis4cats.kcl.RecordProcessor.Config RecordProcessor.Config]].
    *   Default is `RecordProcessor.Config.default` with autoCommit set to false
    * @param F
    *   [[cats.effect.Async Async]]
    * @param LE
    *   [[kinesis4cats.kcl.RecordProcessor.LogEncoders RecordProcessor.LogEncoders]]
    * @return
    *   [[kinesis4cats.kcl.fs2.KCLConsumerFS2.Config KCLConsumerFS2.Config]]
    */
  def kclConfig[F[_]](
      streamName: String,
      appName: String,
      prefix: Option[String] = None,
      workerId: String = UUID.randomUUID().toString,
      position: InitialPositionInStreamExtended =
        InitialPositionInStreamExtended.newInitialPosition(
          InitialPositionInStream.TRIM_HORIZON
        ),
      recordProcessorConfig: RecordProcessor.Config =
        RecordProcessor.Config.default.copy(autoCommit = false)
  )(implicit
      F: Async[F],
      LE: RecordProcessor.LogEncoders
  ): Resource[F, KCLConsumerFS2.Config[F]] = for {
    config <- LocalstackConfig.resource(prefix)
    result <- kclConfig(
      config,
      streamName,
      appName,
      workerId,
      position,
      recordProcessorConfig
    )
  } yield result

  /** Runs a [[kinesis4cats.kcl.fs2.KCLConsumerFS2 KCLConsumerFS2]] that is
    * compliant with Localstack. Also exposes a
    * [[cats.effect.Deferred Deferred]] that will complete when the consumer has
    * started processing records. Useful for allowing tests time for the
    * consumer to start before processing the stream.
    *
    * @param config
    *   [[kinesis4cats.localstack.LocalstackConfig LocalstackConfig]]
    * @param streamName
    *   Name of stream to consume
    * @param appName
    *   Application name for the consumer. Used for the dynamodb table name as
    *   well as the metrics namespace.
    * @param workerId
    *   Unique identifier for the worker. Typically a UUID.
    * @param position
    *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/common/InitialPositionInStreamExtended.java InitialPositionInStreamExtended]]
    * @param recordProcessorConfig
    *   [[kinesis4cats.kcl.RecordProcessor.Config RecordProcessor.Config]].
    * @param F
    *   [[cats.effect.Async Async]]
    * @param LE
    *   [[kinesis4cats.kcl.RecordProcessor.LogEncoders RecordProcessor.LogEncoders]]
    * @return
    *   [[kinesis4cats.kcl.fs2.KCLConsumerFS2]] in a
    *   [[cats.effect.Resource Resource]]
    */
  def kclConsumer[F[_]](
      config: LocalstackConfig,
      streamName: String,
      appName: String,
      workerId: String,
      position: InitialPositionInStreamExtended,
      recordProcessorConfig: RecordProcessor.Config
  )(implicit
      F: Async[F],
      P: Parallel[F],
      LE: RecordProcessor.LogEncoders
  ): Resource[F, KCLConsumerFS2[F]] =
    kclConfig(
      config,
      streamName,
      appName,
      workerId,
      position,
      recordProcessorConfig
    ).map(
      new KCLConsumerFS2[F](_)
    )

  /** Runs a [[kinesis4cats.kcl.fs2.KCLConsumerFS2 KCLConsumerFS2]] that is
    * compliant with Localstack. Also exposes a
    * [[cats.effect.Deferred Deferred]] that will complete when the consumer has
    * started processing records. Useful for allowing tests time for the
    * consumer to start before processing the stream.
    *
    * @param streamName
    *   Name of stream to consume
    * @param appName
    *   Application name for the consumer. Used for the dynamodb table name as
    *   well as the metrics namespace.
    * @param prefix
    *   Optional prefix for parsing configuration. Default to None
    * @param workerId
    *   Unique identifier for the worker. Default to a random UUID.
    * @param position
    *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/common/InitialPositionInStreamExtended.java InitialPositionInStreamExtended]].
    *   Default to TRIM_HORIZON
    * @param recordProcessorConfig
    *   [[kinesis4cats.kcl.RecordProcessor.Config RecordProcessor.Config]].
    *   Default is `RecordProcessor.Config.default` with autoCommit set to false
    * @param F
    *   [[cats.effect.Async Async]]
    * @param LE
    *   [[kinesis4cats.kcl.RecordProcessor.LogEncoders RecordProcessor.LogEncoders]]
    * @return
    *   [[kinesis4cats.kcl.fs2.KCLConsumerFS2 KCLConsumerFS2]] in a
    *   [[cats.effect.Resource Resource]]
    */
  def kclConsumer[F[_]](
      streamName: String,
      appName: String,
      prefix: Option[String] = None,
      workerId: String = UUID.randomUUID().toString(),
      position: InitialPositionInStreamExtended =
        InitialPositionInStreamExtended.newInitialPosition(
          InitialPositionInStream.TRIM_HORIZON
        ),
      recordProcessorConfig: RecordProcessor.Config =
        RecordProcessor.Config.default.copy(autoCommit = false)
  )(implicit
      F: Async[F],
      P: Parallel[F],
      LE: RecordProcessor.LogEncoders
  ): Resource[F, KCLConsumerFS2[F]] = for {
    config <- LocalstackConfig.resource(prefix)
    result <- kclConsumer(
      config,
      streamName,
      appName,
      workerId,
      position,
      recordProcessorConfig
    )
  } yield result
}
