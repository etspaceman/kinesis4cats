package kinesis4cats.localstack

import cats.effect.Async
import cats.syntax.all._
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient
import software.amazon.awssdk.utils.AttributeMap
import software.amazon.awssdk.http.SdkHttpConfigurationOption
import software.amazon.awssdk.http.async.SdkAsyncHttpClient
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient
import software.amazon.awssdk.regions.Region
import kinesis4cats.localstack.aws.v2.AwsCreds
import java.net.URI
import cats.effect.syntax.all._
import cats.effect.Resource
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient

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
        .endpointOverride(
          URI.create(s"${config.protocol}://localhost:${config.servicePort}")
        )
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

  def dynamoClient[F[_]](
      config: LocalstackConfig
  )(implicit F: Async[F]): F[DynamoDbAsyncClient] =
    F.delay(
      DynamoDbAsyncClient
        .builder()
        .httpClient(nettyClient)
        .region(Region.of(config.region.name))
        .credentialsProvider(AwsCreds.LocalCreds)
        .endpointOverride(
          URI.create(s"${config.protocol}://localhost:${config.servicePort}")
        )
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
        .endpointOverride(
          URI.create(s"${config.protocol}://localhost:${config.servicePort}")
        )
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
