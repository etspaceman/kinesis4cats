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

import cats.Order

/** Basic wrapper for Shard ID values
  *
  * @param shardId
  *   Underlying shardId string value
  */
final case class ShardId(shardId: String)

object ShardId {
  implicit val shardIdOrder: Order[ShardId] = Order.by(_.shardId)
}
