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

sealed trait StreamNameOrArn { self =>
  def streamName: Option[String] = self match {
    case StreamNameOrArn.Name(streamName) => Some(streamName)
    case _                                => None
  }

  def streamArn: Option[StreamArn] = self match {
    case StreamNameOrArn.Arn(streamArn) => Some(streamArn)
    case _                              => None
  }
}

object StreamNameOrArn {
  final case class Name(_streamName: String) extends StreamNameOrArn
  final case class Arn(_streamArn: StreamArn) extends StreamNameOrArn
}
