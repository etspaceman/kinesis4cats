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

sealed trait RecordProcessorState

object RecordProcessorState {
  case object NoState extends RecordProcessorState
  case object Initialized extends RecordProcessorState
  case object ShardEnded extends RecordProcessorState
  case object Processing extends RecordProcessorState
  case object Shutdown extends RecordProcessorState
  case object LeaseLost extends RecordProcessorState
}
