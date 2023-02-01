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

import java.util.UUID

import cats.effect.kernel.Deferred
import cats.effect.syntax.all._
import cats.effect.{Async, Ref, Resource}
import cats.syntax.all._
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient
import software.amazon.kinesis.checkpoint.CheckpointConfig
import software.amazon.kinesis.common.ConfigsBuilder
import software.amazon.kinesis.coordinator.WorkerStateChangeListener.WorkerState
import software.amazon.kinesis.coordinator._
import software.amazon.kinesis.leases.LeaseManagementConfig
import software.amazon.kinesis.lifecycle.LifecycleConfig
import software.amazon.kinesis.metrics.MetricsConfig
import software.amazon.kinesis.processor.ProcessorConfig
import software.amazon.kinesis.retrieval.RetrievalConfig

import kinesis4cats.kcl.WorkerListeners._
import kinesis4cats.kcl.multistream.MultiStreamTracker
import kinesis4cats.syntax.id._

/** Wrapper offering for the
  * [[https://docs.aws.amazon.com/streams/latest/dev/shared-throughput-kcl-consumers.html KCL]]
  *
  * @param config
  *   [[kinesis4cats.kcl.KCLConsumer.Config Config]]
  * @param F
  *   [[cats.effect.Async Async]]
  */
class KCLConsumer[F[_]] private[kinesis4cats] (
    config: KCLConsumer.Config[F]
)(implicit F: Async[F]) {

  /** Runs a [[https://github.com/awslabs/amazon-kinesis-client KCL Consumer]]
    * in the background as a [[cats.effect.Resource Resource]]
    *
    * @return
    *   [[cats.effect.Resource Resource]] that manages the lifecycle of the
    *   [[https://github.com/awslabs/amazon-kinesis-client KCL Consumer]]
    */
  def run(): Resource[F, Unit] =
    KCLConsumer.run(config)

  /** Runs a [[https://github.com/awslabs/amazon-kinesis-client KCL Consumer]]
    * in the background as a [[cats.effect.Resource Resource]]. This exposes the
    * [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/coordinator/WorkerStateChangeListener.java WorkerState]]
    * which can be used as a means to determine if the KCL Consumer is running.
    * A common use case for this is to service healthcheck endpoints when
    * running these in an orchestration service (e.g. Kubernetes or AWS ECS) or
    * running tests that require the consumer to be up before an assertion is
    * checked.
    *
    * @return
    *   [[cats.effect.Resource Resource]] that manages the lifecycle of the
    *   [[https://github.com/awslabs/amazon-kinesis-client KCL Consumer]]
    */
  def runWithRefListener(): Resource[F, Ref[F, WorkerState]] = for {
    listener <- RefListener[F]
    state = listener.state
    _ <- KCLConsumer.run(
      config.copy(
        coordinatorConfig =
          config.coordinatorConfig.workerStateChangeListener(listener)
      )
    )
  } yield state

  /** Runs a [[https://github.com/awslabs/amazon-kinesis-client KCL Consumer]]
    * in the background as a [[cats.effect.Resource Resource]]. This exposes the
    * [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/coordinator/WorkerStateChangeListener.java WorkerState]]
    * which can be used as a means to determine if the KCL Consumer is running.
    * A common use case for this is to service healthcheck endpoints when
    * running these in an orchestration service (e.g. Kubernetes or AWS ECS) or
    * running tests that require the consumer to be up before an assertion is
    * checked.
    *
    * Unlike `runWithRefListener`, this method uses a
    * [[cats.effect.Deferred Deferred]] instance. This is useful when you only
    * need to react when the KCL has reached a specific state 1 time (e.g. wait
    * to produce to a stream until the consumer is started)
    *
    * @param stateToCompleteOn
    *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/coordinator/WorkerStateChangeListener.java WorkerState]]
    *   to expect when completing the [[cats.effect.Deferred Deferred]]. Default
    *   is STARTED.
    * @return
    *   [[cats.effect.Resource Resource]] that manages the lifecycle of the
    *   [[https://github.com/awslabs/amazon-kinesis-client KCL Consumer]]
    */
  def runWithDeferredListener(
      stateToCompleteOn: WorkerState = WorkerState.STARTED
  ): Resource[F, Deferred[F, Unit]] = for {
    listener <- DeferredListener[F](stateToCompleteOn)
    deferred = listener.deferred
    _ <- KCLConsumer.run(
      config.copy(
        coordinatorConfig =
          config.coordinatorConfig.workerStateChangeListener(listener)
      )
    )
  } yield deferred
}

object KCLConsumer {

