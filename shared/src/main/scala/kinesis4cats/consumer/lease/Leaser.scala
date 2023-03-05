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

package kinesis4cats.consumer.lease

abstract class Leaser[F[_]] extends Product with Serializable {

  def config: Leaser.Config

  def maybeCreateTable(): F[Unit]

  def deleteTable(): F[Unit]

  def getLeases(): F[List[Lease]]

  def createLease(lease: Lease): F[Either[Leaser.Error, Unit]]

  def releaseLease(lease: Lease): F[Either[Leaser.Error, Unit]]

  def claimLease(lease: Lease): F[Either[Leaser.Error, Unit]]

  def renewLease(lease: Lease): F[Either[Leaser.Error, Unit]]

  def updateCheckpoint(lease: Lease): F[Either[Leaser.Error, Unit]]

}

object Leaser {
  final case class Config(tableName: String)

  sealed abstract class Error(msg: String, e: Option[Throwable] = None)
      extends Exception(msg, e.orNull)
  object Error {

    final case class LeaseCollision(lease: Lease)
        extends Error(
          s"Lease with key ${lease.leaseKey} was updated before the task was complete"
        )

    final case class LeaseExists(lease: Lease)
        extends Error(s"Lease already exists for key ${lease.leaseKey}")

    final case class Unhandled(lease: Lease, e: Throwable)
        extends Error(
          s"Unhandled exception occurred during lease operation for lease key ${lease.leaseKey}",
          Some(e)
        )
  }
}
