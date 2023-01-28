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

import java.util.UUID

import cats.Parallel
import cats.effect.kernel.Deferred
import cats.effect.std.Queue
import cats.effect.syntax.all._
import cats.effect.{Async, Ref, Resource}
import cats.syntax.all._
import fs2.concurrent.SignallingRef
import fs2.{Pipe, Stream}
import retry.RetryPolicies._
import retry._
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient
import software.amazon.kinesis.checkpoint.CheckpointConfig
import software.amazon.kinesis.coordinator.WorkerStateChangeListener.WorkerState
import software.amazon.kinesis.coordinator._
import software.amazon.kinesis.leases.LeaseManagementConfig
import software.amazon.kinesis.lifecycle.LifecycleConfig
import software.amazon.kinesis.metrics.MetricsConfig
import software.amazon.kinesis.retrieval.RetrievalConfig

import kinesis4cats.kcl.WorkerListeners._
import kinesis4cats.kcl.{CommittableRecord, RecordProcessor}

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
  private[kinesis4cats] def callback[F[_]](
      queue: Queue[F, CommittableRecord[F]]
  )(implicit
      F: Async[F]
  ): List[CommittableRecord[F]] => F[Unit] =
    (records: List[CommittableRecord[F]]) => records.traverse_(queue.offer)

  /** Low-level constructor for
    * [[kinesis4cats.kcl.fs2.KCLConsumerFS2 KCLConsumerFS2]].
    *
    * @param checkpointConfig
    *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/checkpoint/CheckpointConfig.java CheckpointConfig]]
    * @param coordinatorConfig
    *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/coordinator/CoordinatorConfig.java CoordinatorConfig]]
    * @param leaseManagementConfig
    *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/leases/LeaseManagementConfig.java LeaseManagementConfig]]
    * @param lifecycleConfig
    *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/lifecycle/LifecycleConfig.java LifecycleConfig]]
    * @param metricsConfig
    *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/metrics/MetricsConfig.java MetricsConfig]]
    * @param retrievalConfig
    *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/retrieval/RetrievalConfig.java RetrievalConfig]]
    * @param queueSize
    *   Size of the underlying queue for the FS2 stream. If the queue fills up,
    *   backpressure on the processors will occur. Default 100
    * @param commitMaxChunk
    *   Max records to be received in the commitRecords [[fs2.Pipe Pipe]] before
    *   a commit is run. Default is 1000
    * @param commitMaxWait
    *   Max duration to wait in commitRecords [[fs2.Pipe Pipe]] before a commit
    *   is run. Default is 10 seconds
    * @param raiseOnError
    *   Whether the [[kinesis4cats.kcl.RecordProcessor RecordProcessor]] should
    *   raise exceptions or simply log them. It is recommended to set this to
    *   true. See this
    *   [[https://github.com/awslabs/amazon-kinesis-client/issues/10 issue]] for
    *   more information.
    * @param recordProcessorConfig
    *   [[kinesis4cats.kcl.RecordProcessor.Config RecordProcessor.Config]]
    * @param callProcessRecordsEvenForEmptyRecordList
    *   Determines if processRecords() should run on the record processor for
    *   empty record lists.
    * @param F
    *   [[cats.effect.Async Async]] instance
    * @param encoders
    *   [[kinesis4cats.kcl.RecordProcessor.LogEncoders RecordProcessor.LogEncoders]]
    *   for encoding structured logs
    * @return
    *   [[cats.effect.Resource Resource]] containing the
    *   [[kinesis4cats.kcl.fs2.KCLConsumerFS2 KCLConsumerFS2]]
    */
  def apply[F[_]](
      checkpointConfig: CheckpointConfig,
      coordinatorConfig: CoordinatorConfig,
      leaseManagementConfig: LeaseManagementConfig,
      lifecycleConfig: LifecycleConfig,
      metricsConfig: MetricsConfig,
      retrievalConfig: RetrievalConfig,
      queueSize: Int = 100,
      commitMaxChunk: Int = 1000,
      commitMaxWait: FiniteDuration = 10.seconds,
      commitMaxRetries: Int = 5,
      commitRetryDuration: FiniteDuration = 0.seconds,
      raiseOnError: Boolean = true,
      recordProcessorConfig: RecordProcessor.Config =
        RecordProcessor.Config.default.copy(autoCommit = false),
      callProcessRecordsEvenForEmptyRecordList: Boolean = false
  )(implicit
      F: Async[F],
      P: Parallel[F],
      encoders: RecordProcessor.LogEncoders
  ): Resource[F, KCLConsumerFS2[F]] = Config
    .create(
      checkpointConfig,
      coordinatorConfig,
      leaseManagementConfig,
      lifecycleConfig,
      metricsConfig,
      retrievalConfig,
      queueSize,
      commitMaxChunk,
      commitMaxWait,
      commitMaxRetries,
      commitRetryDuration,
      raiseOnError,
      recordProcessorConfig,
      callProcessRecordsEvenForEmptyRecordList
    )
    .map(new KCLConsumerFS2[F](_))

  /** Constructor for the [[kinesis4cats.kcl.fs2.KCLConsumerFS2 KCLConsumerFS2]]
    * that leverages the
    * [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/common/ConfigsBuilder.java ConfigsBuilder]]
    * from the KCL. This is a simpler entry-point for creating the
    * configuration, and provides a transform function to add any custom
    * configuration that was not covered by the default
    *
    * @param kinesisClient
    *   [[https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/kinesis/KinesisAsyncClient.html KinesisAsyncClient]]
    * @param dynamoClient
    *   [[https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/dynamodb/DynamoDbAsyncClient.html DynamoDbAsyncClient]]
    * @param cloudWatchClient
    *   [[https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/cloudwatch/CloudWatchClient.html CloudWatchClient]]
    * @param streamName
    *   Name of the Kinesis stream to consume from
    * @param appName
    *   Name of the application. Usually also the dynamo table name for
    *   checkpoints
    * @param queueSize
    *   Size of the underlying queue for the FS2 stream. If the queue fills up,
    *   backpressure on the processors will occur. Default 100
    * @param commitMaxChunk
    *   Max records to be received in the commitRecords [[fs2.Pipe Pipe]] before
    *   a commit is run. Default is 1000
    * @param commitMaxWait
    *   Max duration to wait in commitRecords [[fs2.Pipe Pipe]] before a commit
    *   is run. Default is 10 seconds
    * @param raiseOnError
    *   Whether the [[kinesis4cats.kcl.RecordProcessor RecordProcessor]] should
    *   raise exceptions or simply log them. It is recommended to set this to
    *   true. See this
    *   [[https://github.com/awslabs/amazon-kinesis-client/issues/10 issue]] for
    *   more information.
    * @param workerId
    *   Unique identifier for a single instance of this consumer. Default is a
    *   random UUID.
    * @param recordProcessorConfig
    *   [[kinesis4cats.kcl.RecordProcessor.Config RecordProcessor.Config]]
    * @param tfn
    *   Function to update the
    *   [[kinesis4cats.kcl.KCLConsumer.Config KCLConsumer.Config]]. Useful for
    *   overriding defaults.
    * @param F
    *   [[cats.effect.Async Async]] instance
    * @param encoders
    *   [[kinesis4cats.kcl.RecordProcessor.LogEncoders RecordProcessor.LogEncoders]]
    *   for encoding structured logs
    * @return
    *   [[cats.effect.Resource Resource]] containing the
    *   [[kinesis4cats.kcl.fs2.KCLConsumerFS2.Config KCLConsumerFS2.Config]]
    */
  def configsBuilder[F[_]](
      kinesisClient: KinesisAsyncClient,
      dynamoClient: DynamoDbAsyncClient,
      cloudWatchClient: CloudWatchAsyncClient,
      streamName: String,
      appName: String,
      queueSize: Int = 100,
      commitMaxChunk: Int = 1000,
      commitMaxWait: FiniteDuration = 10.seconds,
      commitMaxRetries: Int = 5,
      commitRetryDuration: FiniteDuration = 0.seconds,
      raiseOnError: Boolean = true,
      workerId: String = UUID.randomUUID.toString,
      recordProcessorConfig: RecordProcessor.Config =
        RecordProcessor.Config.default.copy(autoCommit = false)
  )(
      tfn: kinesis4cats.kcl.KCLConsumer.Config[
        F
      ] => kinesis4cats.kcl.KCLConsumer.Config[F] =
        (x: kinesis4cats.kcl.KCLConsumer.Config[F]) => x
  )(implicit
      F: Async[F],
      P: Parallel[F],
      encoders: RecordProcessor.LogEncoders
  ): Resource[F, KCLConsumerFS2[F]] = Config
    .configsBuilder(
      kinesisClient,
      dynamoClient,
      cloudWatchClient,
      streamName,
      appName,
      queueSize,
      commitMaxChunk,
      commitMaxWait,
      commitMaxRetries,
      commitRetryDuration,
      raiseOnError,
      workerId,
      recordProcessorConfig
    )(tfn)
    .map(new KCLConsumerFS2[F](_))

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
  final case class Config[F[_]](
      underlying: kinesis4cats.kcl.KCLConsumer.Config[F],
      queue: Queue[F, CommittableRecord[F]],
      maxCommitChunk: Int,
      maxCommitWait: FiniteDuration,
      maxCommitRetries: Int,
      maxCommitRetryDuration: FiniteDuration
  )

  object Config {

    /** Low-level constructor for
      * [[kinesis4cats.kcl.fs2.KCLConsumerFS2.Config KCLConsumerFS2.Config]].
      *
      * @param checkpointConfig
      *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/checkpoint/CheckpointConfig.java CheckpointConfig]]
      * @param coordinatorConfig
      *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/coordinator/CoordinatorConfig.java CoordinatorConfig]]
      * @param leaseManagementConfig
      *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/leases/LeaseManagementConfig.java LeaseManagementConfig]]
      * @param lifecycleConfig
      *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/lifecycle/LifecycleConfig.java LifecycleConfig]]
      * @param metricsConfig
      *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/metrics/MetricsConfig.java MetricsConfig]]
      * @param retrievalConfig
      *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/retrieval/RetrievalConfig.java RetrievalConfig]]
      * @param queueSize
      *   Size of the underlying queue for the FS2 stream. If the queue fills
      *   up, backpressure on the processors will occur. Default 100
      * @param commitMaxChunk
      *   Max records to be received in the commitRecords [[fs2.Pipe Pipe]]
      *   before a commit is run. Default is 1000
      * @param commitMaxWait
      *   Max duration to wait in commitRecords [[fs2.Pipe Pipe]] before a
      *   commit is run. Default is 10 seconds
      * @param commitMaxRetries
      *   Max number of retries for a commit operation
      * @param commitMaxRetryDuration
      *   Delay between retries of commits
      * @param raiseOnError
      *   Whether the [[kinesis4cats.kcl.RecordProcessor RecordProcessor]]
      *   should raise exceptions or simply log them. It is recommended to set
      *   this to true. See this
      *   [[https://github.com/awslabs/amazon-kinesis-client/issues/10 issue]]
      *   for more information.
      * @param recordProcessorConfig
      *   [[kinesis4cats.kcl.RecordProcessor.Config RecordProcessor.Config]]
      * @param callProcessRecordsEvenForEmptyRecordList
      *   Determines if processRecords() should run on the record processor for
      *   empty record lists.
      * @param F
      *   [[cats.effect.Async Async]] instance
      * @param encoders
      *   [[kinesis4cats.kcl.RecordProcessor.LogEncoders RecordProcessor.LogEncoders]]
      *   for encoding structured logs
      * @return
      *   [[cats.effect.Resource Resource]] containing the
      *   [[kinesis4cats.kcl.fs2.KCLConsumerFS2.Config KCLConsumerFS2.Config]]
      */
    def create[F[_]](
        checkpointConfig: CheckpointConfig,
        coordinatorConfig: CoordinatorConfig,
        leaseManagementConfig: LeaseManagementConfig,
        lifecycleConfig: LifecycleConfig,
        metricsConfig: MetricsConfig,
        retrievalConfig: RetrievalConfig,
        queueSize: Int = 100,
        commitMaxChunk: Int = 1000,
        commitMaxWait: FiniteDuration = 10.seconds,
        commitMaxRetries: Int = 5,
        commitRetryDuration: FiniteDuration = 0.seconds,
        raiseOnError: Boolean = true,
        recordProcessorConfig: RecordProcessor.Config =
          RecordProcessor.Config.default.copy(autoCommit = false),
        callProcessRecordsEvenForEmptyRecordList: Boolean = false
    )(implicit
        F: Async[F],
        encoders: RecordProcessor.LogEncoders
    ): Resource[F, Config[F]] = for {
      queue <- Queue.bounded[F, CommittableRecord[F]](queueSize).toResource
      underlying <- kinesis4cats.kcl.KCLConsumer.Config
        .create(
          checkpointConfig,
          coordinatorConfig,
          leaseManagementConfig,
          lifecycleConfig,
          metricsConfig,
          retrievalConfig,
          raiseOnError,
          recordProcessorConfig,
          callProcessRecordsEvenForEmptyRecordList
        )(callback(queue))
    } yield Config(
      underlying,
      queue,
      commitMaxChunk,
      commitMaxWait,
      commitMaxRetries,
      commitRetryDuration
    )

    /** Constructor for the
      * [[kinesis4cats.kcl.fs2.KCLConsumerFS2.Config KCLConsumerFS2.Config]]
      * that leverages the
      * [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/common/ConfigsBuilder.java ConfigsBuilder]]
      * from the KCL. This is a simpler entry-point for creating the
      * configuration, and provides a transform function to add any custom
      * configuration that was not covered by the default
      *
      * @param kinesisClient
      *   [[https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/kinesis/KinesisAsyncClient.html KinesisAsyncClient]]
      * @param dynamoClient
      *   [[https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/dynamodb/DynamoDbAsyncClient.html DynamoDbAsyncClient]]
      * @param cloudWatchClient
      *   [[https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/cloudwatch/CloudWatchClient.html CloudWatchClient]]
      * @param streamName
      *   Name of the Kinesis stream to consume from
      * @param appName
      *   Name of the application. Usually also the dynamo table name for
      *   checkpoints
      * @param queueSize
      *   Size of the underlying queue for the FS2 stream. If the queue fills
      *   up, backpressure on the processors will occur. Default 100
      * @param commitMaxChunk
      *   Max records to be received in the commitRecords [[fs2.Pipe Pipe]]
      *   before a commit is run. Default is 1000
      * @param commitMaxWait
      *   Max duration to wait in commitRecords [[fs2.Pipe Pipe]] before a
      *   commit is run. Default is 10 seconds
      * @param commitMaxRetries
      *   Max number of retries for a commit operation
      * @param commitMaxRetryDuration
      * @param raiseOnError
      *   Whether the [[kinesis4cats.kcl.RecordProcessor RecordProcessor]]
      *   should raise exceptions or simply log them. It is recommended to set
      *   this to true. See this
      *   [[https://github.com/awslabs/amazon-kinesis-client/issues/10 issue]]
      *   for more information.
      * @param workerId
      *   Unique identifier for a single instance of this consumer. Default is a
      *   random UUID.
      * @param recordProcessorConfig
      *   [[kinesis4cats.kcl.RecordProcessor.Config RecordProcessor.Config]]
      * @param tfn
      *   Function to update the
      *   [[kinesis4cats.kcl.KCLConsumer.Config KCLConsumer.Config]]. Useful for
      *   overriding defaults.
      * @param F
      *   [[cats.effect.Async Async]] instance
      * @param encoders
      *   [[kinesis4cats.kcl.RecordProcessor.LogEncoders RecordProcessor.LogEncoders]]
      *   for encoding structured logs
      * @return
      *   [[cats.effect.Resource Resource]] containing the
      *   [[kinesis4cats.kcl.fs2.KCLConsumerFS2.Config KCLConsumerFS2.Config]]
      */
    def configsBuilder[F[_]](
        kinesisClient: KinesisAsyncClient,
        dynamoClient: DynamoDbAsyncClient,
        cloudWatchClient: CloudWatchAsyncClient,
        streamName: String,
        appName: String,
        queueSize: Int = 100,
        commitMaxChunk: Int = 1000,
        commitMaxWait: FiniteDuration = 10.seconds,
        commitMaxRetries: Int = 5,
        commitRetryDuration: FiniteDuration = 0.seconds,
        raiseOnError: Boolean = true,
        workerId: String = UUID.randomUUID.toString,
        recordProcessorConfig: RecordProcessor.Config =
          RecordProcessor.Config.default.copy(autoCommit = false)
    )(
        tfn: kinesis4cats.kcl.KCLConsumer.Config[
          F
        ] => kinesis4cats.kcl.KCLConsumer.Config[F] =
          (x: kinesis4cats.kcl.KCLConsumer.Config[F]) => x
    )(implicit
        F: Async[F],
        encoders: RecordProcessor.LogEncoders
    ): Resource[F, Config[F]] = for {
      queue <- Queue.bounded[F, CommittableRecord[F]](queueSize).toResource
      underlying <- kinesis4cats.kcl.KCLConsumer.Config
        .configsBuilder(
          kinesisClient,
          dynamoClient,
          cloudWatchClient,
          streamName,
          appName,
          raiseOnError,
          workerId,
          recordProcessorConfig
        )(callback(queue))(tfn)
    } yield Config(
      underlying,
      queue,
      commitMaxChunk,
      commitMaxWait,
      commitMaxRetries,
      commitRetryDuration
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
    _.groupWithin(config.maxCommitChunk, config.maxCommitWait)
      .evalTap(chunk =>
        chunk.toList.groupBy(_.shardId).toList.parTraverse_ {
          case (_, records) =>
            val max = records.max
            max.canCheckpoint.ifM(
              retryingOnAllErrors(
                limitRetries(config.maxCommitRetries)
                  .join(constantDelay(config.maxCommitRetryDuration)),
                noop[F, Throwable]
              )(max.checkpoint),
              F.unit
            )
        }
      )
      .unchunks
}
