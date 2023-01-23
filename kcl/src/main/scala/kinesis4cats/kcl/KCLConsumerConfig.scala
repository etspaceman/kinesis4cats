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

import cats.effect.{Async, Deferred, Resource}
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

import kinesis4cats.kcl.processor._

/** Required configuration for the [[kinesis4cats.kcl.KCLConsumer]].
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
  * @param processorConfig
  *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/processor/ProcessorConfig.java ProcessorConfig]]
  * @param retrievalConfig
  *   [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/retrieval/RetrievalConfig.java RetrievalConfig]]
  * @param deferredException
  *   [[cats.effect.Deferred Deferred]] instance, for handling exceptions
  * @param raiseOnError
  *   Whether the [[kinesis4cats.kcl.processor.RecordProcessor RecordProcessor]]
  *   should raise exceptions or simply log them. It is recommended to set this
  *   to true. See this
  *   [[https://github.com/awslabs/amazon-kinesis-client/issues/10 issue]] for
  *   more information.
  */
final case class KCLConsumerConfig[F[_]] private (
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

object KCLConsumerConfig {

  /** Low-level constructor for
    * [[kinesis4cats.kcl.KCLConsumerConfig KCLConsumerConfig]].
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
    * @param raiseOnError
    *   Whether the
    *   [[kinesis4cats.kcl.processor.RecordProcessor RecordProcessor]] should
    *   raise exceptions or simply log them. It is recommended to set this to
    *   true. See this
    *   [[https://github.com/awslabs/amazon-kinesis-client/issues/10 issue]] for
    *   more information.
    * @param recordProcessorConfig
    *   [[kinesis4cats.kcl.processor.RecordProcessorConfig RecordProcessorConfig]]
    * @param callProcessRecordsEvenForEmptyRecordList
    *   Determines if processRecords() should run on the record processor for
    *   empty record lists.
    * @param cb
    *   Function to process
    *   [[kinesis4cats.kcl.CommittableRecord CommittableRecords]] received from
    *   Kinesis
    * @param F
    *   [[cats.effect.Async Async]] instance
    * @param encoders
    *   [[kinesis4cats.kcl.processor.RecordProcessorLogEncoders RecordProcessorLogEncoders]]
    *   for encoding structured logs
    * @return
    *   [[cats.effect.Resource Resource]] containing the
    *   [[kinesis4cats.kcl.KCLConsumerConfig KCLConsumerConfig]]
    */
  def create[F[_]](
      checkpointConfig: CheckpointConfig,
      coordinatorConfig: CoordinatorConfig,
      leaseManagementConfig: LeaseManagementConfig,
      lifecycleConfig: LifecycleConfig,
      metricsConfig: MetricsConfig,
      retrievalConfig: RetrievalConfig,
      raiseOnError: Boolean = true,
      recordProcessorConfig: RecordProcessorConfig =
        RecordProcessorConfig.default,
      callProcessRecordsEvenForEmptyRecordList: Boolean = false
  )(cb: List[CommittableRecord[F]] => F[Unit])(implicit
      F: Async[F],
      encoders: RecordProcessorLogEncoders
  ): Resource[F, KCLConsumerConfig[F]] =
    for {
      deferredException <- Resource.eval(Deferred[F, Throwable])
      processorFactory <- RecordProcessorFactory[F](
        recordProcessorConfig,
        deferredException,
        raiseOnError
      )(cb)
    } yield KCLConsumerConfig(
      checkpointConfig,
      coordinatorConfig,
      leaseManagementConfig,
      lifecycleConfig,
      metricsConfig,
      new ProcessorConfig(processorFactory)
        .callProcessRecordsEvenForEmptyRecordList(
          callProcessRecordsEvenForEmptyRecordList
        ),
      retrievalConfig,
      deferredException,
      raiseOnError
    )

  private def defaultTfn[F[_]]: KCLConsumerConfig[F] => KCLConsumerConfig[F] =
    identity

  /** Constructor for the
    * [[kinesis4cats.kcl.KCLConsumerConfig KCLConsumerConfig]] that leverages
    * the
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
    * @param raiseOnError
    *   Whether the
    *   [[kinesis4cats.kcl.processor.RecordProcessor RecordProcessor]] should
    *   raise exceptions or simply log them. It is recommended to set this to
    *   true. See this
    *   [[https://github.com/awslabs/amazon-kinesis-client/issues/10 issue]] for
    *   more information.
    * @param workerId
    *   Unique identifier for a single instance of this consumer. Default is a
    *   random UUID.
    * @param recordProcessorConfig
    *   [[kinesis4cats.kcl.processor.RecordProcessorConfig RecordProcessorConfig]]
    * @param cb
    *   Function to process
    *   [[kinesis4cats.kcl.CommittableRecord CommittableRecords]] received from
    *   Kinesis
    * @param tfn
    *   Function to update the
    *   [[kinesis4cats.kcl.KCLConsumerConfig KCLConsumerConfig]]. Useful for
    *   overriding defaults.
    * @param F
    *   [[cats.effect.Async Async]] instance
    * @param encoders
    *   [[kinesis4cats.kcl.processor.RecordProcessorLogEncoders RecordProcessorLogEncoders]]
    *   for encoding structured logs
    * @return
    *   [[cats.effect.Resource Resource]] containing the
    *   [[kinesis4cats.kcl.KCLConsumerConfig KCLConsumerConfig]]
    * @return
    */
  def configsBuilder[F[_]](
      kinesisClient: KinesisAsyncClient,
      dynamoClient: DynamoDbAsyncClient,
      cloudWatchClient: CloudWatchAsyncClient,
      streamName: String,
      appName: String,
      raiseOnError: Boolean = true,
      workerId: String = UUID.randomUUID.toString,
      recordProcessorConfig: RecordProcessorConfig =
        RecordProcessorConfig.default
  )(
      cb: List[CommittableRecord[F]] => F[Unit]
  )(
      tfn: KCLConsumerConfig[F] => KCLConsumerConfig[F] = defaultTfn
  )(implicit
      F: Async[F],
      encoders: RecordProcessorLogEncoders
  ): Resource[F, KCLConsumerConfig[F]] = for {
    deferredException <- Resource.eval(Deferred[F, Throwable])
    processorFactory <- RecordProcessorFactory[F](
      recordProcessorConfig,
      deferredException,
      raiseOnError
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
    KCLConsumerConfig(
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
