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

package kinesis4cats.kcl.fs2

import scala.concurrent.duration._

import cats.Applicative
import cats.Parallel
import cats.effect.Deferred
import cats.effect.kernel.Sync
import cats.effect.std.Queue
import cats.effect.syntax.all._
import cats.effect.{Async, Ref, Resource}
import cats.syntax.all._
import fs2.concurrent.SignallingRef
import fs2.{Pipe, Stream}
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient
import software.amazon.kinesis.coordinator.WorkerStateChangeListener.WorkerState
import software.amazon.kinesis.processor.StreamTracker

import kinesis4cats.Utils
import kinesis4cats.compat.retry.RetryPolicies._
import kinesis4cats.compat.retry._
import kinesis4cats.kcl.WorkerListeners._
import kinesis4cats.kcl.{CommittableRecord, KCLConsumer, RecordProcessor}

/** Wrapper offering for the
  * [[https://docs.aws.amazon.com/streams/latest/dev/shared-throughput-kcl-consumers.html KCL]]
  * via an [[fs2.Stream fs2.Stream]]
  *
  * @param config
  *   [[kinesis4cats.kcl.fs2.KCLConsumerFS2.Config Config]]
  * @param F
  *   [[cats.effect.Async Async]]
  */
class KCLConsumerFS2[F[_]] private[kinesis4cats] (
    config: KCLConsumerFS2.Config[F]
)(implicit F: Async[F], P: Parallel[F]) {

  /** Runs a [[https://github.com/awslabs/amazon-kinesis-client KCL Consumer]]
    * as an [[fs2.Stream fs2.Stream]]
    *
    * @return
    *   [[fs2.Stream fs2.Stream]] that manages the lifecycle of the
    *   [[https://github.com/awslabs/amazon-kinesis-client KCL Consumer]]
    */
  def stream(): Resource[F, Stream[F, CommittableRecord[F]]] =
    KCLConsumerFS2.stream(config)

  /** Runs a [[https://github.com/awslabs/amazon-kinesis-client KCL Consumer]]
    * as an [[fs2.Stream fs2.Stream]]. This exposes the
    * [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/coordinator/WorkerStateChangeListener.java WorkerState]]
    * which can be used as a means to determine if the KCL Consumer is running.
    * A common use case for this is to service healthcheck endpoints when
    * running these in an orchestration service (e.g. Kubernetes or AWS ECS) or
    * running tests that require the consumer to be up before an assertion is
    * checked.
    *
    * @return
    *   [[cats.effect.Resource Resource]] containing an
    *   [[fs2.Stream fs2.Stream]] and [[cats.effect.Deferred Deferred]]
    */
  def streamWithRefListener(): Resource[F, KCLConsumerFS2.StreamAndRef[F]] =
    for {
      listener <- RefListener[F]
      state = listener.state
      stream <- KCLConsumerFS2.stream(
        config.copy(underlying =
          config.underlying.copy(
            coordinatorConfig = config.underlying.coordinatorConfig
              .workerStateChangeListener(listener)
          )
        )
      )
    } yield KCLConsumerFS2.StreamAndRef(stream, state)

  /** Runs a [[https://github.com/awslabs/amazon-kinesis-client KCL Consumer]]
    * in the background as a [[cats.effect.Resource Resource]]. This exposes the
    * [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/coordinator/WorkerStateChangeListener.java WorkerState]]
    * which can be used as a means to determine if the KCL Consumer is running.
    * A common use case for this is to service healthcheck endpoints when
    * running these in an orchestration service (e.g. Kubernetes or AWS ECS) or
    * running tests that require the consumer to be up before an assertion is
    * checked.
    *
    * Unlike `streamWithRefListener`, this method uses a
    * [[cats.effect.Deferred Deferred]] instance. This is useful when you only
    * need to react when the KCL has reached a specific state 1 time (e.g. wait
    * to produce to a stream until the consumer is started)
    *
    * @param stateToCompleteOn
    *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/coordinator/WorkerStateChangeListener.java WorkerState]]
    *   to expect when completing the [[cats.effect.Deferred Deferred]]. Default
    *   is STARTED.
    * @return
    *   [[cats.effect.Resource Resource]] containing an
    *   [[fs2.Stream fs2.Stream]] and [[cats.effect.Deferred Deferred]]
    */
  def streamWithDeferredListener(
      stateToCompleteOn: WorkerState = WorkerState.STARTED
  ): Resource[F, KCLConsumerFS2.StreamAndDeferred[F]] = for {
    listener <- DeferredListener[F](stateToCompleteOn)
    deferred = listener.deferred
    stream <- KCLConsumerFS2.stream(
      config.copy(underlying =
        config.underlying.copy(
          coordinatorConfig = config.underlying.coordinatorConfig
            .workerStateChangeListener(listener)
        )
      )
    )
  } yield KCLConsumerFS2.StreamAndDeferred(stream, deferred)

  /** A [[fs2.Pipe Pipe]] definition that users can leverage to commit the
    * records after they have completed processing.
    *
    * @return
    *   [[fs2.Pipe Pipe]] for committing records.
    */
  val commitRecords: Pipe[F, CommittableRecord[F], CommittableRecord[F]] =
    KCLConsumerFS2.commitRecords(config)
}

