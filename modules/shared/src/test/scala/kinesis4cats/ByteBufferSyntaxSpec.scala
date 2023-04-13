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

import java.nio.ByteBuffer
import java.util.Base64

import cats.syntax.all._
import org.scalacheck.Prop._

import kinesis4cats.syntax.bytebuffer._

class ByteBufferSyntaxSpec extends munit.ScalaCheckSuite {
  property(
    "It should output a string without editing the original ByteBuffer"
  ) {
    forAll { (s: String) =>
      val bb = ByteBuffer.wrap(s.getBytes())
      val pos1 = bb.position()
      val test = bb.asString
      val pos2 = bb.position()
      val test2 = bb.asString
      val pos3 = bb.position()

      (pos1 === pos2 && pos1 === pos3 && s === test && s === test2) :|
        s"s: $s\n" +
        s"test: $test\n" +
        s"test2: $test2\n" +
        s"pos1: $pos1\n" +
        s"pos2: $pos2\n" +
        s"pos3: $pos3\n"
    }
  }

  property(
    "It should output a base64 string without editing the original ByteBuffer"
  ) {
    forAll { (s: String) =>
      val bb = ByteBuffer.wrap(s.getBytes())
      val pos1 = bb.position()
      val test = bb.asBase64String
      val decoded = new String(Base64.getDecoder().decode(test.getBytes()))
      val pos2 = bb.position()
      val test2 = bb.asBase64String
      val decoded2 = new String(Base64.getDecoder().decode(test2.getBytes()))
      val pos3 = bb.position()

      (pos1 === pos2 && pos1 === pos3 && s === decoded && s === decoded2 && test === test2) :|
        s"s: $s\n" +
        s"test: $test\n" +
        s"test2: $test2\n" +
        s"decoded: $decoded\n" +
        s"decoded2: $decoded2\n" +
        s"pos1: $pos1\n" +
        s"pos2: $pos2\n" +
        s"pos3: $pos3\n"
    }
  }
}
