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

package kinesis4cats.models

import scala.util.Try

import java.time.Instant

import cats.syntax.all._

/** Represents an AWS ARN for Kinesis Consumers
  *
  * @param streamArn
  *   [[kinesis4cats.models.StreamArn StreamArn]]
  * @param consumerName
  *   Name of consumer
  * @param creationTime
  *   [[https://docs.oracle.com/javase/8/docs/api/java/time/Instant.html Instant]]
  *   of when the consumer was created
  */
final case class ConsumerArn(
    streamArn: StreamArn,
    consumerName: String,
    creationTime: Instant
) {
  val consumerArn =
    s"$streamArn/consumer/$consumerName:${creationTime.getEpochSecond}"

  override def toString(): String = consumerArn
}

object ConsumerArn {

  /** Parses an AWS ARN string into a
    * [[kinesis4cats.models.ConsumerArn ConsumerARn]]
    *
    * @param consumerArn
    *   AWS ARN of the Consumer to parse
    * @return
    *   Either [[kinesis4cats.models.ConsumerArn ConsumerArn]] or a string with
    *   an error message
    */
  def fromArn(consumerArn: String): Either[String, ConsumerArn] =
    for {
      streamArn <- Try(consumerArn.split("/consumer")(0)).toEither
        .leftMap(e =>
          s"Could not get stream arn part from consumer arn: ${e.getMessage}"
        )
        .flatMap(StreamArn.fromArn)
      consumerName <- Try(consumerArn.split("/")(3)).toEither
        .leftMap(e => s"Could not get consumer name from ARN: ${e.getMessage}")
        .flatMap(x =>
          Try(x.split(":").head).toEither
            .leftMap(e =>
              s"Could not get consumer name from ARN: ${e.getMessage}"
            )
        )
      consumerParts = consumerArn.split(":")
      creationTimestamp <- Try(consumerParts.last).toEither
        .leftMap(_.getMessage)
        .flatMap(x =>
          Try(x.toLong).toEither.leftMap(e =>
            s"Could not convert timestamp to Long: ${e.getMessage}"
          )
        )
        .flatMap(x =>
          Try(Instant.ofEpochSecond(x)).toEither
            .leftMap(e =>
              s"Could not convert timestamp from ARN to Instant: ${e.getMessage}"
            )
        )
    } yield ConsumerArn(streamArn, consumerName, creationTimestamp)
}
