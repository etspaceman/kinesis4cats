package kinesis4cats.consumer

import java.time.Instant

sealed abstract class StartingPosition(val value: String)
    extends Product
    with Serializable { self =>
  def timestamp: Option[Instant] = None
  def sequenceNumber: Option[String] = None
}

sealed abstract class InitialPosition(override val value: String)
    extends StartingPosition(value)

object StartingPosition {
  case object Latest extends InitialPosition("LATEST")

  case object TrimHorizon extends InitialPosition("TRIM_HORIZON")

  case class AtTimestamp(getTimestamp: Instant)
      extends InitialPosition("AT_TIMESTAMP") {
    override val timestamp: Option[Instant] = Some(getTimestamp)
  }

  case class AtSequenceNumber(getSequenceNumber: String)
      extends StartingPosition("AT_SEQUENCE_NUMBER") {
    override val sequenceNumber: Option[String] = Some(getSequenceNumber)
  }

  case class AfterSequenceNumber(getSequenceNumber: String)
      extends StartingPosition("AFTER_SEQUENCE_NUMBER") {
    override val sequenceNumber: Option[String] = Some(getSequenceNumber)
  }
}
