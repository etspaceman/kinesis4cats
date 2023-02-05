package kinesis4cats.smithy4s.client
package localstack

import org.http4s.client.Client
import smithy4s.aws.kernel.AwsRegion
import cats.effect.Async
import kinesis4cats.smithy4s.client.KinesisClient
import cats.effect.kernel.Resource
import cats.effect.syntax.all._
import org.typelevel.log4cats.slf4j.Slf4jLogger
import smithy4s.aws.http4s._
import com.amazonaws.kinesis._
import kinesis4cats.smithy4s.client.middleware.RequestResponseLogger
import kinesis4cats.smithy4s.client.middleware.LocalstackProxy
import kinesis4cats.localstack.LocalstackConfig
import smithy4s.aws._
import cats.syntax.all._

object LocalstackKinesisClient {
  private def localstackEnv[F[_]](client: Client[F], region: AwsRegion)(implicit
      F: Async[F]
  ) = AwsEnvironment.make[F](
    AwsHttp4sBackend(client),
    F.pure(region),
    F.pure(AwsCredentials.Default("mock-key-id", "mock-secret-key", None)),
    F.realTime.map(_.toSeconds).map(Timestamp(_, 0))
  )

  def clientResource[F[_]](
      client: Client[F],
      region: AwsRegion,
      config: LocalstackConfig
  )(implicit
      F: Async[F],
      LE: KinesisClient.LogEncoders[F]
  ): Resource[F, KinesisClient[F]] = for {
    logger <- Slf4jLogger.create[F].toResource
    clnt = RequestResponseLogger(logger)(
      LocalstackProxy(config)(RequestResponseLogger(logger)(client))
    )
    env = localstackEnv(clnt, region)
    awsClient <- AwsClient(Kinesis.service, env)
  } yield awsClient

  def clientResource[F[_]](
      client: Client[F],
      region: AwsRegion,
      prefix: Option[String] = None
  )(implicit
      F: Async[F],
      LE: KinesisClient.LogEncoders[F]
  ): Resource[F, KinesisClient[F]] = for {
    logger <- Slf4jLogger.create[F].toResource
    clnt = RequestResponseLogger(logger)(
      LocalstackProxy(prefix)(RequestResponseLogger(logger)(client))
    )
    env = localstackEnv(clnt, region)
    awsClient <- AwsClient(Kinesis.service, env)
  } yield awsClient
}
