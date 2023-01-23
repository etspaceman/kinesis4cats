package kinesis4cats.kpl

import org.scalacheck.Prop._
import cats.effect.Resource
import cats.effect.IO

class KPLProducerSpec extends munit.ScalaCheckEffectSuite {
    
}

object KPLProducerSpec {
    def resource: Resource[IO, KPLProducer[IO]] = for {
        client <- Resource.fromAutoCloseable()
    } yield KPLProducer[IO]
}
