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

package kinesis4cats

import java.util.UUID

import cats.effect.Async
import cats.effect.kernel.{Ref, Resource}
import cats.effect.syntax.all._
import cats.syntax.all._
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient
import software.amazon.kinesis.checkpoint.CheckpointConfig
import software.amazon.kinesis.common.ConfigsBuilder
import software.amazon.kinesis.coordinator._
import software.amazon.kinesis.leases.LeaseManagementConfig
import software.amazon.kinesis.lifecycle.LifecycleConfig
import software.amazon.kinesis.metrics.MetricsConfig
import software.amazon.kinesis.processor.ProcessorConfig
import software.amazon.kinesis.retrieval.RetrievalConfig

object KCLConsumer {
  def run[F[_]](
      config: KCLConsumerConfig
  )(implicit F: Async[F]): Resource[F, Unit] = for {
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
    _ <- F.delay(scheduler.run()).background
    _ <- Resource.onFinalize(
      F.fromCompletableFuture(F.delay(scheduler.startGracefulShutdown())).void
    )
  } yield ()

  def runWithWorkerStateChangeListener[F[_]](
      config: KCLConsumerConfig
  )(implicit
      F: Async[F]
  ): Resource[F, Ref[F, WorkerStateChangeListener.WorkerState]] = for {
    listener <- RefWorkerStateChangeListener[F]
    state <- Resource.pure(listener.state)
    _ <- run(
      config.copy(
        coordinatorConfig =
          config.coordinatorConfig.workerStateChangeListener(listener)
      )
    )
  } yield state
}

final case class KCLConsumerConfig private (
    checkpointConfig: CheckpointConfig,
    coordinatorConfig: CoordinatorConfig,
    leaseManagementConfig: LeaseManagementConfig,
    lifecycleConfig: LifecycleConfig,
    metricsConfig: MetricsConfig,
    processorConfig: ProcessorConfig,
    retrievalConfig: RetrievalConfig
)

object KCLConsumerConfig {
  def create[F[_]: Async](
      checkpointConfig: CheckpointConfig,
      coordinatorConfig: CoordinatorConfig,
      leaseManagementConfig: LeaseManagementConfig,
      lifecycleConfig: LifecycleConfig,
      metricsConfig: MetricsConfig,
      retrievalConfig: RetrievalConfig,
      recordProcessorConfig: RecordProcessorConfig =
        RecordProcessorConfig.default, // scalafix:ok
      callProcessRecordsEvenForEmptyRecordList: Boolean = false // scalafix:ok
  )(cb: List[CommittableRecord[F]] => F[Unit]): Resource[F, KCLConsumerConfig] =
    RecordProcessorFactory[F](recordProcessorConfig)(cb).map {
      processorFactory =>
        KCLConsumerConfig(
          checkpointConfig,
          coordinatorConfig,
          leaseManagementConfig,
          lifecycleConfig,
          metricsConfig,
          new ProcessorConfig(processorFactory)
            .callProcessRecordsEvenForEmptyRecordList(
              callProcessRecordsEvenForEmptyRecordList
            ),
          retrievalConfig
        )
    }

  def configsBuilder[F[_]: Async](
      kinesisClient: KinesisAsyncClient,
      dynamoClient: DynamoDbAsyncClient,
      cloudWatchClient: CloudWatchAsyncClient,
      streamName: String,
      appName: String,
      workerId: String = UUID.randomUUID.toString, // scalafix:ok
      recordProcessorConfig: RecordProcessorConfig =
        RecordProcessorConfig.default // scalafix:ok
  )(cb: List[CommittableRecord[F]] => F[Unit]): Resource[F, KCLConsumerConfig] =
    RecordProcessorFactory[F](recordProcessorConfig)(cb).map {
      processorFactory =>
        val confBuilder = new ConfigsBuilder(
          streamName,
          appName,
          kinesisClient,
          dynamoClient,
          cloudWatchClient,
          workerId,
          processorFactory
        )

        KCLConsumerConfig(
          confBuilder.checkpointConfig(),
          confBuilder.coordinatorConfig(),
          confBuilder.leaseManagementConfig(),
          confBuilder.lifecycleConfig(),
          confBuilder.metricsConfig(),
          confBuilder.processorConfig(),
          confBuilder.retrievalConfig()
        )
    }
}
