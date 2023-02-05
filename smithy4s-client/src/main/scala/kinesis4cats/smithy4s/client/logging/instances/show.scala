package kinesis4cats
package smithy4s.client
package logging.instances

import cats.Show
import org.http4s.Request
import org.http4s.Response

import kinesis4cats.logging.instances.show._

object show {
  implicit def requestShow[F[_]]: Show[Request[F]] = x =>
    ShowBuilder("Request")
      .add("method", x.method)
      .add("headers", x.headers)
      .add("httpVersion", x.httpVersion)
      .add("uri", x.uri)
      .build

  implicit def responseShow[F[_]]: Show[Response[F]] = x =>
    ShowBuilder("Response")
      .add("headers", x.headers)
      .add("httpVersion", x.httpVersion)
      .add("status", x.status)
      .build

  implicit def smithy4sKinesisClientLogEncoders[F[_]]
      : KinesisClient.LogEncoders[F] = new KinesisClient.LogEncoders[F]()

}
