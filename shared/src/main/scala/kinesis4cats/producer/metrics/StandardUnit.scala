package kinesis4cats.producer.metrics

sealed abstract class StandardUnit(val value: String)

object StandardUnit {
  case object Count extends StandardUnit("Count")
  case object Bytes extends StandardUnit("Bytes")
  case object Milliseconds extends StandardUnit("Milliseconds")
}
