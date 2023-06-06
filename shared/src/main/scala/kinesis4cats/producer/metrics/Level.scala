package kinesis4cats.producer.metrics

sealed abstract class Level(val value: String) {
  def isDetailed: Boolean
  def isSummary: Boolean
}

object Level {
  case object None extends Level("NONE") {
    override def isDetailed: Boolean = false
    override def isSummary: Boolean = false
  }
  case object Summary extends Level("SUMMARY") {
    override def isDetailed: Boolean = false
    override def isSummary: Boolean = true
  }
  case object Detailed extends Level("DETAILED") {
    override def isDetailed: Boolean = true
    override def isSummary: Boolean = true
  }
}
