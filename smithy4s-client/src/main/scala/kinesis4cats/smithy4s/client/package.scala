package kinesis4cats.smithy4s

import smithy4s.aws.AwsClient
import com.amazonaws.kinesis.KinesisGen

package object client {
  type KinesisClient[F[_]] = AwsClient[KinesisGen, F]
}
