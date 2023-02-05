package kinesis4cats.smithy4s.client
package logging.instances

import io.circe.Encoder
import org.http4s.Request
import org.http4s.circe._
import org.http4s.Headers
import org.http4s.HttpVersion
import org.http4s.Method
import org.http4s.Response
import org.http4s.Status
import kinesis4cats.logging.instances.circe._

object circe {
  implicit val headersCirceEncoder: Encoder[Headers] =
    Encoder[List[(String, String)]]
      .contramap(_.headers.map(x => x.name.toString -> x.value))

  implicit val httpVersionEncoder: Encoder[HttpVersion] =
    Encoder.forProduct2("major", "minor")(x => (x.major, x.minor))

  implicit val methodEncoder: Encoder[Method] =
    Encoder[String].contramap(_.name)

  implicit val statusEncoder: Encoder[Status] =
    Encoder.forProduct2("code", "reason")(x => (x.code, x.reason))

  implicit def requestCirceEncoder[F[_]]: Encoder[Request[F]] =
    Encoder.forProduct4("headers", "httpVersion", "method", "uri")(x =>
      (x.headers, x.httpVersion, x.method, x.uri)
    )

  implicit def responseCirceEncoder[F[_]]: Encoder[Response[F]] =
    Encoder.forProduct3("headers", "httpVersion", "status")(x =>
      (x.headers, x.httpVersion, x.status)
    )

  implicit def smithy4sKinesisClientLogEncoders[F[_]]
      : KinesisClient.LogEncoders[F] = new KinesisClient.LogEncoders[F]()
}
