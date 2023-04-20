package kinesis4cats.localstack

import scala.concurrent.duration._

import cats.Applicative

import kinesis4cats.compat.retry.RetryPolicies._
import kinesis4cats.compat.retry.RetryPolicy

final case class TestStreamConfig[F[_]](
    streamName: String,
    shardCount: Int,
    describeRetryPolicy: RetryPolicy[F]
)

object TestStreamConfig {
  def default[F[_]](streamName: String, shardCount: Int)(implicit
      F: Applicative[F]
  ): TestStreamConfig[F] = TestStreamConfig[F](
    streamName,
    shardCount,
    constantDelay(500.millis).join(limitRetries(5))
  )
}
