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

package kinesis4cats.models

final case class ExtendedSequenceNumber(
    sequenceNumber: String,
    subSequenceNumber: Option[Long]
) {
  val sequenceNumberBigInt: BigInt =
    if (this == ExtendedSequenceNumber.TRIM_HORIZON) BigInt(-2)
    else if (this == ExtendedSequenceNumber.LATEST) BigInt(-1)
    else if (this == ExtendedSequenceNumber.AT_TIMESTAMP) BigInt(-3)
    else if (this == ExtendedSequenceNumber.SHARD_END) BigInt(Int.MaxValue)
    else BigInt(sequenceNumber)
}

object ExtendedSequenceNumber {
  val TRIM_HORIZON = ExtendedSequenceNumber("TRIM_HORIZON", None)
  val LATEST = ExtendedSequenceNumber("LATEST", None)
  val AT_TIMESTAMP = ExtendedSequenceNumber("AT_TIMESTAMP", None)
  val SHARD_END = ExtendedSequenceNumber("SHARD_END", None)

  implicit val extendedSequenceNumberOrdering
      : Ordering[ExtendedSequenceNumber] =
    Ordering[(String, Long)].on(x =>
      (
        x.sequenceNumber,
        x.subSequenceNumber.getOrElse(0L)
      )
    )
}
