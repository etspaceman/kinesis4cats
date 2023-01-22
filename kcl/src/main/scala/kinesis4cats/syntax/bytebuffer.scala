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

import java.nio.ByteBuffer
import java.util.Base64

object bytebuffer extends ByteBufferSyntax

trait ByteBufferSyntax {
  implicit def toByteBufferOps(
      buffer: ByteBuffer
  ): ByteBufferSyntax.ByteBufferOps =
    new ByteBufferSyntax.ByteBufferOps(buffer)
}

object ByteBufferSyntax {
  final class ByteBufferOps(private val buffer: ByteBuffer) extends AnyVal {
    def asArray: Array[Byte] = {
      val position = buffer.position()
      buffer.rewind()
      val arr = new Array[Byte](buffer.remaining())
      buffer.get(arr)
      buffer.position(position)
      arr
    }
    def asString: String = new String(asArray)
    def asBase64String: String = Base64.getEncoder().encodeToString(asArray)
  }
}
