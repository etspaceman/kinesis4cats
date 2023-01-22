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

import cats.Show
import cats.syntax.all._

import kinesis4cats.syntax.id._

final case class ToStringBuilder private (current: String, isEmpty: Boolean) {

  private def add(key: String, value: String): ToStringBuilder = copy(
    current = current.safeTransform(value) { (curr, a) =>
      if (isEmpty) s"$curr$key=$a" else s"$curr,$key=$a"
    },
    isEmpty = false
  )

  def add[A: Show](key: String, a: A): ToStringBuilder = Option(a).fold(
    this
  )(x => add(key, x.show))

  def addTfn[A](key: String, a: A)(fn: A => String): ToStringBuilder =
    Option(a).fold(
      this
    )(x => add(key, fn(x)))

  def build = s"$current)"

}

object ToStringBuilder {
  def apply(className: String): ToStringBuilder =
    ToStringBuilder(s"$className(", true)
}
