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

package kinesis4cats.syntax

object id extends IdSyntax

trait IdSyntax {
  implicit def toIdOps[A](a: A): IdSyntax.IdOps[A] = new IdSyntax.IdOps(a)
}

object IdSyntax {
  final class IdOps[A](private val a: A) extends AnyVal {
    def safeTransform[B](b: B)(fn: (A, B) => A): A =
      Option(b).fold(a)(fn(a, _))
  }
}
