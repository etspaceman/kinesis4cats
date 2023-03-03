package kinesis4cats.smithy4s.client.logging

import org.http4s.{Request, Response}

import kinesis4cats.logging.LogEncoder

  /** Helper class containing required
    * [[kinesis4cats.logging.LogEncoder LogEncoders]] for the
    * [[kinesis4cats.smithy4s.client.DynamoClient DynamoClient]]
    */
  final class LogEncoders[F[_]](implicit
      val http4sRequestLogEncoder: LogEncoder[Request[F]],
      val http4sResponseLogEncoder: LogEncoder[Response[F]]
  )
