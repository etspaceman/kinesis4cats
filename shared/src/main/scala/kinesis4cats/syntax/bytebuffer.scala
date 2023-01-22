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

    /** Safely copies the contents of a [[java.nio.ByteBuffer]] to a byte array.
      * Will ensure that the ByteBuffer's current state remains intact.
      *
      * @return
      *   Byte array with the contents of the ByteBuffer
      */
    def asArray: Array[Byte] = {
      val position = buffer.position()
      buffer.rewind()
      val arr = new Array[Byte](buffer.remaining())
      buffer.get(arr)
      buffer.position(position)
      arr
    }

    /** Safely copies the content of a [[java.nio.ByteBuffer]] into a string.
      * Will ensure that the ByteBuffer's current state remains intact.
      *
      * @return
      *   String with the contents of the ByteBuffer
      */
    def asString: String = new String(asArray)

    /** Safely copies the content of a [[java.nio.ByteBuffer]] into a Base64.
      * encoded string. Will ensure that the ByteBuffer's current state remains
      * intact.
      *
      * @return
      *   Base64 string with the contents of the ByteBuffer
      */
    def asBase64String: String = Base64.getEncoder().encodeToString(asArray)
  }
}
