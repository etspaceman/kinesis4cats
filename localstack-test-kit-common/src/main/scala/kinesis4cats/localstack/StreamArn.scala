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

import scala.util.Try

import cats.Eq
import cats.syntax.all._
import io.circe._

final case class StreamArn(
    awsRegion: AwsRegion,
    streamName: String,
    awsAccountId: String
) {
  val streamArn =
    s"arn:${awsRegion.awsArnPiece}:kinesis:${awsRegion.name}:$awsAccountId:stream/$streamName"
  override def toString: String = streamArn
}

object StreamArn {
  def fromArn(streamArn: String): Either[String, StreamArn] = {
    for {
      streamName <- Try(streamArn.split("/")(1)).toEither.leftMap(e =>
        s"Could not get stream name from ARN: ${e.getMessage}"
      )
      streamParts = streamArn.split(":")
      awsRegion <- Try(streamParts(3)).toEither
        .leftMap(_.getMessage)
        .flatMap(region =>
          Either.fromOption(
            AwsRegion.values
              .find(_.name == region),
            s"Could not get awsRegion from ARN. $region is not recognized as a valid region."
          )
        )
      awsAccountId <- Try(streamParts(4)).toEither.leftMap(e =>
        s"Could not get awsAccountId from ARN: ${e.getMessage}"
      )
    } yield StreamArn(awsRegion, streamName, awsAccountId)
  }

  implicit val streamArnCirceEncoder: Encoder[StreamArn] =
    Encoder[String].contramap(_.streamArn)
  implicit val streamArnCirceDecoder: Decoder[StreamArn] =
    Decoder[String].emap(StreamArn.fromArn)
  implicit val streamArnCirceKeyEncoder: KeyEncoder[StreamArn] =
    KeyEncoder[String].contramap(_.streamArn)
  implicit val streamArnCirceKeyDecoder: KeyDecoder[StreamArn] =
    KeyDecoder.instance(StreamArn.fromArn(_).toOption)
  implicit val streamArnEq: Eq[StreamArn] = (x, y) =>
    x.awsRegion === y.awsRegion &&
      x.streamName === y.streamName &&
      x.awsAccountId === y.awsAccountId &&
      x.streamArn === y.streamArn
  implicit val streamArnOrdering: Ordering[StreamArn] =
    (x: StreamArn, y: StreamArn) =>
      Ordering[String].compare(x.streamArn, y.streamArn)
}
