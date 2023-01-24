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
package aws.v2

import scala.concurrent.duration._

import cats.effect.syntax.all._
import cats.effect.{Async, Resource}
import cats.syntax.all._
import retry.RetryPolicies._
import retry._
import software.amazon.awssdk.http.SdkHttpConfigurationOption
import software.amazon.awssdk.http.async.SdkAsyncHttpClient
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient
import software.amazon.awssdk.services.kinesis.model._
import software.amazon.awssdk.utils.AttributeMap

object AwsClients {
  private val trustAllCertificates =
    AttributeMap
      .builder()
      .put(
        SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES,
        java.lang.Boolean.TRUE
      )
      .build()

  def nettyClient: SdkAsyncHttpClient =
    NettyNioAsyncHttpClient
      .builder()
      .buildWithDefaults(trustAllCertificates)

  def kinesisClient[F[_]](
      config: LocalstackConfig
  )(implicit F: Async[F]): F[KinesisAsyncClient] =
    F.delay(
      KinesisAsyncClient
        .builder()
        .httpClient(nettyClient)
        .region(Region.of(config.region.name))
        .credentialsProvider(AwsCreds.LocalCreds)
        .endpointOverride(config.endpointUri)
        .build()
    )

  def kinesisClient[F[_]](
      prefix: Option[String] = None
  )(implicit F: Async[F]): F[KinesisAsyncClient] = for {
    config <- LocalstackConfig.load[F](prefix)
    client <- kinesisClient(config)
  } yield client

  def kinesisClientResource[F[_]](config: LocalstackConfig)(implicit
      F: Async[F]
  ): Resource[F, KinesisAsyncClient] =
    kinesisClient[F](config).toResource

  def kinesisClientResource[F[_]](
      prefix: Option[String] = None
  )(implicit
      F: Async[F]
  ): Resource[F, KinesisAsyncClient] =
    kinesisClient[F](prefix).toResource

  def kinesisStreamResource[F[_]](
      config: LocalstackConfig,
      streamName: String,
      shardCount: Int,
      describeRetries: Int,
      describeRetryDuration: FiniteDuration
  )(implicit
      F: Async[F]
  ): Resource[F, KinesisAsyncClient] = for {
    client <- kinesisClientResource(config)
    retryPolicy = constantDelay(describeRetryDuration).join(
      limitRetries(describeRetries)
    )
    result <- Resource.make(
      for {
        _ <- F.fromCompletableFuture(
          F.delay(
            client.createStream(
              CreateStreamRequest
                .builder()
                .streamName(streamName)
                .shardCount(shardCount)
                .build()
            )
          )
        )
        _ <- retryingOnFailuresAndAllErrors(
          retryPolicy,
          (x: DescribeStreamSummaryResponse) =>
            F.pure(
              x.streamDescriptionSummary()
                .streamStatus() == StreamStatus.ACTIVE
            ),
          noop[F, DescribeStreamSummaryResponse],
          noop[F, Throwable]
        )(
          F.fromCompletableFuture(
            F.delay(
              client.describeStreamSummary(
                DescribeStreamSummaryRequest
                  .builder()
                  .streamName(streamName)
                  .build()
              )
            )
          )
        )
      } yield client
    )(client =>
      for {
        _ <- F.fromCompletableFuture(
          F.delay(
            client.deleteStream(
              DeleteStreamRequest.builder().streamName(streamName).build()
            )
          )
        )
        _ <- retryingOnFailuresAndSomeErrors(
          retryPolicy,
          (x: Either[Throwable, DescribeStreamSummaryResponse]) =>
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
          noop[F, Either[Throwable, DescribeStreamSummaryResponse]],
          noop[F, Throwable]
        )(
          F.fromCompletableFuture(
            F.delay(
              client.describeStreamSummary(
                DescribeStreamSummaryRequest
                  .builder()
                  .streamName(streamName)
                  .build()
              )
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
  ): Resource[F, KinesisAsyncClient] = for {
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
  )(implicit F: Async[F]): F[DynamoDbAsyncClient] =
    F.delay(
      DynamoDbAsyncClient
        .builder()
        .httpClient(nettyClient)
        .region(Region.of(config.region.name))
        .credentialsProvider(AwsCreds.LocalCreds)
        .endpointOverride(config.endpointUri)
        .build()
    )

  def dynamoClient[F[_]](
      prefix: Option[String] = None
  )(implicit F: Async[F]): F[DynamoDbAsyncClient] = for {
    config <- LocalstackConfig.load[F](prefix)
    client <- dynamoClient(config)
  } yield client

  def dynamoClientResource[F[_]](config: LocalstackConfig)(implicit
      F: Async[F]
  ): Resource[F, DynamoDbAsyncClient] =
    dynamoClient[F](config).toResource

  def dynamoClientResource[F[_]](
      prefix: Option[String] = None
  )(implicit
      F: Async[F]
  ): Resource[F, DynamoDbAsyncClient] =
    dynamoClient[F](prefix).toResource

  def cloudwatchClient[F[_]](
      config: LocalstackConfig
  )(implicit F: Async[F]): F[CloudWatchAsyncClient] =
    F.delay(
      CloudWatchAsyncClient
        .builder()
        .httpClient(nettyClient)
        .region(Region.of(config.region.name))
        .credentialsProvider(AwsCreds.LocalCreds)
        .endpointOverride(config.endpointUri)
        .build()
    )

  def cloudwatchClient[F[_]](
      prefix: Option[String] = None
  )(implicit F: Async[F]): F[CloudWatchAsyncClient] = for {
    config <- LocalstackConfig.load[F](prefix)
    client <- cloudwatchClient(config)
  } yield client

  def cloudwatchClientResource[F[_]](config: LocalstackConfig)(implicit
      F: Async[F]
  ): Resource[F, CloudWatchAsyncClient] =
    cloudwatchClient[F](config).toResource

  def cloudwatchClientResource[F[_]](
      prefix: Option[String] = None
  )(implicit
      F: Async[F]
  ): Resource[F, CloudWatchAsyncClient] =
    cloudwatchClient[F](prefix).toResource

}
