package kinesis4cats.consumer.lease

import cats.effect.Async

abstract class Coordinator[F[_]](implicit F: Async[F]) {
  
}
