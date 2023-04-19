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

package kinesis4cats
package client

import java.util.concurrent.CompletableFuture

import cats.Show
import cats.effect.syntax.all._
import cats.effect.{Async, Resource}
import cats.syntax.all._
import org.typelevel.log4cats.StructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient
import software.amazon.awssdk.services.cloudwatch.model._

import kinesis4cats.logging.{LogContext, LogEncoder}

/** Wrapper class for the
  * [[https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/cloudwatch/CloudWatchAsyncClient.html CloudWatchAsyncClient]],
  * returning F as [[cats.effect.Async Async]] results (instead of
  * CompletableFuture)
  *
  * Unlike the [[kinesis4cats.client.KinesisClient KinesisClient]], this class
  * ONLY supports methods that are required for operations in kinesis4cats. This
  * library is not committed to providing full wrappers for DynamoDb
  *
  * @param client
  *   [[https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/dynamodb/CloudWatchAsyncClient.html CloudWatchAsyncClient]]
  * @param logger
  *   [[org.typelevel.log4cats.StructuredLogger StructuredLogger]] in use
  * @param F
  *   F with a [[cats.effect.Async Async]] instance
  * @param LE
  *   [[kinesis4cats.client.CloudWatchClient.LogEncoders CloudWatchClient.LogEncoders]]
  */
class CloudWatchClient[F[_]] private[kinesis4cats] (
    val client: CloudWatchAsyncClient,
    logger: StructuredLogger[F],
    encoders: CloudWatchClient.LogEncoders
)(implicit F: Async[F]) {

  private def requestLogs[A: LogEncoder](
      method: String,
      request: A,
      ctx: LogContext
  ): F[Unit] =
    for {
      _ <- logger.debug(ctx.context)(s"Received $method request")
      _ <- logger.trace(ctx.addEncoded("request", request).context)(
        s"Logging $method request"
      )
    } yield ()

  private def responseLogs[A: LogEncoder](
      method: String,
      response: A,
      ctx: LogContext
  ): F[Unit] = for {
    _ <- logger.debug(ctx.context)(s"Completed $method request")
    _ <- logger.trace(ctx.addEncoded("response", response).context)(
      s"Logging $method response"
    )
  } yield ()

  private def runRequest[A: LogEncoder, B: LogEncoder](
      method: String,
      request: A
  )(fn: (CloudWatchAsyncClient, A) => CompletableFuture[B]): F[B] = {
    val ctx = LogContext()
    for {
      _ <- requestLogs(method, request, ctx)
      response <- F.fromCompletableFuture(
        F.delay(fn(client, request))
      )
      _ <- responseLogs(method, response, ctx)
    } yield response
  }

  import encoders._

  def putMetricData(
      request: PutMetricDataRequest
  ): F[PutMetricDataResponse] =
    runRequest("putMetricData", request)(_.putMetricData(_))
}

object CloudWatchClient {

  /** Constructor for the CloudWatchClient, as a managed
    * [[cats.effect.Resource Resource]]
    *
    * @param client
    *   [[https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/kinesis/CloudWatchAsyncClient.html CloudWatchAsyncClient]]
    * @param F
    *   F with an [[cats.effect.Async Async]] instance
    * @param encoders
    *   [[kinesis4cats.client.CloudWatchClient.LogEncoders LogEncoders]].
    *   Defaults to show instances
    * @return
    *   [[cats.effect.Resource Resource]] containing a
    *   [[kinesis4cats.client.CloudWatchClient]]
    */
  def apply[F[_]](
      client: CloudWatchAsyncClient,
      encoders: CloudWatchClient.LogEncoders = CloudWatchClient.LogEncoders.show
  )(implicit
      F: Async[F]
  ): Resource[F, CloudWatchClient[F]] = for {
    clientResource <- Resource.fromAutoCloseable(F.pure(client))
    logger <- Slf4jLogger.create[F].toResource
  } yield new CloudWatchClient[F](clientResource, logger, encoders)

  /** Helper class containing required
    * [[kinesis4cats.logging.LogEncoder LogEncoders]] for the
    * [[kinesis4cats.client.CloudWatchClient CloudWatchClient]]
    */
  final class LogEncoders(implicit
      val putMetricDataRequestLogEncoder: LogEncoder[
        PutMetricDataRequest
      ],
      val putMetricDataResponseLogEncoder: LogEncoder[
        PutMetricDataResponse
      ]
  )

  object LogEncoders {
    val show: LogEncoders = {
      import kinesis4cats.logging.instances.show._

      implicit val dimensionShow: Show[Dimension] = x =>
        ShowBuilder("Dimension")
          .add("name", x.name())
          .add("value", x.value())
          .build

      implicit val statisticSetShow: Show[StatisticSet] = x =>
        ShowBuilder("StatisticSet")
          .add("maximum", x.maximum())
          .add("minimum", x.minimum())
          .add("sampleCount", x.sampleCount())
          .add("sum", x.sum())
          .build

      implicit val metricDatumShow: Show[MetricDatum] = x =>
        ShowBuilder("MetricDatum")
          .add("counts", x.counts())
          .add("dimensions", x.dimensions())
          .add("hasCounts", x.hasCounts())
          .add("hasDimensions", x.hasDimensions())
          .add("hasValues", x.hasValues())
          .add("metricName", x.metricName())
          .add("statisticValues", x.statisticValues())
          .add("storageResolution", x.storageResolution())
          .add("timestamp", x.timestamp())
          .add("unit", x.unitAsString())
          .add("value", x.value())
          .add("values", x.values())
          .build

      implicit val putMetricDataRequestShow: Show[PutMetricDataRequest] =
        x =>
          ShowBuilder("PutMetricDataRequest")
            .add("hasMetricData", x.hasMetricData())
            .add("metricData", x.metricData())
            .add("namespace", x.namespace())
            .build

      implicit val putMetricDataResponseShow: Show[PutMetricDataResponse] =
        _ => ShowBuilder("PutMetricDataResponse").build

      new LogEncoders()
    }
  }
}
