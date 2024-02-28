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

/** Helper builder class for constructing [[cats.Show Show]] string methods
  * while considering null safety.
  *
  * @param current
  *   Current string
  * @param isEmpty
  *   If no values have been added to the String
  */
private[kinesis4cats] final case class ShowBuilder private (
    current: String,
    isEmpty: Boolean
) {

  private def add(key: String, value: String): ShowBuilder = copy(
    current = current.safeTransform(value) { (curr, a) =>
      if (isEmpty) s"$curr$key=$a" else s"$curr,$key=$a"
    },
    isEmpty = false
  )

  /** Add a value to the [[cats.Show Show]] string
    *
    * @param key
    *   Key to represent the value
    * @param a
    *   Value to add. Must have a [[cats.Show Show]] instance for its type
    * @return
    *   [[kinesis4cats.ShowBuilder ShowBuilder]]
    */
  def add[A: Show](key: String, a: A): ShowBuilder = Option(a).fold(
    this
  )(x => add(key, x.show))

  /** Returns a finished string
    *
    * @return
    *   Completed string for the [[cats.Show Show]] instance
    */
  def build = s"$current)"

}

private[kinesis4cats] object ShowBuilder {

  /** Constructor for [[kinesis4cats.ShowBuilder]]
    *
    * @param className
    *   Name of the class being represented
    * @return
    *   [[kinesis4cats.ShowBuilder ShowBuilder]]
    */
  def apply(className: String): ShowBuilder =
    ShowBuilder(s"$className(", isEmpty = true)
}
