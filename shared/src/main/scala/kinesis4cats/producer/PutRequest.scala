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

import cats.data.NonEmptyList

import kinesis4cats.models.StreamArn

sealed trait PutRequest extends Product with Serializable {
  def record: Record
}
object PutRequest {
  final case class Name(streamName: String, record: Record) extends PutRequest
  final case class Arn(streamArn: StreamArn, record: Record) extends PutRequest
}

sealed trait PutNRequest extends Product with Serializable { self =>
  def records: NonEmptyList[Record]
  def withRecords(records: NonEmptyList[Record]): PutNRequest =
    self match {
      case x: PutNRequest.Name => x.copy(records = records)
      case x: PutNRequest.Arn  => x.copy(records = records)
    }
}
object PutNRequest {
  final case class Name(streamName: String, records: NonEmptyList[Record])
      extends PutNRequest
  final case class Arn(streamArn: StreamArn, records: NonEmptyList[Record])
      extends PutNRequest
}