  /** Low-level constructor for the
    * [[kinesis4cats.kcl.KCLConsumer KCLConsumer]].
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
    * @param processConfig
    *   [[kinesis4cats.kcl.KCLConsumer.ProcessConfig KCLConsumer.ProcessConfig]]
    * @param cb
    *   Function to process
    *   [[kinesis4cats.kcl.CommittableRecord CommittableRecords]] received from
    *   Kinesis
    * @param F
    *   [[cats.effect.Async Async]] instance
    * @param encoders
    *   [[kinesis4cats.kcl.RecordProcessor.LogEncoders RecordProcessor.LogEncoders]]
    *   for encoding structured logs
    * @return
    *   [[cats.effect.Resource Resource]] containing the
    *   [[kinesis4cats.kcl.KCLConsumer KCLConsumer]]
    */
  def apply[F[_]](
      checkpointConfig: CheckpointConfig,
      coordinatorConfig: CoordinatorConfig,
      leaseManagementConfig: LeaseManagementConfig,
      lifecycleConfig: LifecycleConfig,
      metricsConfig: MetricsConfig,
      retrievalConfig: RetrievalConfig,
      processConfig: ProcessConfig = ProcessConfig.default
  )(cb: List[CommittableRecord[F]] => F[Unit])(implicit
      F: Async[F],
      encoders: RecordProcessor.LogEncoders
  ): Resource[F, KCLConsumer[F]] = Config
    .create(
      checkpointConfig,
      coordinatorConfig,
      leaseManagementConfig,
      lifecycleConfig,
      metricsConfig,
      retrievalConfig,
      processConfig
    )(cb)
    .map(new KCLConsumer[F](_))

  /** Constructor for the [[kinesis4cats.kcl.KCLConsumer KCLConsumer]] that
    * leverages the
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
    * @param workerId
    *   Unique identifier for a single instance of this consumer. Default is a
    *   random UUID.
    * @param processConfig
    *   [[kinesis4cats.kcl.KCLConsumer.ProcessConfig KCLConsumer.ProcessConfig]]
    * @param cb
    *   Function to process
    *   [[kinesis4cats.kcl.CommittableRecord CommittableRecords]] received from
    *   Kinesis
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
    *   [[kinesis4cats.kcl.KCLConsumer KCLConsumer]]
    * @return
    */
  def configsBuilder[F[_]](
      kinesisClient: KinesisAsyncClient,
      dynamoClient: DynamoDbAsyncClient,
      cloudWatchClient: CloudWatchAsyncClient,
      streamName: String,
      appName: String,
      workerId: String = UUID.randomUUID.toString,
      processConfig: ProcessConfig = ProcessConfig.default
  )(
      cb: List[CommittableRecord[F]] => F[Unit]
  )(
      tfn: Config[F] => Config[F] = (x: Config[F]) => x
  )(implicit
      F: Async[F],
      encoders: RecordProcessor.LogEncoders
  ): Resource[F, KCLConsumer[F]] = Config
    .configsBuilder(
      kinesisClient,
      dynamoClient,
      cloudWatchClient,
      streamName,
      appName,
      workerId,
      processConfig
    )(cb)(tfn)
    .map(new KCLConsumer[F](_))

  /** Config class for the [[kinesis4cats.kcl.KCLConsumer KCLConsumer]]
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
    * @param deferredException
    *   [[cats.effect.Deferred Deferred]] that completes if an exception is
    *   thrown in the [[kinesis4cats.kcl.RecordProcessor RecordProcessor]], if
    *   raiseOnError is true
    * @param raiseOnError
    *   Whether the [[kinesis4cats.kcl.RecordProcessor RecordProcessor]] should
    *   raise exceptions or simply log them. It is recommended to set this to
    *   true. See this
    *   [[https://github.com/awslabs/amazon-kinesis-client/issues/10 issue]] for
    *   more information.
    */
  final case class Config[F[_]] private (
      checkpointConfig: CheckpointConfig,
      coordinatorConfig: CoordinatorConfig,
      leaseManagementConfig: LeaseManagementConfig,
      lifecycleConfig: LifecycleConfig,
      metricsConfig: MetricsConfig,
      processorConfig: ProcessorConfig,
      retrievalConfig: RetrievalConfig,
      deferredException: Deferred[F, Throwable],
      raiseOnError: Boolean
  )

  /** Helper class to hold configuration for the
    * [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/processor/ProcessorConfig.java ProcessorConfig]]
    * construction
    *
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
    *   empty record lists. Default None.
    */
  final case class ProcessConfig(
      raiseOnError: Boolean,
      recordProcessorConfig: RecordProcessor.Config,
      callProcessRecordsEvenForEmptyRecordList: Option[Boolean]
  )

