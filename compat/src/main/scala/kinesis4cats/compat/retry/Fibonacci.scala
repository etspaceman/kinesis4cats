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

private[kinesis4cats] object Fibonacci {
  def fibonacci(n: Int): Long =
    if (n > 0)
      fib(n)._1
    else
      0

  // "Fast doubling" Fibonacci algorithm.
  // See e.g. http://funloop.org/post/2017-04-14-computing-fibonacci-numbers.html for explanation.
  private def fib(n: Int): (Long, Long) = n match {
    case 0 => (0, 1)
    case m =>
      val (a, b) = fib(m / 2)
      val c = a * (b * 2 - a)
      val d = a * a + b * b
      if (n % 2 == 0)
        (c, d)
      else
        (d, c + d)
  }
}
