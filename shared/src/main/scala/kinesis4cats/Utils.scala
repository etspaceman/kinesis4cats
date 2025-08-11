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

import java.util.UUID

import cats.effect.Sync
import cats.effect.SyncIO
import cats.effect.std.SecureRandom
import cats.effect.std.UUIDGen
import cats.syntax.all._

private[kinesis4cats] object Utils {
  private def getUUIDGenSync: SyncIO[UUIDGen[SyncIO]] = SecureRandom
    .javaSecuritySecureRandom[SyncIO]
    .map(x => UUIDGen.fromSecureRandom[SyncIO](implicitly, x))

  private def randomUUIDSyncIO: SyncIO[UUID] =
    getUUIDGenSync.flatMap(x => x.randomUUID)

  private def uuidGen[F[_]](implicit F: Sync[F]): F[UUIDGen[F]] =
    SecureRandom
      .javaSecuritySecureRandom[F]
      .map(x => UUIDGen.fromSecureRandom[F](implicitly, x))

  def randomUUID =
    randomUUIDSyncIO.unsafeRunSync()

  def randomUUIDString = randomUUIDSyncIO.map(_.toString).unsafeRunSync()

  private[kinesis4cats] def randomUUIDStringSafe[F[_]](implicit
      F: Sync[F]
  ): F[String] =
    uuidGen.flatMap(gen => gen.randomUUID).map(_.toString())

  def md5(bytes: Array[Byte]): Array[Byte] = MD5.compute(bytes)
}
