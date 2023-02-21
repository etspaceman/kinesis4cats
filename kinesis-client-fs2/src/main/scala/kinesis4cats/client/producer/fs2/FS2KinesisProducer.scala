/*
 * Copyright 2023-2023 etspaceman
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kinesis4cats.client
package producer
package fs2

import cats.effect.Async
import cats.effect.kernel.Resource
import cats.effect.std.Queue
import cats.effect.syntax.all._

import kinesis4cats.producer._
import kinesis4cats.producer.fs2.FS2Producer
import software.amazon.awssdk.services.kinesis.model.PutRecordsResponse
import software.amazon.awssdk.services.kinesis.model.PutRecordsRequest
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient

/** A buffered Kinesis producer which will produce batches of data at a
  * configurable rate.
  *
  * @param config
  *   [[kinesis.producer.fs2.FS2Producer.Config FS2Producer.Config]]
  * @param queue
  *   [[cats.effect.std.Queue Queue]] of
  *   [[kinesis4cats.producer.Record Records]] to produce.
  * @param underlying
  *   [[kinesis4cats.smithy4s.client.producer.KinesisProducer KinesisProducer]]
  * @param callback:
  *   Function that can be run after each of the put results from the underlying
  * @param F
  *   [[cats.effect.Async Async]]
  * @param LE
  *   [[kinesis4cats.producer.Producer.LogEncoders Producer.LogEncoders]]
  */
final class FS2KinesisProducer[F[_]] private[kinesis4cats] (
    override val config: FS2Producer.Config,
    override protected val queue: Queue[F, Record],
    override protected val underlying: KinesisProducer[F]
)(
    override protected val callback: (
        Producer.Res[PutRecordsResponse],
        Async[F]
    ) => F[Unit]
)(implicit
    F: Async[F]
) extends FS2Producer[F, PutRecordsRequest, PutRecordsResponse]

object FS2KinesisProducer {

  /** Basic constructor for the
    * [[kinesis4cats.client.producer.KinesisProducer KinesisProducer]]
    *
    * @param config
    *   [[kinesis4cats.producer.Producer.Config Producer.Config]]
    * @param _underlying
    *   [[https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/kinesis/KinesisAsyncClient.html KinesisAsyncClient]]
    *   instance
    * @param callback:
    *   Function that can be run after each of the put results from the
    *   underlying
    * @param F
    *   [[cats.effect.Async Async]]
    * @param LE
    *   [[kinesis4cats.producer.Producer.LogEncoders Producer.LogEncoders]]
    * @param KLE
    *   [[kinesis4cats.client.KinesisClient.LogEncoders KinesisClient.LogEncoders]]
    * @param SLE
    *   [[kinesis4cats.producer.ShardMapCache.LogEncoders ShardMapCache.LogEncoders]]
    * @return
    *   [[cats.effect.Resource Resource]] of
    *   [[kinesis4cats.client.producer.KinesisProducer KinesisProducer]]
    */
  def apply[F[_]](
      config: FS2Producer.Config,
      _underlying: KinesisAsyncClient,
      callback: (Producer.Res[PutRecordsResponse], Async[F]) => F[Unit] =
        (_: Producer.Res[PutRecordsResponse], f: Async[F]) => f.unit
  )(implicit
      F: Async[F],
      LE: Producer.LogEncoders,
      KLE: KinesisClient.LogEncoders,
      SLE: ShardMapCache.LogEncoders
  ): Resource[F, FS2KinesisProducer[F]] = for {
    producer <- KinesisProducer(
      config.producerConfig,
      _underlying
    )
    queue <- Queue.bounded[F, Record](config.queueSize).toResource
  } yield new FS2KinesisProducer[F](config, queue, producer)(callback)
}