object KCLConsumerFS2 {

  private[kinesis4cats] val defaultProcessConfig: KCLConsumer.ProcessConfig =
    KCLConsumer.ProcessConfig.default.copy(recordProcessorConfig =
      RecordProcessor.Config.default.copy(autoCommit = false)
    )

  /** Helper class that holds both an [[fs2.Stream fs2.Stream]] and a
    * [[cats.effect.Deferred Deferred]]
    *
    * @param stream
    *   [[fs2.Stream fs2.Stream]] of [[kinesis4cats.kcl.CommittableRecord]]
    *   values to process
    * @param deferred
    *   [[cats.effect.Deferred Deferred]] which will complete when a defined
    *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/coordinator/WorkerStateChangeListener.java WorkerState]]
    *   is recognized.
    */
  final case class StreamAndDeferred[F[_]](
      stream: Stream[F, CommittableRecord[F]],
      deferred: Deferred[F, Unit]
  )

  /** Helper class that holds both an [[fs2.Stream fs2.Stream]] and a
    * [[cats.effect.Ref Ref]] of the
    * [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/coordinator/WorkerStateChangeListener.java WorkerState]]
    *
    * @param stream
    *   [[fs2.Stream fs2.Stream]] of [[kinesis4cats.kcl.CommittableRecord]]
    *   values to process
    * @param ref
    *   [[cats.effect.Ref Ref]] that contains the current
    *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/coordinator/WorkerStateChangeListener.java WorkerState]]
    */
  final case class StreamAndRef[F[_]](
      stream: Stream[F, CommittableRecord[F]],
      ref: Ref[F, WorkerState]
  )

  /** [[kinesis4cats.kcl.RecordProcessor RecordProcessor]] callback function
    * responsible for enqueueing events.
    *
    * @param queue
    *   [[cats.effect.std.Queue Queue]] for
    *   [[kinesis4cats.kcl.CommittableRecord]] values
    * @param F
    *   [[cats.effect.Async Async]]
    * @return
    */
  private[kinesis4cats] def callback[F[_]: Applicative](
      queue: Queue[F, CommittableRecord[F]]
  ): List[CommittableRecord[F]] => F[Unit] =
    (records: List[CommittableRecord[F]]) => records.traverse_(queue.offer)

  final case class Builder[F[_]] private (
      config: KCLConsumer.BuilderConfig.Make[F],
      mkKinesisClient: Resource[F, KinesisAsyncClient],
      mkDynamoClient: Resource[F, DynamoDbAsyncClient],
      mkCloudWatchClient: Resource[F, CloudWatchAsyncClient],
      mkWorkerId: F[String],
      fs2Config: FS2Config
  )(implicit F: Async[F], P: Parallel[F]) {
    def configure(
        f: KCLConsumer.BuilderConfig[F] => KCLConsumer.BuilderConfig[F]
    ): Builder[F] =
      copy(config = config.andThen(f))

    def configureFs2Config(f: FS2Config => FS2Config): Builder[F] =
      copy(fs2Config = f(fs2Config))
    def withFs2Config(fs2Config: FS2Config): Builder[F] =
      copy(fs2Config = fs2Config)

    def withKinesisClient(client: KinesisAsyncClient): Builder[F] =
      copy(mkKinesisClient = Resource.pure(client))

    def withDynamoClient(client: DynamoDbAsyncClient): Builder[F] =
      copy(mkDynamoClient = Resource.pure(client))

    def withCloudWatchClient(client: CloudWatchAsyncClient): Builder[F] =
      copy(mkCloudWatchClient = Resource.pure(client))

    def withWorkerId(workerId: String): Builder[F] =
      copy(mkWorkerId = F.pure(workerId))

    def build: Resource[F, KCLConsumerFS2[F]] = for {
      queue <- Queue
        .bounded[F, CommittableRecord[F]](fs2Config.queueSize)
        .toResource
      underlying <-
        (
          mkKinesisClient,
          mkDynamoClient,
          mkCloudWatchClient,
          mkWorkerId.toResource
        ).mapN(config.make)
          .map(_.withCallback(callback(queue)))
          .flatMap(_.build)
    } yield new KCLConsumerFS2[F](Config(underlying, queue, fs2Config))
  }

  object Builder {
    def default[F[_]](
        streamTracker: StreamTracker,
        appName: String
    )(implicit
        F: Async[F],
        P: Parallel[F]
    ): Builder[F] = default(streamTracker, appName, appName)

