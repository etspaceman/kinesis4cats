package kinesis4cats.consumer

import cats.effect.Async

abstract class Coordinator[F[_]](implicit F: Async[F]) {
  
}
