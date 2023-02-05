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

package kinesis4cats.smithy4s.client.middleware

import cats.effect.kernel.Async
import org.http4s.client.Client
import org.typelevel.log4cats.StructuredLogger

import kinesis4cats.smithy4s.client.KinesisClient

object RequestResponseLogger {
  def apply[F[_]](logger: StructuredLogger[F])(client: Client[F])(implicit
      F: Async[F],
      LE: KinesisClient.LogEncoders[F]
  ): Client[F] = ResponseLogger(logger)(RequestLogger(logger)(client))
}
