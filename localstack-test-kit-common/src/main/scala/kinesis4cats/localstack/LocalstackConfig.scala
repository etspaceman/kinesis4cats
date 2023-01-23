package kinesis4cats.localstack

import ciris._

import kinesis4cats.ciris.CirisReader
import cats.effect.Async
import cats.effect.Resource
import cats.syntax.all._

final case class LocalstackConfig(
    servicePort: Int,
    protocol: String,
    host: String,
    region: AwsRegion
)

object LocalstackConfig {
  def read(
      prefix: Option[String] = None
  ): ConfigValue[Effect, LocalstackConfig] = for {
    servicePort <- CirisReader.readDefaulted(
      List("localstack", "service", "port"),
      4567,
      prefix
    )
    protocolDefault = if (servicePort === 4568) "http" else "https"
    protocol <- CirisReader.readDefaulted(
      List("localstack", "protocol"),
      protocolDefault
    )
    region <- CirisReader.readDefaulted[AwsRegion](
      List("aws", "region"),
      AwsRegion.US_EAST_1,
      prefix
    )
    host <- CirisReader.readDefaulted(
      List("localstack", "host"),
      "localhost",
      prefix
    )
  } yield LocalstackConfig(servicePort, protocol, host, region)

  def load[F[_]: Async](prefix: Option[String] = None): F[LocalstackConfig] =
    read(prefix).load[F]

  def resource[F[_]: Async](
      prefix: Option[String] = None
  ): Resource[F, LocalstackConfig] = read(prefix).resource[F]
}
