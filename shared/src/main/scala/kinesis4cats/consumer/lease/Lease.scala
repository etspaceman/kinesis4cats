package kinesis4cats.consumer.lease

final case class Lease(
    leaseKey: String,
    leaseOwner: Option[String],
    leaseCounter: Long = 0L

)
