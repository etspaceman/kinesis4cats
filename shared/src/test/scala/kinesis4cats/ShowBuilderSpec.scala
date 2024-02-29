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

class ShowBuilderSpec extends munit.FunSuite {

  case class TestData(s: String, d: Double, l: Long, b: Boolean) {
    def asString(): String = ShowBuilder("TestData")
      .add("s", s)
      .add("d", d)
      .add("l", l)
      .add("b", b)
      .build
  }

  test("It should build a string properly") {
    val x = TestData("foo", 2.0, 5, b = true).asString()
    assert(
      x == "TestData(s=foo,d=2.0,l=5,b=true)" ||
        x == "TestData(s=foo,d=2,l=5,b=true)" // ScalaJS handles doubles poorly
    )
  }
}
