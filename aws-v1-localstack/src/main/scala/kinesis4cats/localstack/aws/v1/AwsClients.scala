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

package kinesis4cats.localstack
package aws.v1

import cats.effect.syntax.all._
import cats.effect.{Async, Resource}
import cats.syntax.all._
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.cloudwatch._
import com.amazonaws.services.dynamodbv2._
import com.amazonaws.services.kinesis._
import com.amazonaws.services.kinesis.model._

import kinesis4cats.compat.retry._

/** Helpers for constructing and leveraging AWS Java Client interfaces with
  * Localstack.
  */
object AwsClients {

  /** Builds a
    * [[https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/kinesis/AmazonKinesisAsync.html AmazonKinesisAsync]]
    * that is compliant for Localstack usage.
    *
    * @param config
    *   [[kinesis4cats.localstack.LocalstackConfig LocalstackConfig]]
    * @param F
    *   F with an [[cats.effect.Async Async]] instance
    * @return
    *   F of
    *   [[https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/kinesis/AmazonKinesisAsync.html AmazonKinesisAsync]]
    */
  def kinesisClient[F[_]](
      config: LocalstackConfig
  )(implicit F: Async[F]): F[AmazonKinesisAsync] =
    F.delay(
      AmazonKinesisAsyncClientBuilder
        .standard()
        .withEndpointConfiguration(
          new EndpointConfiguration(config.kinesisEndpoint, config.region.name)
        )
        .withCredentials(AwsCreds.LocalCredsProvider)
        .build()
    )

  /** Builds a
    * [[https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/kinesis/AmazonKinesisAsync.html AmazonKinesisAsync]]
    * that is compliant for Localstack usage.
    *
    * @param prefix
    *   Optional prefix for parsing configuration. Default to None
    * @param F
    *   F with an [[cats.effect.Async Async]] instance
    * @return
    *   F of
    *   [[https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/kinesis/AmazonKinesisAsync.html AmazonKinesisAsync]]
    */
  def kinesisClient[F[_]](
      prefix: Option[String] = None
  )(implicit F: Async[F]): F[AmazonKinesisAsync] = for {
    config <- LocalstackConfig.load[F](prefix)
    client <- kinesisClient(config)
  } yield client

  /** Builds a
    * [[https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/kinesis/AmazonKinesisAsync.html AmazonKinesisAsync]]
    * that is compliant for Localstack usage. Lifecycle is managed as a
    * [[cats.effect.Resource Resource]].
    *
    * @param config
    *   [[kinesis4cats.localstack.LocalstackConfig LocalstackConfig]]
    * @param F
    *   F with an [[cats.effect.Async Async]] instance
    * @return
    *   [[cats.effect.Resource Resource]] of
    *   [[https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/kinesis/AmazonKinesisAsync.html AmazonKinesisAsync]]
    */
  def kinesisClientResource[F[_]](config: LocalstackConfig)(implicit
      F: Async[F]
  ): Resource[F, AmazonKinesisAsync] =
    kinesisClient[F](config).toResource

  /** Builds a
    * [[https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/kinesis/AmazonKinesisAsync.html AmazonKinesisAsync]]
    * that is compliant for Localstack usage. Lifecycle is managed as a
    * [[cats.effect.Resource Resource]].
    *
    * @param prefix
    *   Optional prefix for parsing configuration. Default to None
    * @param F
    *   F with an [[cats.effect.Async Async]] instance
    * @return
    *   [[cats.effect.Resource Resource]] of
    *   [[https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/kinesis/AmazonKinesisAsync.html AmazonKinesisAsync]]
    */
  def kinesisClientResource[F[_]](
      prefix: Option[String] = None
  )(implicit
      F: Async[F]
  ): Resource[F, AmazonKinesisAsync] =
    kinesisClient[F](prefix).toResource

