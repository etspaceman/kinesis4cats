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

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

import org.scalacheck.Prop._

class MD5Spec extends munit.ScalaCheckSuite {

  property("It should convert to MD5 correctly") {
    forAll { (str: String) =>
      val bytes = str.getBytes(StandardCharsets.UTF_8)
      val res = MD5.compute(bytes)
      val expected = MessageDigest.getInstance("MD5").digest(bytes)

      (res.sameElements(
        expected
      )) :| s"res: ${res.mkString}\nexp: ${res.mkString}"
    }
  }
}
