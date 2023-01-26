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

package kinesis4cats.logging.instances

import scala.concurrent.duration.FiniteDuration
import scala.jdk.CollectionConverters._

import java.nio.ByteBuffer

import io.circe.syntax._
import io.circe.{Encoder, KeyEncoder}
import retry.RetryDetails

import kinesis4cats.logging.LogEncoder
import kinesis4cats.syntax.bytebuffer._

/** [[kinesis4cats.logging.LogEncoder LogEncoder]] instances for JSON encoding
  * of log structures using [[https://circe.github.io/circe/ Circe]]
  */
object circe {

  implicit def circeEncoderLogEncoder[A](implicit
      E: Encoder[A]
  ): LogEncoder[A] = LogEncoder.instance(a => E(a).noSpacesSortKeys)

  implicit def javaListEncoder[A: Encoder]: Encoder[java.util.List[A]] =
    Encoder[List[A]].contramap(_.asScala.toList)

  implicit val byteBufferEncoder: Encoder[ByteBuffer] =
    Encoder[String].contramap(x => x.asBase64String)

  implicit val finiteDurationEncoder: Encoder[FiniteDuration] =
    Encoder.forProduct2("length", "unit")(x => (x.length, x.unit.name))

  implicit def javaMapEncoder[A: KeyEncoder, B: Encoder]
      : Encoder[java.util.Map[A, B]] =
    Encoder[Map[A, B]].contramap(_.asScala.toMap)

  implicit val retryDetailsGivingUpEncoder: Encoder[RetryDetails.GivingUp] =
    Encoder.forProduct6(
      "retriesSoFar",
      "cumulativeDelay",
      "givingUp",
      "upcomingDelay",
      "totalRetries",
      "totalDelay"
    )(x =>
      (
        x.retriesSoFar,
        x.cumulativeDelay,
        x.givingUp,
        x.upcomingDelay,
        x.totalRetries,
        x.totalDelay
      )
    )

  implicit val retryDetailsWillDelayAndRetryEncoder
      : Encoder[RetryDetails.WillDelayAndRetry] =
    Encoder.forProduct5(
      "nextDelay",
      "retriesSoFar",
      "cumulativeDelay",
      "givingUp",
      "upcomingDelay"
    )(x =>
      (
        x.nextDelay,
        x.retriesSoFar,
        x.cumulativeDelay,
        x.givingUp,
        x.upcomingDelay
      )
    )

  implicit val retryDetailsEncoder: Encoder[RetryDetails] = {
    case x: RetryDetails.GivingUp          => x.asJson
    case x: RetryDetails.WillDelayAndRetry => x.asJson
  }
}