  /** Creates a stream and awaits for the status to be ready
    *
    * @param client
    *   [[https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/kinesis/AmazonKinesisAsync.html AmazonKinesisAsync]]
    *   to use
    * @param streamName
    *   Stream name
    * @param shardCount
    *   Shard count for stream
    * @param describeRetries
    *   How many times to retry DescribeStreamSummary when checking the stream
    *   status
    * @param describeRetryDuration
    *   How long to delay between retries of the DescribeStreamSummary call
    * @param F
    *   F with an [[cats.effect.Async Async]] instance
    * @return
    */
  def createStream[F[_]](
      client: AmazonKinesisAsync,
      config: TestStreamConfig[F]
  )(implicit F: Async[F]): F[Unit] =
    for {
      _ <- F.interruptibleMany(
        client.createStream(
          new CreateStreamRequest()
            .withStreamName(config.streamName)
            .withShardCount(config.shardCount)
            .withStreamModeDetails(
              new StreamModeDetails().withStreamMode(StreamMode.PROVISIONED)
            )
        )
      )
      _ <- retryingOnFailuresAndAllErrors(
        config.describeRetryPolicy,
        (x: DescribeStreamSummaryResult) =>
          F.pure(
            x.getStreamDescriptionSummary().getStreamStatus() === "ACTIVE"
          ),
        noop[F, DescribeStreamSummaryResult],
        noop[F, Throwable]
      )(
        F.interruptibleMany(
          client.describeStreamSummary(
            new DescribeStreamSummaryRequest().withStreamName(config.streamName)
          )
        )
      )
    } yield ()

  /** Deletes a stream and awaits for the stream deletion to be finalized
    *
    * @param client
    *   [[https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/kinesis/AmazonKinesisAsync.html AmazonKinesisAsync]]
    *   to use
    * @param streamName
    *   Stream name
    * @param describeRetries
    *   How many times to retry DescribeStreamSummary when checking the stream
    *   status
    * @param describeRetryDuration
    *   How long to delay between retries of the DescribeStreamSummary call
    * @param F
    *   F with an [[cats.effect.Async Async]] instance
    * @return
    */
  def deleteStream[F[_]](
      client: AmazonKinesisAsync,
      config: TestStreamConfig[F]
  )(implicit F: Async[F]): F[Unit] =
    for {
      _ <- F.interruptibleMany(client.deleteStream(config.streamName))
      _ <- retryingOnFailuresAndSomeErrors(
        config.describeRetryPolicy,
        (x: Either[Throwable, DescribeStreamSummaryResult]) =>
          F.pure(
            x.swap.exists {
              case _: ResourceNotFoundException => true
              case _                            => false
            }
          ),
        (e: Throwable) =>
          e match {
            case _: ResourceNotFoundException => F.pure(false)
            case _                            => F.pure(true)
          },
        noop[F, Either[Throwable, DescribeStreamSummaryResult]],
        noop[F, Throwable]
      )(
        F.interruptibleMany(
          client.describeStreamSummary(
            new DescribeStreamSummaryRequest().withStreamName(config.streamName)
          )
        ).attempt
      )
    } yield ()

  /** A resource that does the following:
    *
    *   - Builds a
    *     [[https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/kinesis/AmazonKinesisAsync.html AmazonKinesisAsync]]
    *     that is compliant for Localstack usage.
    *   - Creates a stream with the desired name and shard count, and waits
    *     until the stream is active.
    *   - Destroys the stream when the [[cats.effect.Resource Resource]] is
    *     closed
    *
    * @param config
    *   [[kinesis4cats.localstack.LocalstackConfig LocalstackConfig]]
    * @param streamName
    *   Stream name
    * @param shardCount
    *   Shard count for stream
    * @param describeRetries
    *   How many times to retry DescribeStreamSummary when checking the stream
    *   status
    * @param describeRetryDuration
    *   How long to delay between retries of the DescribeStreamSummary call
    * @param F
    *   F with an [[cats.effect.Async Async]] instance
    * @return
    *   [[cats.effect.Resource Resource]] of
    *   [[https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/kinesis/AmazonKinesisAsync.html AmazonKinesisAsync]]
    */
  def kinesisStreamResource[F[_]](
      localstackConfig: LocalstackConfig,
      streamsToCreate: List[TestStreamConfig[F]]
  )(implicit
      F: Async[F]
  ): Resource[F, AmazonKinesisAsync] = for {
    client <- kinesisClientResource(localstackConfig)
    _ <- streamsToCreate.traverse_(config =>
      Resource.make(
        createStream(client, config).as(client)
      )(client => deleteStream(client, config))
    )
  } yield client

