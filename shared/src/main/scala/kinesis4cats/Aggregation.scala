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

import cats.syntax.all._
import scodec.bits.ByteVector

private[kinesis4cats] object Aggregation {
  // From https://github.com/awslabs/amazon-kinesis-producer/blob/master/aggregation-format.md
  val magicBytes: Array[Byte] =
    Array(0xf3, 0x89, 0x9a, 0xc2).map(_.toByte)

  val magicByteVector: ByteVector = ByteVector(magicBytes)

  val digestSize = 16 // MD5 digest length

  val aggregatedByteSize: Int = magicBytes.length + digestSize
}