  object ProcessConfig {
    val default: ProcessConfig =
      ProcessConfig(true, RecordProcessor.Config.default, None)
  }

  object Config {

    /** Low-level constructor for
      * [[kinesis4cats.kcl.KCLConsumer.Config KCLConsumer.Config]].
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
      * @param processConfig
      *   [[kinesis4cats.kcl.KCLConsumer.ProcessConfig KCLConsumer.ProcessConfig]]
      * @param cb
      *   Function to process
      *   [[kinesis4cats.kcl.CommittableRecord CommittableRecords]] received
      *   from Kinesis
      * @param F
      *   [[cats.effect.Async Async]] instance
      * @param encoders
      *   [[kinesis4cats.kcl.RecordProcessor.LogEncoders RecordProcessor.LogEncoders]]
      *   for encoding structured logs
      * @return
      *   [[cats.effect.Resource Resource]] containing the
      *   [[kinesis4cats.kcl.KCLConsumer.Config KCLConsumer.Config]]
      */
    def create[F[_]](
        checkpointConfig: CheckpointConfig,
        coordinatorConfig: CoordinatorConfig,
        leaseManagementConfig: LeaseManagementConfig,
        lifecycleConfig: LifecycleConfig,
        metricsConfig: MetricsConfig,
        retrievalConfig: RetrievalConfig,
        processConfig: ProcessConfig = ProcessConfig.default
    )(cb: List[CommittableRecord[F]] => F[Unit])(implicit
        F: Async[F],
        encoders: RecordProcessor.LogEncoders
    ): Resource[F, Config[F]] =
      for {
        deferredException <- Resource.eval(Deferred[F, Throwable])
        processorFactory <- RecordProcessor.Factory[F](
          processConfig.recordProcessorConfig,
          deferredException,
          processConfig.raiseOnError
        )(cb)
      } yield Config(
        checkpointConfig,
        coordinatorConfig,
        leaseManagementConfig,
        lifecycleConfig,
        metricsConfig,
        new ProcessorConfig(processorFactory)
          .maybeTransform(
            processConfig.callProcessRecordsEvenForEmptyRecordList
          )(
            _.callProcessRecordsEvenForEmptyRecordList(_)
          ),
        retrievalConfig,
        deferredException,
        processConfig.raiseOnError
      )

    /** Constructor for the
      * [[kinesis4cats.kcl.KCLConsumer.Config KCLConsumer.Config]] that
      * leverages the
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
      * @param workerId
      *   Unique identifier for a single instance of this consumer. Default is a
      *   random UUID.
      * @param processConfig
      *   [[kinesis4cats.kcl.KCLConsumer.ProcessConfig KCLConsumer.ProcessConfig]]
      * @param cb
      *   Function to process
      *   [[kinesis4cats.kcl.CommittableRecord CommittableRecords]] received
      *   from Kinesis
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
      *   [[kinesis4cats.kcl.KCLConsumer.Config KCLConsumer.Config]]
      * @return
      */
    def configsBuilder[F[_]](
        kinesisClient: KinesisAsyncClient,
        dynamoClient: DynamoDbAsyncClient,
        cloudWatchClient: CloudWatchAsyncClient,
        streamName: String,
        appName: String,
        workerId: String = UUID.randomUUID.toString,
        processConfig: ProcessConfig = ProcessConfig.default
    )(
        cb: List[CommittableRecord[F]] => F[Unit]
    )(
        tfn: Config[F] => Config[F] = (x: Config[F]) => x
    )(implicit
        F: Async[F],
        encoders: RecordProcessor.LogEncoders
    ): Resource[F, Config[F]] = for {
      deferredException <- Resource.eval(Deferred[F, Throwable])
      processorFactory <- RecordProcessor.Factory[F](
        processConfig.recordProcessorConfig,
        deferredException,
        processConfig.raiseOnError
      )(cb)
      confBuilder = new ConfigsBuilder(
        streamName,
        appName,
        kinesisClient,
        dynamoClient,
        cloudWatchClient,
        workerId,
        processorFactory
      )
    } yield tfn(
      Config(
        confBuilder.checkpointConfig(),
        confBuilder.coordinatorConfig(),
        confBuilder.leaseManagementConfig(),
        confBuilder.lifecycleConfig(),
        confBuilder.metricsConfig(),
        confBuilder.processorConfig(),
        confBuilder.retrievalConfig(),
        deferredException,
        processConfig.raiseOnError
      )
    )

