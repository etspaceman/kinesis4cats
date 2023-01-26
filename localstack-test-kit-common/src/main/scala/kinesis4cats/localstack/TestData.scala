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

package kinesis4cats.localstack

import io.circe.{Decoder, Encoder}
import org.scalacheck.Arbitrary

final case class TestData(
    string: String,
    float: Float,
    double: Double,
    boolean: Boolean,
    int: Int,
    long: Long
)

object TestData {
  implicit val testDataEncoder: Encoder[TestData] =
    Encoder.forProduct6("string", "float", "double", "boolean", "int", "long") {
      x =>
        (x.string, x.float, x.double, x.boolean, x.int, x.long)
    }

  implicit val testDataDecoder: Decoder[TestData] = x =>
    for {
      string <- x.downField("string").as[String]
      float <- x.downField("float").as[Float]
      double <- x.downField("double").as[Double]
      boolean <- x.downField("boolean").as[Boolean]
      int <- x.downField("int").as[Int]
      long <- x.downField("long").as[Long]
    } yield TestData(string, float, double, boolean, int, long)

  implicit val testDataArb: Arbitrary[TestData] = Arbitrary(
    for {
      string <- Arbitrary.arbitrary[String]
      float <- Arbitrary.arbitrary[Float]
      double <- Arbitrary.arbitrary[Double]
      boolean <- Arbitrary.arbitrary[Boolean]
      int <- Arbitrary.arbitrary[Int]
      long <- Arbitrary.arbitrary[Long]
    } yield TestData(string, float, double, boolean, int, long)
  )
}
