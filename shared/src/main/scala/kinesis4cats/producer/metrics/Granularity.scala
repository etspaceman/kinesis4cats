package kinesis4cats.producer.metrics

sealed abstract class Granularity(val value: String)

object Granularity {
  case object Global extends Granularity("GLOBAL")
  case object Stream extends Granularity("STREAM")
  case object Shard extends Granularity("SHARD") 
}
