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

package kinesis4cats.kcl.multistream

import cats.effect.Async
import cats.syntax.all._
import software.amazon.kinesis.common.{InitialPositionInStreamExtended, StreamConfig}

import kinesis4cats.client.KinesisClient
import kinesis4cats.models.{ConsumerArn, StreamArn}
import kinesis4cats.syntax.id._

final class MultiStreamConfig(
    instance: MultiStreamInstance,
    position: InitialPositionInStreamExtended,
    consumerArn: Option[ConsumerArn]
) {
  val streamConfig: StreamConfig =
    new StreamConfig(instance.streamIdentifier, position)
      .maybeTransform(consumerArn.map(_.consumerArn))(_.consumerArn(_))
}

object MultiStreamConfig {
  def apply[F[_]](
      client: KinesisClient[F],
      streamArn: StreamArn,
      position: InitialPositionInStreamExtended,
      consumerArn: Option[ConsumerArn] = None
  )(implicit
      F: Async[F]
  ): F[MultiStreamConfig] = MultiStreamInstance[F](client, streamArn).map(
    new MultiStreamConfig(_, position, consumerArn)
  )
}
