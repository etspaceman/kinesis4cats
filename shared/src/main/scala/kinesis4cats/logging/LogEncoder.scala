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

package kinesis4cats.logging

import cats.Contravariant

trait LogEncoder[A] extends Serializable { self =>
  def encode(a: A): String

  def contramap[B](f: B => A): LogEncoder[B] = new LogEncoder[B] {
    override def encode(b: B): String = self.encode(f(b))
  }
}

object LogEncoder {
  def apply[A](implicit LE: LogEncoder[A]): LogEncoder[A] = LE

  def instance[A](f: A => String): LogEncoder[A] = new LogEncoder[A] {
    override def encode(a: A): String = f(a)
  }

  implicit val encoderContravariant: Contravariant[LogEncoder] =
    new Contravariant[LogEncoder] {
      final def contramap[A, B](e: LogEncoder[A])(f: B => A): LogEncoder[B] =
        e.contramap(f)
    }
}
