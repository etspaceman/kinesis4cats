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

import java.util.concurrent.CompletableFuture

import cats.effect.syntax.all._
import cats.effect.{Async, Resource}
import cats.syntax.all._
import org.typelevel.log4cats.StructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient
import software.amazon.awssdk.services.dynamodb.model._

import kinesis4cats.logging.{LogContext, LogEncoder}

/** Wrapper class for the
  * [[https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/dynamodb/DynamoDbAsyncClient.html DynamoDbAsyncClient]],
  * returning F as [[cats.effect.Async Async]] results (instead of
  * CompletableFuture)
  *
  * Unlike the [[kinesis4cats.client.KinesisClient KinesisClient]], this class
  * ONLY supports methods that are required for operations in kinesis4cats. This
  * library is not committed to providing full wrappers for DynamoDb
  *
  * @param client
  *   [[https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/dynamodb/DynamoDbAsyncClient.html DynamoDbAsyncClient]]
  * @param logger
  *   [[org.typelevel.log4cats.StructuredLogger StructuredLogger]] in use
  * @param F
  *   F with a [[cats.effect.Async Async]] instance
  * @param LE
  *   [[kinesis4cats.client.DynamoClientLogEncoders DynamoClientLogEncoders]]
  */
class DynamoClient[F[_]] private[kinesis4cats] (
    val client: DynamoDbAsyncClient,
    logger: StructuredLogger[F]
)(implicit
    F: Async[F],
    LE: DynamoClient.LogEncoders
) {

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
  )(fn: (DynamoDbAsyncClient, A) => CompletableFuture[B]): F[B] = {
    val ctx = LogContext()
    for {
      _ <- requestLogs(method, request, ctx)
      response <- F.fromCompletableFuture(
        F.delay(fn(client, request))
      )
      _ <- responseLogs(method, response, ctx)
    } yield response
  }

  import LE._

  def createTable(
      request: CreateTableRequest
  ): F[CreateTableResponse] =
    runRequest("createTable", request)(_.createTable(_))

  def describeTable(
      request: DescribeTableRequest
  ): F[DescribeTableResponse] =
    runRequest("describeTable", request)(_.describeTable(_))

  def scan(
      request: ScanRequest
  ): F[ScanResponse] =
    runRequest("scan", request)(_.scan(_))

  def updateItem(
      request: UpdateItemRequest
  ): F[UpdateItemResponse] =
    runRequest("updateItem", request)(_.updateItem(_))

  def putItem(
      request: PutItemRequest
  ): F[PutItemResponse] =
    runRequest("putItem", request)(_.putItem(_))

  def deleteTable(
      request: DeleteTableRequest
  ): F[DeleteTableResponse] =
    runRequest("deleteTable", request)(_.deleteTable(_))

}

object DynamoClient {

  /** Constructor for the DynamoClient, as a managed
    * [[cats.effect.Resource Resource]]
    *
    * @param client
    *   [[https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/kinesis/DynamoDbAsyncClient.html DynamoDbAsyncClient]]
    * @param F
    *   F with an [[cats.effect.Async Async]] instance
    * @param LE
    *   [[kinesis4cats.client.DynamoClient.LogEncoders LogEncoders]]
    * @return
    *   [[cats.effect.Resource Resource]] containing a
    *   [[kinesis4cats.client.DynamoClient]]
    */
  def apply[F[_]](
      client: DynamoDbAsyncClient
  )(implicit
      F: Async[F],
      LE: LogEncoders
  ): Resource[F, DynamoClient[F]] = for {
    clientResource <- Resource.fromAutoCloseable(F.pure(client))
    logger <- Slf4jLogger.create[F].toResource
  } yield new DynamoClient[F](clientResource, logger)

  /** Helper class containing required
    * [[kinesis4cats.logging.LogEncoder LogEncoders]] for the
    * [[kinesis4cats.client.DynamoClient DynamoClient]]
    */
  final class LogEncoders(implicit
      val createTableRequestLogEncoder: LogEncoder[CreateTableRequest],
      val createTableResponseLogEncoder: LogEncoder[
        CreateTableResponse
      ],
      val describeTableRequestLogEncoder: LogEncoder[
        DescribeTableRequest
      ],
      val describeTableResponseLogEncoder: LogEncoder[
        DescribeTableResponse
      ],
      val scanRequestLogEncoder: LogEncoder[ScanRequest],
      val scanResponseLogEncoder: LogEncoder[ScanResponse],
      val updateItemRequestLogEncoder: LogEncoder[UpdateItemRequest],
      val updateItemResponseLogEncoder: LogEncoder[UpdateItemResponse],
      val putItemRequestLogEncoder: LogEncoder[PutItemRequest],
      val putItemResponseLogEncoder: LogEncoder[PutItemResponse],
      val deleteTableRequestLogEncoder: LogEncoder[DeleteTableRequest],
      val deleteTableResponseLogEncoder: LogEncoder[
        DeleteTableResponse
      ]
  )
}
