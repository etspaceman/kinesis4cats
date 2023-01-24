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

import scala.concurrent.duration._

import cats.effect.syntax.all._
import cats.effect.{Async, Resource}
import cats.syntax.all._
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.cloudwatch._
import com.amazonaws.services.dynamodbv2._
import com.amazonaws.services.kinesis._
import com.amazonaws.services.kinesis.model._
import retry.RetryPolicies._
import retry._

object AwsClients {
  def kinesisClient[F[_]](
      config: LocalstackConfig
  )(implicit F: Async[F]): F[AmazonKinesisAsync] =
    F.delay(
      AmazonKinesisAsyncClientBuilder
        .standard()
        .withEndpointConfiguration(
          new EndpointConfiguration(config.endpoint, config.region.name)
        )
        .withCredentials(AwsCreds.LocalCreds)
        .build()
    )

  def kinesisClient[F[_]](
      prefix: Option[String] = None
  )(implicit F: Async[F]): F[AmazonKinesisAsync] = for {
    config <- LocalstackConfig.load[F](prefix)
    client <- kinesisClient(config)
  } yield client

  def kinesisClientResource[F[_]](config: LocalstackConfig)(implicit
      F: Async[F]
  ): Resource[F, AmazonKinesisAsync] =
    kinesisClient[F](config).toResource

  def kinesisClientResource[F[_]](
      prefix: Option[String] = None
  )(implicit
      F: Async[F]
  ): Resource[F, AmazonKinesisAsync] =
    kinesisClient[F](prefix).toResource

  def kinesisStreamResource[F[_]](
      config: LocalstackConfig,
      streamName: String,
      shardCount: Int,
      describeRetries: Int,
      describeRetryDuration: FiniteDuration
  )(implicit
      F: Async[F]
  ): Resource[F, AmazonKinesisAsync] = for {
    client <- kinesisClientResource(config)
    retryPolicy = constantDelay(describeRetryDuration).join(
      limitRetries(describeRetries)
    )
    result <- Resource.make(
      for {
        _ <- F.interruptibleMany(client.createStream(streamName, shardCount))
        _ <- retryingOnFailuresAndAllErrors(
          retryPolicy,
          (x: DescribeStreamSummaryResult) =>
            F.pure(
              x.getStreamDescriptionSummary().getStreamStatus() === "ACTIVE"
            ),
          noop[F, DescribeStreamSummaryResult],
          noop[F, Throwable]
        )(
          F.interruptibleMany(
            client.describeStreamSummary(
              new DescribeStreamSummaryRequest().withStreamName(streamName)
            )
          )
        )
      } yield client
    )(client =>
      for {
        _ <- F.interruptibleMany(client.deleteStream(streamName))
        _ <- retryingOnFailuresAndSomeErrors(
          retryPolicy,
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
              new DescribeStreamSummaryRequest().withStreamName(streamName)
            )
          ).attempt
        )
      } yield ()
    )
  } yield result

  def kinesisStreamResource[F[_]](
      streamName: String,
      shardCount: Int,
      prefix: Option[String] = None,
      describeRetries: Int = 5,
      describeRetryDuration: FiniteDuration = 500.millis
  )(implicit
      F: Async[F]
  ): Resource[F, AmazonKinesisAsync] = for {
    config <- LocalstackConfig.resource(prefix)
    result <- kinesisStreamResource(
      config,
      streamName,
      shardCount,
      describeRetries,
      describeRetryDuration
    )
  } yield result

  def dynamoClient[F[_]](
      config: LocalstackConfig
  )(implicit F: Async[F]): F[AmazonDynamoDBAsync] =
    F.delay(
      AmazonDynamoDBAsyncClientBuilder
        .standard()
        .withEndpointConfiguration(
          new EndpointConfiguration(config.endpoint, config.region.name)
        )
        .withCredentials(AwsCreds.LocalCreds)
        .build()
    )

  def dynamoClient[F[_]](
      prefix: Option[String] = None
  )(implicit F: Async[F]): F[AmazonDynamoDBAsync] = for {
    config <- LocalstackConfig.load[F](prefix)
    client <- dynamoClient(config)
  } yield client

  def dynamoClientResource[F[_]](config: LocalstackConfig)(implicit
      F: Async[F]
  ): Resource[F, AmazonDynamoDBAsync] =
    dynamoClient[F](config).toResource

  def dynamoClientResource[F[_]](
      prefix: Option[String] = None
  )(implicit
      F: Async[F]
  ): Resource[F, AmazonDynamoDBAsync] =
    dynamoClient[F](prefix).toResource

  def cloudwatchClient[F[_]](
      config: LocalstackConfig
  )(implicit F: Async[F]): F[AmazonCloudWatchAsync] =
    F.delay(
      AmazonCloudWatchAsyncClientBuilder
        .standard()
        .withEndpointConfiguration(
          new EndpointConfiguration(config.endpoint, config.region.name)
        )
        .withCredentials(AwsCreds.LocalCreds)
        .build()
    )

  def cloudwatchClient[F[_]](
      prefix: Option[String] = None
  )(implicit F: Async[F]): F[AmazonCloudWatchAsync] = for {
    config <- LocalstackConfig.load[F](prefix)
    client <- cloudwatchClient(config)
  } yield client

  def cloudwatchClientResource[F[_]](config: LocalstackConfig)(implicit
      F: Async[F]
  ): Resource[F, AmazonCloudWatchAsync] =
    cloudwatchClient[F](config).toResource

  def cloudwatchClientResource[F[_]](
      prefix: Option[String] = None
  )(implicit
      F: Async[F]
  ): Resource[F, AmazonCloudWatchAsync] =
    cloudwatchClient[F](prefix).toResource

}