  /** A resource that does the following:
    *
    *   - Builds a
    *     [[https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/kinesis/AmazonKinesisAsync.html AmazonKinesisAsync]]
    *     that is compliant for Localstack usage.
    *   - Creates a stream with the desired name and shard count, and waits
    *     until the stream is active.
    *   - Destroys the stream when the [[cats.effect.Resource Resource]] is
    *     closed
    *
    * @param streamName
    *   Stream name
    * @param shardCount
    *   Shard count for stream
    * @param prefix
    *   Optional prefix for parsing configuration. Default to None
    * @param describeRetries
    *   How many times to retry DescribeStreamSummary when checking the stream
    *   status. Default to 5
    * @param describeRetryDuration
    *   How long to delay between retries of the DescribeStreamSummary call.
    *   Default to 500 ms
    * @param F
    *   F with an [[cats.effect.Async Async]] instance
    * @return
    *   [[cats.effect.Resource Resource]] of
    *   [[https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/kinesis/AmazonKinesisAsync.html AmazonKinesisAsync]]
    */
  def kinesisStreamResource[F[_]](
      streamsToCreate: List[TestStreamConfig[F]],
      prefix: Option[String] = None
  )(implicit
      F: Async[F]
  ): Resource[F, AmazonKinesisAsync] = for {
    localstackConfig <- LocalstackConfig.resource(prefix)
    result <- kinesisStreamResource(localstackConfig, streamsToCreate)
  } yield result

  /** Builds a
    * [[https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/dynamodbv2/AmazonDynamoDBAsync.html AmazonDynamoDBAsync]]
    * that is compliant for Localstack usage.
    *
    * @param config
    *   [[kinesis4cats.localstack.LocalstackConfig LocalstackConfig]]
    * @param F
    *   F with an [[cats.effect.Async Async]] instance
    * @return
    *   F of
    *   [[https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/dynamodbv2/AmazonDynamoDBAsync.html AmazonDynamoDBAsync]]
    */
  def dynamoClient[F[_]](
      config: LocalstackConfig
  )(implicit F: Async[F]): F[AmazonDynamoDBAsync] =
    F.delay(
      AmazonDynamoDBAsyncClientBuilder
        .standard()
        .withEndpointConfiguration(
          new EndpointConfiguration(config.dynamoEndpoint, config.region.name)
        )
        .withCredentials(AwsCreds.LocalCredsProvider)
        .build()
    )

  /** Builds a
    * [[https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/dynamodbv2/AmazonDynamoDBAsync.html AmazonDynamoDBAsync]]
    * that is compliant for Localstack usage.
    *
    * @param prefix
    *   Optional prefix for parsing configuration. Default to None
    * @param F
    *   F with an [[cats.effect.Async Async]] instance
    * @return
    *   F of
    *   [[https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/dynamodbv2/AmazonDynamoDBAsync.html AmazonDynamoDBAsync]]
    */
  def dynamoClient[F[_]](
      prefix: Option[String] = None
  )(implicit F: Async[F]): F[AmazonDynamoDBAsync] = for {
    config <- LocalstackConfig.load[F](prefix)
    client <- dynamoClient(config)
  } yield client

  /** Builds a
    * [[https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/dynamodbv2/AmazonDynamoDBAsync.html AmazonDynamoDBAsync]]
    * that is compliant for Localstack usage. Lifecycle is managed as a
    * [[cats.effect.Resource Resource]].
    *
    * @param config
    *   [[kinesis4cats.localstack.LocalstackConfig LocalstackConfig]]
    * @param F
    *   F with an [[cats.effect.Async Async]] instance
    * @return
    *   [[cats.effect.Resource Resource]] of
    *   [[https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/dynamodbv2/AmazonDynamoDBAsync.html AmazonDynamoDBAsync]]
    */
  def dynamoClientResource[F[_]](config: LocalstackConfig)(implicit
      F: Async[F]
  ): Resource[F, AmazonDynamoDBAsync] =
    dynamoClient[F](config).toResource

