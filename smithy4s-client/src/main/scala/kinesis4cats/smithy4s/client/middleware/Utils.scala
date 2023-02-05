package kinesis4cats.smithy4s.client.middleware

import cats.effect.Concurrent
import org.http4s.Message
import org.http4s.MediaType
import org.http4s.Charset
import scodec.bits.ByteVector
import cats.syntax.all._

object Utils {
  def logBody[F[_]: Concurrent](
      message: Message[F]
  ): F[String] = {
    val isBinary = message.contentType.exists(_.mediaType.binary)
    val isJson = message.contentType.exists(mT =>
      mT.mediaType == MediaType.application.json || mT.mediaType.subType
        .endsWith("+json")
    )
    val string =
      if (!isBinary || isJson)
        message
          .bodyText(implicitly, message.charset.getOrElse(Charset.`UTF-8`))
          .compile
          .string
      else
        message.body.compile.to(ByteVector).map(_.toHex)

    string
  }
}
