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

import cats.effect.SyncIO
import cats.effect.std.UUIDGen
import fs2.Stream

object Utils {
  def randomUUID = UUIDGen.randomUUID[SyncIO].unsafeRunSync()
  def randomUUIDString = UUIDGen.randomString[SyncIO].unsafeRunSync()
  def md5(bytes: Array[Byte]): Array[Byte] =
    Stream
      .emits[SyncIO, Byte](bytes.toList)
      .through(fs2.hash.md5[SyncIO])
      .compile
      .to(Array)
      .unsafeRunSync()
}
