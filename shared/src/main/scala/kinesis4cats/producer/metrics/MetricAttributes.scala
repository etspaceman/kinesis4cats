/*
 * Copyright 2023-2026 etspaceman
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
package metrics

import org.typelevel.otel4s.Attribute

import kinesis4cats.models.ShardId
import kinesis4cats.models.StreamNameOrArn

/** Single source of truth for producer metric attribute keys, shared by all
  * metric bundles in this package.
  */
private[metrics] object MetricAttributes {

  def streamValue(stream: StreamNameOrArn): String =
    stream.streamName
      .orElse(stream.streamArn.map(_.streamArn))
      .getOrElse("")

  def streamAttrs(stream: StreamNameOrArn): List[Attribute[_]] =
    List(Attribute("stream.name", streamValue(stream)))

  def shardAttrs(
      stream: StreamNameOrArn,
      shard: ShardId
  ): List[Attribute[_]] =
    List(
      Attribute("stream.name", streamValue(stream)),
      Attribute("shard.id", shard.shardId)
    )

  def errorAttrs(
      stream: StreamNameOrArn,
      code: String
  ): List[Attribute[_]] =
    List(
      Attribute("stream.name", streamValue(stream)),
      Attribute("error.code", code)
    )

  def droppedAttrs(
      stream: StreamNameOrArn,
      reason: String
  ): List[Attribute[_]] =
    List(
      Attribute("stream.name", streamValue(stream)),
      Attribute("reason", reason)
    )
}
