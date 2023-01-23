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

/** A typeclass that encodes values as strings for log messages.
  */
trait LogEncoder[A] extends Serializable { self =>

  /** Encode a value of type A as a String
    *
    * @param a
    *   Value to encode
    * @return
    *   String for log message
    */
  def encode(a: A): String

  /** Constructs a [[kinesis4cats.logging.LogEncoder LogEncoder]] for a new type
    * (B) given a function from A => B.
    *
    * @param f
    * @return
    */
  def contramap[B](f: B => A): LogEncoder[B] = new LogEncoder[B] {
    override def encode(b: B): String = self.encode(f(b))
  }
}

object LogEncoder {

  /** Summoning method */
  def apply[A](implicit LE: LogEncoder[A]): LogEncoder[A] = LE

  /** Instance creator for [[kinesis4cats.logging.LogEncoder]]
    *
    * @param f
    *   A => String
    * @return
    *   [[kinesis4cats.logging.LogEncoder]]
    */
  def instance[A](f: A => String): LogEncoder[A] = new LogEncoder[A] {
    override def encode(a: A): String = f(a)
  }

  implicit val encoderContravariant: Contravariant[LogEncoder] =
    new Contravariant[LogEncoder] {
      final def contramap[A, B](e: LogEncoder[A])(f: B => A): LogEncoder[B] =
        e.contramap(f)
    }
}
