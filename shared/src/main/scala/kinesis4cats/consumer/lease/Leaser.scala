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
