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

package kinesis4cats
package logging
package instances

import scala.concurrent.duration._
import scala.jdk.CollectionConverters._

import java.nio.ByteBuffer
import java.time.Instant

import cats.Show
import cats.syntax.all._
import retry.RetryDetails

import kinesis4cats.syntax.bytebuffer._

object show {
  implicit def showLogEncoder[A](implicit S: Show[A]): LogEncoder[A] =
    LogEncoder.instance(S.show)

  implicit val instantShow: Show[Instant] = Show.fromToString

  implicit def javaListShow[A: Show]: Show[java.util.List[A]] = x =>
    Show[List[A]].show(x.asScala.toList)

  implicit val javaLongShow: Show[java.lang.Long] = x => Show[Long].show(x)

  implicit val javaIntegerShow: Show[java.lang.Integer] = x => Show[Int].show(x)

  implicit val javaDurationShow: Show[java.time.Duration] = x =>
    Show[FiniteDuration].show(x.toNanos().nanos)

  implicit val javaBooleanShow: Show[java.lang.Boolean] = x =>
    Show[Boolean].show(x)

  implicit def javaMapShow[A: Show, B: Show]: Show[java.util.Map[A, B]] =
    Show[Map[A, B]].contramap(_.asScala.toMap)

  implicit val byteBufferShow: Show[ByteBuffer] = x =>
    Show[String].show(x.asBase64String)

  implicit val arrayBytesShow: Show[Array[Byte]] =
    byteBufferShow.contramap(ByteBuffer.wrap)

  implicit val retryDetailsGivingUpShow: Show[RetryDetails.GivingUp] =
    x =>
      ShowBuilder("GivingUp")
        .add("retriesSoFar", x.retriesSoFar)
        .add("cumulativeDelay", x.cumulativeDelay)
        .add("givingUp", x.givingUp)
        .add("upcomingDelay", x.upcomingDelay)
        .add("totalRetries", x.totalRetries)
        .add("totalDelay", x.totalDelay)
        .build

  implicit val retryDetailsWillDelayAndRetryShow
      : Show[RetryDetails.WillDelayAndRetry] =
    x =>
      ShowBuilder("WillDelayAndRetry")
        .add("nextDelay", x.nextDelay)
        .add("retriesSoFar", x.retriesSoFar)
        .add("cumulativeDelay", x.cumulativeDelay)
        .add("givingUp", x.givingUp)
        .add("upcomingDelay", x.upcomingDelay)
        .build

  implicit val retryDetailsShow: Show[RetryDetails] = {
    case x: RetryDetails.GivingUp          => x.show
    case x: RetryDetails.WillDelayAndRetry => x.show
  }
}