    def default[F[_]](
        streamTracker: StreamTracker,
        dynamoTableName: String,
        appName: String
    )(implicit
        F: Async[F],
        P: Parallel[F]
    ): Builder[F] = Builder(
      config = KCLConsumer.BuilderConfig.Make
        .default(dynamoTableName, appName, streamTracker)
        .andThen(_.withProcessConfig(defaultProcessConfig)),
      mkKinesisClient = Resource.fromAutoCloseable(
        Sync[F].delay(KinesisAsyncClient.create())
      ),
      mkDynamoClient = Resource.fromAutoCloseable(
        Sync[F].delay(DynamoDbAsyncClient.create())
      ),
      mkCloudWatchClient = Resource.fromAutoCloseable(
        Sync[F].delay(CloudWatchAsyncClient.create())
      ),
      mkWorkerId = Utils.randomUUIDStringSafe[F],
      FS2Config.default
    )

    @annotation.unused
    private def unapply[F[_]](builder: Builder[F]): Unit = ()
  }

  /** Configuration for the
    * [[kinesis4cats.kcl.fs2.KCLConsumerFS2 KCLConsumerFS2]]
    *
    * @param underlying
    *   [[kinesis4cats.kcl.KCLConsumer.Config KCLConsumer.Config]]
    * @param queue
    *   [[cats.effect.std.Queue Queue]] of
    *   [[kinesis4cats.kcl.CommittableRecord CommittableRecord]]
    * @param maxCommitChunk
    *   Max records to be received in the commitRecords [[fs2.Pipe Pipe]] before
    *   a commit is run.
    * @param maxCommitWait
    *   Max duration to wait in commitRecords [[fs2.Pipe Pipe]] before a commit
    *   is run.
    * @param maxCommitRetries
    *   Max number of retries for a commit operation
    * @param maxCommitRetryDuration
    *   Delay between retries of commits
    */
  final case class Config[F[_]] private[kinesis4cats] (
      underlying: kinesis4cats.kcl.KCLConsumer.Config[F],
      queue: Queue[F, CommittableRecord[F]],
      fs2Config: FS2Config
  )

  /** Configuration for the FS2 implementation
    *
    * @param queueSize
    *   Size of the underlying queue for the FS2 stream. If the queue fills up,
    *   backpressure on the processors will occur. Default 100
    * @param commitMaxChunk
    *   Max records to be received in the commitRecords [[fs2.Pipe Pipe]] before
    *   a commit is run. Default is 1000
    * @param commitMaxWait
    *   Max duration to wait in commitRecords [[fs2.Pipe Pipe]] before a commit
    *   is run. Default is 10 seconds
    * @param commitMaxRetries
    *   Max number of retries for a commit operation
    * @param commitRetryInterval
    *   Interval to wait between commit retries
    */
  final case class FS2Config(
      queueSize: Int,
      commitMaxChunk: Int,
      commitMaxWait: FiniteDuration,
      commitMaxRetries: Int,
      commitRetryInterval: FiniteDuration
  )

  object FS2Config {
    val default = FS2Config(
      1000,
      100,
      10.seconds,
      5,
      0.seconds
    )
  }

  /** Runs a [[https://github.com/awslabs/amazon-kinesis-client KCL Consumer]]
    * as an [[fs2.Stream fs2.Stream]]
    *
    * @param config
    *   [[kinesis4cats.kcl.fs2.KCLConsumerFS2.Config Config]]
    * @param F
    *   [[cats.effect.Async Async]]
    * @return
    *   [[fs2.Stream fs2.Stream]] that manages the lifecycle of the
    *   [[https://github.com/awslabs/amazon-kinesis-client KCL Consumer]]
    */
  private[kinesis4cats] def stream[F[_]](
      config: Config[F]
  )(implicit F: Async[F]): Resource[F, Stream[F, CommittableRecord[F]]] = for {
    interruptSignal <- SignallingRef[F, Boolean](false).toResource
    _ <- kinesis4cats.kcl.KCLConsumer
      .run(config.underlying)
      .onFinalize(interruptSignal.set(true))
    stream = Stream
      .fromQueueUnterminated(config.queue)
      .interruptWhen(interruptSignal)
  } yield stream

  /** A [[fs2.Pipe Pipe]] definition that users can leverage to commit the
    * records after they have completed processing.
    *
    * @param config
    *   [[kinesis4cats.kcl.fs2.KCLConsumerFS2.Config Config]]
    * @param F
    *   [[cats.effect.Async Async]]
    * @return
    *   [[fs2.Pipe Pipe]] for committing records.
    */
  private[kinesis4cats] def commitRecords[F[_]](
      config: Config[F]
  )(implicit
      F: Async[F],
      P: Parallel[F]
  ): Pipe[F, CommittableRecord[F], CommittableRecord[F]] =
    _.groupWithin(
      config.fs2Config.commitMaxChunk,
      config.fs2Config.commitMaxWait
    )
      .evalTap(chunk =>
        chunk.toList.groupBy(_.shardId).toList.parTraverse_ {
          case (_, records) =>
            val max = records.max
            max.canCheckpoint.ifM(
              retryingOnAllErrors(
                limitRetries(config.fs2Config.commitMaxRetries)
                  .join(constantDelay(config.fs2Config.commitRetryInterval)),
                noop[F, Throwable]
              )(max.checkpoint),
              F.unit
            )
        }
      )
      .unchunks
}