  /** Builds a
    * [[https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/dynamodbv2/AmazonDynamoDBAsync.html AmazonDynamoDBAsync]]
    * that is compliant for Localstack usage. Lifecycle is managed as a
    * [[cats.effect.Resource Resource]].
    *
    * @param prefix
    *   Optional prefix for parsing configuration. Default to None
    * @param F
    *   F with an [[cats.effect.Async Async]] instance
    * @return
    *   [[cats.effect.Resource Resource]] of
    *   [[https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/dynamodbv2/AmazonDynamoDBAsync.html AmazonDynamoDBAsync]]
    */
  def dynamoClientResource[F[_]](
      prefix: Option[String] = None
  )(implicit
      F: Async[F]
  ): Resource[F, AmazonDynamoDBAsync] =
    dynamoClient[F](prefix).toResource

  /** Builds a
    * [[https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/cloudwatch/AmazonCloudWatchAsync.html AmazonCloudWatchAsync]]
    * that is compliant for Localstack usage.
    *
    * @param config
    *   [[kinesis4cats.localstack.LocalstackConfig LocalstackConfig]]
    * @param F
    *   F with an [[cats.effect.Async Async]] instance
    * @return
    *   F of
    *   [[https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/cloudwatch/AmazonCloudWatchAsync.html AmazonCloudWatchAsync]]
    */
  def cloudwatchClient[F[_]](
      config: LocalstackConfig
  )(implicit F: Async[F]): F[AmazonCloudWatchAsync] =
    F.delay(
      AmazonCloudWatchAsyncClientBuilder
        .standard()
        .withEndpointConfiguration(
          new EndpointConfiguration(
            config.cloudwatchEndpoint,
            config.region.name
          )
        )
        .withCredentials(AwsCreds.LocalCredsProvider)
        .build()
    )

  /** Builds a
    * [[https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/cloudwatch/AmazonCloudWatchAsync.html AmazonCloudWatchAsync]]
    * that is compliant for Localstack usage.
    *
    * @param prefix
    *   Optional prefix for parsing configuration. Default to None
    * @param F
    *   F with an [[cats.effect.Async Async]] instance
    * @return
    *   F of
    *   [[https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/cloudwatch/AmazonCloudWatchAsync.html AmazonCloudWatchAsync]]
    */
  def cloudwatchClient[F[_]](
      prefix: Option[String] = None
  )(implicit F: Async[F]): F[AmazonCloudWatchAsync] = for {
    config <- LocalstackConfig.load[F](prefix)
    client <- cloudwatchClient(config)
  } yield client

  /** Builds a
    * [[https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/cloudwatch/AmazonCloudWatchAsync.html AmazonCloudWatchAsync]]
    * that is compliant for Localstack usage. Lifecycle is managed as a
    * [[cats.effect.Resource Resource]].
    *
    * @param config
    *   [[kinesis4cats.localstack.LocalstackConfig LocalstackConfig]]
    * @param F
    *   F with an [[cats.effect.Async Async]] instance
    * @return
    *   [[cats.effect.Resource Resource]] of
    *   [[https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/cloudwatch/AmazonCloudWatchAsync.html AmazonCloudWatchAsync]]
    */
  def cloudwatchClientResource[F[_]](config: LocalstackConfig)(implicit
      F: Async[F]
  ): Resource[F, AmazonCloudWatchAsync] =
    cloudwatchClient[F](config).toResource

  /** Builds a
    * [[https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/cloudwatch/AmazonCloudWatchAsync.html AmazonCloudWatchAsync]]
    * that is compliant for Localstack usage. Lifecycle is managed as a
    * [[cats.effect.Resource Resource]].
    *
    * @param prefix
    *   Optional prefix for parsing configuration. Default to None
    * @param F
    *   F with an [[cats.effect.Async Async]] instance
    * @return
    *   [[cats.effect.Resource Resource]] of
    *   [[https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/cloudwatch/AmazonCloudWatchAsync.html AmazonCloudWatchAsync]]
    */
  def cloudwatchClientResource[F[_]](
      prefix: Option[String] = None
  )(implicit
      F: Async[F]
  ): Resource[F, AmazonCloudWatchAsync] =
    cloudwatchClient[F](prefix).toResource

}
