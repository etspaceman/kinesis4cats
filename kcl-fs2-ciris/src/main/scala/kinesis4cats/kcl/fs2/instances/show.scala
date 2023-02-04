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

package kinesis4cats.kcl.fs2.instances

import cats.Show

import kinesis4cats.ShowBuilder
import kinesis4cats.kcl.fs2.KCLConsumerFS2

object show {
  implicit val fs2ConfigShow: Show[KCLConsumerFS2.FS2Config] = x =>
    ShowBuilder("FS2Config")
      .add("queueSize", x.queueSize)
      .add("commitMaxChunk", x.commitMaxChunk)
      .add("commitMaxWait", x.commitMaxWait)
      .add("commitMaxRetries", x.commitMaxRetries)
      .add("commitRetryInterval", x.commitRetryInterval)
      .build
}
