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

package kinesis4cats.kcl.processor

import scala.concurrent.duration._

/** Configuration for the
  * [[kinesis4cats.kcl.processor.RecordProcessor RecordProcessor]]
  *
  * @param shardEndTimeout
  *   Optional timeout for the shard-end routine. It is recommended to not set a
  *   timeout, as using a timeout could lead to potential data loss.
  * @param checkpointRetries
  *   Number of retries for running a checkpoint operation.
  * @param checkpointRetryInterval
  *   Amount of time to wait between retries
  * @param autoCommit
  *   Whether the processor should automatically commit records after it is done
  *   processing
  */
final case class RecordProcessorConfig(
    shardEndTimeout: Option[FiniteDuration],
    checkpointRetries: Int,
    checkpointRetryInterval: FiniteDuration,
    autoCommit: Boolean
)

object RecordProcessorConfig {
  val default = RecordProcessorConfig(None, 5, 0.seconds, true)
}
