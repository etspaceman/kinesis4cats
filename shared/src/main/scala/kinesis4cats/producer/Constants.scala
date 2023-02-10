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

package kinesis4cats.producer

object Constants {
  val MaxChunkSize: Int = 1024 // Stream-internal max chunk size
  val MaxRecordsPerRequest = 500 // This is a Kinesis API limitation
  val MaxPayloadSizePerRequest = 5 * 1024 * 1024 // 5 MB
  val MaxPayloadSizePerRecord =
    1 * 1024 * 921 // 1 MB TODO actually 90%, to avoid underestimating and getting Kinesis errors
  val MaxIngestionPerShardPerSecond = 1 * 1024 * 1024 // 1 MB
  val MaxRecordsPerShardPerSecond = 1000
}