    /** Constructor for the
      * [[kinesis4cats.kcl.KCLConsumer.Config KCLConsumer.Config]] that
      * leverages the
      * [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/common/ConfigsBuilder.java ConfigsBuilder]]
      * from the KCL. This is a simpler entry-point for creating the
      * configuration, and provides a transform function to add any custom
      * configuration that was not covered by the default. This constructor
      * specifically leverages the
      * [[kinesis4cats.kcl.multistream.MultiStreamTracker MultiStreamTracker]]
      * to allow for consumption from multiple streams.
      *
      * @param kinesisClient
      *   [[https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/kinesis/KinesisAsyncClient.html KinesisAsyncClient]]
      * @param dynamoClient
      *   [[https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/dynamodb/DynamoDbAsyncClient.html DynamoDbAsyncClient]]
      * @param cloudWatchClient
      *   [[https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/cloudwatch/CloudWatchClient.html CloudWatchClient]]
      * @param tracker
      *   [[kinesis4cats.kcl.multistream.MultiStreamTracker MultiStreamTracker]]
      *   containing the streams and positions for consumption
      * @param appName
      *   Name of the application. Usually also the dynamo table name for
      *   checkpoints
      * @param workerId
      *   Unique identifier for a single instance of this consumer. Default is a
      *   random UUID.
      * @param processConfig
      *   [[kinesis4cats.kcl.KCLConsumer.ProcessConfig KCLConsumer.ProcessConfig]]
      * @param cb
      *   Function to process
      *   [[kinesis4cats.kcl.CommittableRecord CommittableRecords]] received
      *   from Kinesis
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
      *   [[kinesis4cats.kcl.KCLConsumer.Config KCLConsumer.Config]]
      * @return
      */
    def configsBuilderMultiStream[F[_]](
        kinesisClient: KinesisAsyncClient,
        dynamoClient: DynamoDbAsyncClient,
        cloudWatchClient: CloudWatchAsyncClient,
        tracker: MultiStreamTracker,
        appName: String,
        raiseOnError: Boolean = true,
        workerId: String = UUID.randomUUID.toString,
        recordProcessorConfig: RecordProcessor.Config =
          RecordProcessor.Config.default
    )(
        cb: List[CommittableRecord[F]] => F[Unit]
    )(
        tfn: Config[F] => Config[F] = (x: Config[F]) => x
    )(implicit
        F: Async[F],
        encoders: RecordProcessor.LogEncoders
    ): Resource[F, Config[F]] = for {
      deferredException <- Resource.eval(Deferred[F, Throwable])
      processorFactory <- RecordProcessor.Factory[F](
        recordProcessorConfig,
        deferredException,
        raiseOnError
      )(cb)
      confBuilder = new ConfigsBuilder(
        tracker,
        appName,
        kinesisClient,
        dynamoClient,
        cloudWatchClient,
        workerId,
        processorFactory
      )
    } yield tfn(
      Config(
        confBuilder.checkpointConfig(),
        confBuilder.coordinatorConfig(),
        confBuilder.leaseManagementConfig(),
        confBuilder.lifecycleConfig(),
        confBuilder.metricsConfig(),
        confBuilder.processorConfig(),
        confBuilder.retrievalConfig(),
        deferredException,
        raiseOnError
      )
    )
  }

  /** Runs a [[https://github.com/awslabs/amazon-kinesis-client KCL Consumer]]
    * in the background as a [[cats.effect.Resource Resource]]
    *
    * @return
    *   [[cats.effect.Resource Resource]] that manages the lifecycle of the
    *   [[https://github.com/awslabs/amazon-kinesis-client KCL Consumer]]
    */
  private[kinesis4cats] def run[F[_]](
      config: Config[F]
  )(implicit F: Async[F]): Resource[F, Unit] =
    for {
      scheduler <- Resource.eval(
        F.delay(
          new Scheduler(
            config.checkpointConfig,
            config.coordinatorConfig,
            config.leaseManagementConfig,
            config.lifecycleConfig,
            config.metricsConfig,
            config.processorConfig,
            config.retrievalConfig
          )
        )
      )
      _ <- F
        .race(
          F.blocking(scheduler.run()),
          if (config.raiseOnError)
            config.deferredException.get.flatMap(F.raiseError(_).void)
          else F.never
        )
        .background
      _ <- Resource.onFinalize(
        for {
          _ <- F.fromCompletableFuture(
            F.delay(scheduler.startGracefulShutdown())
          )
          // There is sometimes a race condition which causes the graceful shutdown to complete
          // but with a returned value of `false`, meaning that the Scheduler is not fully
          // shut down. We run the shutdown() directly in these cases.
          // See https://github.com/awslabs/amazon-kinesis-client/issues/616
          _ <- F.blocking(scheduler.shutdown())
        } yield ()
      )
    } yield ()
}
