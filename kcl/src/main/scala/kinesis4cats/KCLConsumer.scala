package kinesis4cats

import cats.effect.kernel.Resource

trait KCLConsumer[F[_]] {
  def run(): Resource[F, Unit]
}
