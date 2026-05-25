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

package kinesis4cats.compat

import scala.concurrent.duration.{Duration => ScalaDuration, FiniteDuration}

import java.time.temporal.ChronoUnit
import java.time.{Duration => JavaDuration}
import java.util.concurrent.TimeUnit

/** This class contains static methods which convert between Java Durations and
  * the durations from the Scala concurrency package. This is useful when
  * mediating between Scala and Java libraries with asynchronous APIs where
  * timeouts for example are often expressed as durations.
  */
@SuppressWarnings(Array("scalafix:DisableSyntax.throw"))
private[kinesis4cats] object DurationConverters {

  /** Transform a Java duration into a Scala duration. If the nanosecond part of
    * the Java duration is zero the returned duration will have a time unit of
    * seconds and if there is a nanoseconds part the Scala duration will have a
    * time unit of nanoseconds.
    *
    * @throws IllegalArgumentException
    *   If the given Java Duration is out of bounds of what can be expressed
    *   with the Scala FiniteDuration.
    */
  final def toScala(
      duration: java.time.Duration
  ): scala.concurrent.duration.FiniteDuration = {
    val originalSeconds = duration.getSeconds
    val originalNanos = duration.getNano
    if (originalNanos == 0) {
      if (originalSeconds == 0) ScalaDuration.Zero
      else FiniteDuration(originalSeconds, TimeUnit.SECONDS)
    } else if (originalSeconds == 0) {
      FiniteDuration(originalNanos.toLong, TimeUnit.NANOSECONDS)
    } else {
      try {
        val secondsAsNanos = Math.multiplyExact(originalSeconds, 1000000000)
        val totalNanos = secondsAsNanos + originalNanos
        if (
          (totalNanos < 0 && secondsAsNanos < 0) || (totalNanos > 0 && secondsAsNanos > 0)
        ) FiniteDuration(totalNanos, TimeUnit.NANOSECONDS)
        else throw new ArithmeticException()
      } catch {
        case _: ArithmeticException =>
          throw new IllegalArgumentException(
            s"Java duration $duration cannot be expressed as a Scala duration"
          )
      }
    }
  }

  /** Transform a Scala FiniteDuration into a Java duration. Note that the Scala
    * duration keeps the time unit it was created with while a Java duration
    * always is a pair of seconds and nanos, so the unit it lost.
    */
  final def toJava(
      duration: scala.concurrent.duration.FiniteDuration
  ): java.time.Duration =
    if (duration.length == 0) JavaDuration.ZERO
    else
      duration.unit match {
        case TimeUnit.NANOSECONDS  => JavaDuration.ofNanos(duration.length)
        case TimeUnit.MICROSECONDS =>
          JavaDuration.of(duration.length, ChronoUnit.MICROS)
        case TimeUnit.MILLISECONDS => JavaDuration.ofMillis(duration.length)
        case TimeUnit.SECONDS      => JavaDuration.ofSeconds(duration.length)
        case TimeUnit.MINUTES      => JavaDuration.ofMinutes(duration.length)
        case TimeUnit.HOURS        => JavaDuration.ofHours(duration.length)
        case TimeUnit.DAYS         => JavaDuration.ofDays(duration.length)
      }

  implicit final class DurationOps(val duration: java.time.Duration)
      extends AnyVal {

    /** See [[DurationConverters#toScala]]
      */
    def toScala: scala.concurrent.duration.FiniteDuration =
      DurationConverters.toScala(duration)
  }

  implicit final class FiniteDurationops(
      val duration: scala.concurrent.duration.FiniteDuration
  ) extends AnyVal {

    /** See [[DurationConverters#toJava]]
      */
    def toJava: java.time.Duration = DurationConverters.toJava(duration)
  }

}
