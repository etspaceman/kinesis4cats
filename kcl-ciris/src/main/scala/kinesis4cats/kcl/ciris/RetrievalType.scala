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

package kinesis4cats.kcl.ciris

import ciris.{ConfigDecoder, ConfigError}

/** Polling or FanOut enum, used to help determine which
  * [[https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/retrieval/RetrievalSpecificConfig.java RetrievalSpecificConfig]]
  * to load from the configuration loaders
  *
  * @param value
  *   Underlying value
  */
private[kinesis4cats] sealed abstract class RetrievalType(val value: String)

private[kinesis4cats] object RetrievalType {
  private[kinesis4cats] case object Polling extends RetrievalType("polling")
  private[kinesis4cats] case object FanOut extends RetrievalType("fanout")

  implicit val retrievalTypeConfigDecoder
      : ConfigDecoder[String, RetrievalType] = ConfigDecoder[String].mapEither {
    case (_, value) =>
      value match {
        case "polling" => Right(Polling)
        case "fanout"  => Right(FanOut)
        case x         =>
          Left(
            ConfigError(
              s"Unrecognized Retrieval Type $x. Valid values are polling and fanout"
            )
          )
      }
  }
}
