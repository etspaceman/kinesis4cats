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

import cats.Parallel
import cats.effect.std.Queue
import cats.effect.syntax.all._
import cats.effect.{Async, Resource}
import software.amazon.kinesis.common._
import software.amazon.kinesis.processor.StreamTracker

import kinesis4cats.Utils
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
    * @param streamTracker
    *   Name of stream to consume
    * @param appName
    *   Application name for the consumer. Used for the dynamodb table name as
    *   well as the metrics namespace.
    * @param workerId
    *   Unique identifier for the worker. Typically a UUID.
    * @param position
    *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/common/InitialPositionInStreamExtended.java InitialPositionInStreamExtended]]
    * @param processConfig
    *   [[kinesis4cats.kcl.KCLConsumer.ProcessConfig KCLConsumer.ProcessConfig]]
    * @param F
    *   [[cats.effect.Async Async]]
    * @param LE
    *   [[kinesis4cats.kcl.RecordProcessor.LogEncoders RecordProcessor.LogEncoders]]
    * @return
    *   [[kinesis4cats.kcl.fs2.KCLConsumerFS2.Config KCLConsumerFS2.Config]]
    */
  def kclConfig[F[_]](
      config: LocalstackConfig,
      streamTracker: StreamTracker,
      appName: String,
      workerId: String,
      position: InitialPositionInStreamExtended,
      processConfig: KCLConsumer.ProcessConfig
  )(implicit
      F: Async[F],
      LE: RecordProcessor.LogEncoders
  ): Resource[F, KCLConsumerFS2.Config[F]] = for {
    queue <- Queue.bounded[F, CommittableRecord[F]](100).toResource
    underlying <- LocalstackKCLConsumer.kclConfig(
      config,
      streamTracker,
      appName,
      workerId,
      position,
      processConfig
    )(KCLConsumerFS2.callback(queue))
  } yield KCLConsumerFS2
    .Config[F](underlying, queue, KCLConsumerFS2.FS2Config.default)

  /** Creates a
    * [[kinesis4cats.kcl.fs2.KCLConsumerFS2.Config KCLConsumerFS2.Config]] that
    * is compliant with Localstack.
    *
    * @param streamTracker
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
    * @param processConfig
    *   [[kinesis4cats.kcl.KCLConsumer.ProcessConfig KCLConsumer.ProcessConfig]]
    *   Default is `ProcessConfig.default` with autoCommit set to false
    * @param F
    *   [[cats.effect.Async Async]]
    * @param LE
    *   [[kinesis4cats.kcl.RecordProcessor.LogEncoders RecordProcessor.LogEncoders]]
    * @return
    *   [[kinesis4cats.kcl.fs2.KCLConsumerFS2.Config KCLConsumerFS2.Config]]
    */
  def kclConfig[F[_]](
      streamTracker: StreamTracker,
      appName: String,
      prefix: Option[String] = None,
      workerId: String = Utils.randomUUIDString,
      position: InitialPositionInStreamExtended =
        InitialPositionInStreamExtended.newInitialPosition(
          InitialPositionInStream.TRIM_HORIZON
        ),
      processConfig: KCLConsumer.ProcessConfig =
        KCLConsumerFS2.defaultProcessConfig
  )(implicit
      F: Async[F],
      LE: RecordProcessor.LogEncoders
  ): Resource[F, KCLConsumerFS2.Config[F]] = for {
    config <- LocalstackConfig.resource(prefix)
    result <- kclConfig(
      config,
      streamTracker,
      appName,
      workerId,
      position,
      processConfig
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
    * @param streamTracker
    *   Name of stream to consume
    * @param appName
    *   Application name for the consumer. Used for the dynamodb table name as
    *   well as the metrics namespace.
    * @param workerId
    *   Unique identifier for the worker. Typically a UUID.
    * @param position
    *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/common/InitialPositionInStreamExtended.java InitialPositionInStreamExtended]]
    * @param processConfig
    *   [[kinesis4cats.kcl.KCLConsumer.ProcessConfig KCLConsumer.ProcessConfig]]
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
      streamTracker: StreamTracker,
      appName: String,
      workerId: String,
      position: InitialPositionInStreamExtended,
      processConfig: KCLConsumer.ProcessConfig
  )(implicit
      F: Async[F],
      P: Parallel[F],
      LE: RecordProcessor.LogEncoders
  ): Resource[F, KCLConsumerFS2[F]] =
    kclConfig(
      config,
      streamTracker,
      appName,
      workerId,
      position,
      processConfig
    ).map(
      new KCLConsumerFS2[F](_)
    )

  /** Runs a [[kinesis4cats.kcl.fs2.KCLConsumerFS2 KCLConsumerFS2]] that is
    * compliant with Localstack. Also exposes a
    * [[cats.effect.Deferred Deferred]] that will complete when the consumer has
    * started processing records. Useful for allowing tests time for the
    * consumer to start before processing the stream.
    *
    * @param streamTracker
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
    * @param processConfig
    *   [[kinesis4cats.kcl.KCLConsumer.ProcessConfig KCLConsumer.ProcessConfig]]
    *   Default is `ProcessConfig.default` with autoCommit set to false
    * @param F
    *   [[cats.effect.Async Async]]
    * @param LE
    *   [[kinesis4cats.kcl.RecordProcessor.LogEncoders RecordProcessor.LogEncoders]]
    * @return
    *   [[kinesis4cats.kcl.fs2.KCLConsumerFS2 KCLConsumerFS2]] in a
    *   [[cats.effect.Resource Resource]]
    */
  def kclConsumer[F[_]](
      streamTracker: StreamTracker,
      appName: String,
      prefix: Option[String] = None,
      workerId: String = Utils.randomUUIDString,
      position: InitialPositionInStreamExtended =
        InitialPositionInStreamExtended.newInitialPosition(
          InitialPositionInStream.TRIM_HORIZON
        ),
      processConfig: KCLConsumer.ProcessConfig =
        KCLConsumerFS2.defaultProcessConfig
  )(implicit
      F: Async[F],
      P: Parallel[F],
      LE: RecordProcessor.LogEncoders
  ): Resource[F, KCLConsumerFS2[F]] = for {
    config <- LocalstackConfig.resource(prefix)
    result <- kclConsumer(
      config,
      streamTracker,
      appName,
      workerId,
      position,
      processConfig
    )
  } yield result
}
