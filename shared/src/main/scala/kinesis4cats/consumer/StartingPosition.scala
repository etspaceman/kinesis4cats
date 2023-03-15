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

package kinesis4cats.consumer

import java.time.Instant

sealed abstract class StartingPosition(val value: String)
    extends Product
    with Serializable { self =>
  def timestamp: Option[Instant] = None
  def sequenceNumber: Option[String] = None
}

sealed abstract class InitialPosition(override val value: String)
    extends StartingPosition(value)

object StartingPosition {
  case object Latest extends InitialPosition("LATEST")

  case object TrimHorizon extends InitialPosition("TRIM_HORIZON")

  case object ShardEnd extends StartingPosition("SHARD_END")

  case class AtTimestamp(getTimestamp: Instant)
      extends InitialPosition("AT_TIMESTAMP") {
    override val timestamp: Option[Instant] = Some(getTimestamp)
  }

  case class AtSequenceNumber(getSequenceNumber: String)
      extends StartingPosition("AT_SEQUENCE_NUMBER") {
    override val sequenceNumber: Option[String] = Some(getSequenceNumber)
  }

  case class AfterSequenceNumber(getSequenceNumber: String)
      extends StartingPosition("AFTER_SEQUENCE_NUMBER") {
    override val sequenceNumber: Option[String] = Some(getSequenceNumber)
  }
}
