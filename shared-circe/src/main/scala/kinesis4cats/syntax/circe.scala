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

import io.circe.{Encoder, Json}

import kinesis4cats.syntax.id._

object circe extends CirceSyntax

trait CirceSyntax {
  implicit def toCirceMapOps(map: Map[String, Json]): CirceSyntax.CirceMapOps =
    new CirceSyntax.CirceMapOps(map)
}

object CirceSyntax {
  final class CirceMapOps(private val map: Map[String, Json]) extends AnyVal {
    def maybeAdd[A](key: String, value: A)(implicit E: Encoder[A]) =
      map.safeTransform(value) { case (m, v) => m + (key -> E(v)) }
  }
}
