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

package kinesis4cats.compat.retry

import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.util.Random

import java.util.concurrent.TimeUnit

import cats.Applicative
import cats.syntax.functor._
import cats.syntax.show._

import kinesis4cats.compat.retry.PolicyDecision._

object RetryPolicies {
  private val LongMax: BigInt = BigInt(Long.MaxValue)

  /*
   * Multiply the given duration by the given multiplier, but cap the result to
   * ensure we don't try to create a FiniteDuration longer than 2^63 - 1 nanoseconds.
   *
   * Note: despite the "safe" in the name, we can still create an invalid
   * FiniteDuration if the multiplier is negative. But an assumption of the library
   * as a whole is that nobody would be silly enough to use negative delays.
   */
  private def safeMultiply(
      duration: FiniteDuration,
      multiplier: Long
  ): FiniteDuration = {
    val durationNanos = BigInt(duration.toNanos)
    val resultNanos = durationNanos * BigInt(multiplier)
    val safeResultNanos = resultNanos min LongMax
    FiniteDuration(safeResultNanos.toLong, TimeUnit.NANOSECONDS)
  }

  /** Don't retry at all and always give up. Only really useful for combining
    * with other policies.
    */
  def alwaysGiveUp[M[_]: Applicative]: RetryPolicy[M] =
    RetryPolicy.liftWithShow(
      Function.const(PolicyDecision.GiveUp),
      "alwaysGiveUp"
    )

  /** Delay by a constant amount before each retry. Never give up.
    */
  def constantDelay[M[_]: Applicative](delay: FiniteDuration): RetryPolicy[M] =
    RetryPolicy.liftWithShow(
      Function.const(DelayAndRetry(delay)),
      show"constantDelay($delay)"
    )

  /** Each delay is twice as long as the previous one. Never give up.
    */
  def exponentialBackoff[M[_]: Applicative](
      baseDelay: FiniteDuration
  ): RetryPolicy[M] =
    RetryPolicy.liftWithShow(
      { status =>
        val delay =
          safeMultiply(
            baseDelay,
            Math.pow(2.0, status.retriesSoFar.toDouble).toLong
          )
        DelayAndRetry(delay)
      },
      show"exponentialBackOff(baseDelay=$baseDelay)"
    )

  /** Retry without delay, giving up after the given number of retries.
    */
  def limitRetries[M[_]: Applicative](maxRetries: Int): RetryPolicy[M] =
    RetryPolicy.liftWithShow(
      status =>
        if (status.retriesSoFar >= maxRetries) {
          GiveUp
        } else {
          DelayAndRetry(Duration.Zero)
        },
      show"limitRetries(maxRetries=$maxRetries)"
    )

  /** Delay(n) = Delay(n - 2) + Delay(n - 1)
    *
    * e.g. if `baseDelay` is 10 milliseconds, the delays before each retry will
    * be 10 ms, 10 ms, 20 ms, 30ms, 50ms, 80ms, 130ms, ...
    */
  def fibonacciBackoff[M[_]: Applicative](
      baseDelay: FiniteDuration
  ): RetryPolicy[M] =
    RetryPolicy.liftWithShow(
      { status =>
        val delay =
          safeMultiply(baseDelay, Fibonacci.fibonacci(status.retriesSoFar + 1))
        DelayAndRetry(delay)
      },
      show"fibonacciBackoff(baseDelay=$baseDelay)"
    )

  /** "Full jitter" backoff algorithm. See
    * https://aws.amazon.com/blogs/architecture/exponential-backoff-and-jitter/
    */
  def fullJitter[M[_]: Applicative](baseDelay: FiniteDuration): RetryPolicy[M] =
    RetryPolicy.liftWithShow(
      { status =>
        val e = Math.pow(2.0, status.retriesSoFar.toDouble).toLong
        val maxDelay = safeMultiply(baseDelay, e)
        val delayNanos = (maxDelay.toNanos * Random.nextDouble()).toLong
        DelayAndRetry(new FiniteDuration(delayNanos, TimeUnit.NANOSECONDS))
      },
      show"fullJitter(baseDelay=$baseDelay)"
    )

  /** Set an upper bound on any individual delay produced by the given policy.
    */
  def capDelay[M[_]: Applicative](
      cap: FiniteDuration,
      policy: RetryPolicy[M]
  ): RetryPolicy[M] =
    policy.meet(constantDelay(cap))

  /** Add an upper bound to a policy such that once the given time-delay amount
    * <b>per try</b> has been reached or exceeded, the policy will stop retrying
    * and give up. If you need to stop retrying once <b>cumulative</b> delay
    * reaches a time-delay amount, use [[limitRetriesByCumulativeDelay]].
    */
  def limitRetriesByDelay[M[_]: Applicative](
      threshold: FiniteDuration,
      policy: RetryPolicy[M]
  ): RetryPolicy[M] = {
    def decideNextRetry(status: RetryStatus): M[PolicyDecision] =
      policy.decideNextRetry(status).map {
        case r @ DelayAndRetry(delay) =>
          if (delay > threshold) GiveUp else r
        case GiveUp => GiveUp
      }

    RetryPolicy.withShow[M](
      decideNextRetry,
      show"limitRetriesByDelay(threshold=$threshold, $policy)"
    )
  }

  /** Add an upperbound to a policy such that once the cumulative delay over all
    * retries has reached or exceeded the given limit, the policy will stop
    * retrying and give up.
    */
  def limitRetriesByCumulativeDelay[M[_]: Applicative](
      threshold: FiniteDuration,
      policy: RetryPolicy[M]
  ): RetryPolicy[M] = {
    def decideNextRetry(status: RetryStatus): M[PolicyDecision] =
      policy.decideNextRetry(status).map {
        case r @ DelayAndRetry(delay) =>
          if (status.cumulativeDelay + delay >= threshold) GiveUp else r
        case GiveUp => GiveUp
      }

    RetryPolicy.withShow[M](
      decideNextRetry,
      show"limitRetriesByCumulativeDelay(threshold=$threshold, $policy)"
    )
  }
}
