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

import scala.jdk.CollectionConverters._

import java.nio.ByteBuffer
import java.util.concurrent.CompletableFuture

import cats.Eval
import cats.Show
import cats.effect.{Async, Resource}
import cats.syntax.all._
import org.typelevel.log4cats.StructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import software.amazon.awssdk.core.SdkBytes
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
  * @param encoders
  *   [[kinesis4cats.client.DynamoClient.LogEncoders DynamoClient.LogEncoders]]
  */
class DynamoClient[F[_]] private[kinesis4cats] (
    val client: DynamoDbAsyncClient,
    logger: StructuredLogger[F],
    encoders: DynamoClient.LogEncoders
)(implicit
    F: Async[F]
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
  )(fn: (DynamoDbAsyncClient, A) => CompletableFuture[B]): F[B] =
    for {
      ctx <- LogContext.safe[F]
      _ <- requestLogs(method, request, ctx)
      response <- F.fromCompletableFuture(
        F.delay(fn(client, request))
      )
      _ <- responseLogs(method, response, ctx)
    } yield response

  import encoders._

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

  final case class Builder[F[_]] private (
      clientResource: Resource[F, DynamoDbAsyncClient],
      encoders: LogEncoders,
      logger: StructuredLogger[F]
  )(implicit F: Async[F]) {
    def withClient(
        client: => DynamoDbAsyncClient,
        managed: Boolean = true
    ): Builder[F] = copy(
      clientResource =
        if (managed) Resource.fromAutoCloseable(F.delay(client))
        else Resource.pure(client)
    )
    def withLogEncoders(encoders: LogEncoders): Builder[F] =
      copy(encoders = encoders)
    def withLogger(logger: StructuredLogger[F]): Builder[F] =
      copy(logger = logger)

    def build: Resource[F, DynamoClient[F]] =
      clientResource.map(new DynamoClient[F](_, logger, encoders))
  }

  object Builder {
    def default[F[_]](implicit F: Async[F]): Builder[F] = Builder[F](
      Resource.fromAutoCloseable(
        F.delay(DynamoDbAsyncClient.builder().build())
      ),
      LogEncoders.show,
      Slf4jLogger.getLogger
    )

    @annotation.unused
    private def unapply[F[_]](builder: Builder[F]): Unit = ()
  }

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

  object LogEncoders {
    val show: LogEncoders = {
      import kinesis4cats.logging.instances.show._

      implicit val attributeDefinitionShow: Show[AttributeDefinition] = x =>
        ShowBuilder("AttributeDefinition")
          .add("attributeName", x.attributeName())
          .add("attributeType", x.attributeTypeAsString())
          .build

      implicit val keySchemaElementShow: Show[KeySchemaElement] = x =>
        ShowBuilder("KeySchemaElement")
          .add("attributeName", x.attributeName())
          .add("keyType", x.keyTypeAsString())
          .build

      implicit val projectionShow: Show[Projection] = x =>
        ShowBuilder("Projection")
          .add("hasNonKeyAttributes", x.hasNonKeyAttributes())
          .add("nonKeyAttributes", x.nonKeyAttributes())
          .add("projectionType", x.projectionTypeAsString())
          .build

      implicit val provisionedThroughputShow: Show[ProvisionedThroughput] =
        x =>
          ShowBuilder("ProvisionedThroughput")
            .add("readCapacityUnits", x.readCapacityUnits())
            .add("writeCapacityUnits", x.writeCapacityUnits())
            .build

      implicit val globalSecondaryIndexShow: Show[GlobalSecondaryIndex] =
        x =>
          ShowBuilder("GlobalSecondaryIndex")
            .add("hasKeySchema", x.hasKeySchema())
            .add("indexName", x.indexName())
            .add("keySchema", x.keySchema())
            .add("projection", x.projection())
            .add("provisionedThroughput", x.provisionedThroughput())
            .build

      implicit val localSecondaryIndexShow: Show[LocalSecondaryIndex] = x =>
        ShowBuilder("LocalSecondaryIndex")
          .add("hasKeySchema", x.hasKeySchema())
          .add("indexName", x.indexName())
          .add("keySchema", x.keySchema())
          .add("projection", x.projection())
          .build

      implicit val sseSpecificationShow: Show[SSESpecification] = x =>
        ShowBuilder("SSESpecification")
          .add("enabled", x.enabled())
          .add("kmsMasterKeyId", x.kmsMasterKeyId())
          .add("sseType", x.sseTypeAsString())
          .build

      implicit val streamSpecificationShow: Show[StreamSpecification] = x =>
        ShowBuilder("StreamSpecification")
          .add("streamEnabled", x.streamEnabled())
          .add("streamViewType", x.streamViewTypeAsString())
          .build

      implicit val ddbTagShow: Show[Tag] = x =>
        ShowBuilder("Tag")
          .add("key", x.key())
          .add("value", x.value())
          .build

      implicit val archivalSummaryShow: Show[ArchivalSummary] = x =>
        ShowBuilder("ArchivalSummary")
          .add("archivalBackupArn", x.archivalBackupArn())
          .add("archivalDateTime", x.archivalDateTime())
          .add("archivalReason", x.archivalReason())
          .build

      implicit val billingModeSummaryShow: Show[BillingModeSummary] = x =>
        ShowBuilder("BillingModeSummary")
          .add("billingMode", x.billingModeAsString())
          .add(
            "lastUpdateToPayPerRequestDateTime",
            x.lastUpdateToPayPerRequestDateTime()
          )
          .build

      implicit val provisionedThroughputDescriptionShow
          : Show[ProvisionedThroughputDescription] = x =>
        ShowBuilder("ProvisionedThroughputDescription")
          .add("lastDecreaseDateTime", x.lastDecreaseDateTime())
          .add("lastIncreaseDateTime", x.lastIncreaseDateTime())
          .add("numberOfDecreasesToday", x.numberOfDecreasesToday())
          .add("readCapacityUnits", x.readCapacityUnits())
          .add("writeCapacityUnits", x.writeCapacityUnits())
          .build

      implicit val globalSecondaryIndexDescriptionShow
          : Show[GlobalSecondaryIndexDescription] = x =>
        ShowBuilder("GlobalSecondaryIndexDescription")
          .add("backfilling", x.backfilling())
          .add("hasKeySchema", x.hasKeySchema())
          .add("indexArn", x.indexArn())
          .add("indexName", x.indexName())
          .add("indexSizeBytes", x.indexSizeBytes())
          .add("indexStatus", x.indexStatusAsString())
          .add("itemCount", x.itemCount())
          .add("keySchema", x.keySchema())
          .add("projection", x.projection())
          .add("provisionedThroughput", x.provisionedThroughput())
          .build

      implicit val localSecondaryIndexDescriptionShow
          : Show[LocalSecondaryIndexDescription] = x =>
        ShowBuilder("LocalSecondaryIndexDescription")
          .add("hasKeySchema", x.hasKeySchema())
          .add("indexArn", x.indexArn())
          .add("indexName", x.indexName())
          .add("indexSizeBytes", x.indexSizeBytes())
          .add("itemCount", x.itemCount())
          .add("keySchema", x.keySchema())
          .add("projection", x.projection())
          .build

      implicit val provisionedThroughputOverrideShow
          : Show[ProvisionedThroughputOverride] = x =>
        ShowBuilder("ProvisionedThroughputOverride")
          .add("readCapacityUnits", x.readCapacityUnits())
          .build

      implicit val replicaGlobalSecondaryIndexDescriptionShow
          : Show[ReplicaGlobalSecondaryIndexDescription] = x =>
        ShowBuilder("ReplicaGlobalSecondaryIndexDescription")
          .add("indexName", x.indexName())
          .add(
            "provisionedThroughputOverride",
            x.provisionedThroughputOverride()
          )
          .build

      implicit val tableClassSummaryShow: Show[TableClassSummary] = x =>
        ShowBuilder("TableClassSummary")
          .add("lastUpdateDateTime", x.lastUpdateDateTime())
          .add("tableClass", x.tableClassAsString())
          .build

      implicit val replicaDescriptionShow: Show[ReplicaDescription] = x =>
        ShowBuilder("ReplicaDescription")
          .add("globalSecondaryIndexes", x.globalSecondaryIndexes())
          .add("hasGlobalSecondaryIndexes", x.hasGlobalSecondaryIndexes())
          .add("kmsMasterKeyId", x.kmsMasterKeyId())
          .add(
            "provisionedThroughputOverride",
            x.provisionedThroughputOverride()
          )
          .add("regionName", x.regionName())
          .add("replicaInaccessibleDateTime", x.replicaInaccessibleDateTime())
          .add("replicaStatus", x.replicaStatusAsString())
          .add("replicaStatusDescription", x.replicaStatusDescription())
          .add("replicaStatusPercentProgress", x.replicaStatusPercentProgress())
          .add("replicaTableClassSummary", x.replicaTableClassSummary())
          .build

      implicit val restoreSummaryShow: Show[RestoreSummary] = x =>
        ShowBuilder("RestoreSummary")
          .add("restoreDateTime", x.restoreDateTime())
          .add("restoreInProgress", x.restoreInProgress())
          .add("sourceBackupArn", x.sourceBackupArn())
          .add("sourceTableArn", x.sourceTableArn())
          .build

      implicit val sseDescriptionShow: Show[SSEDescription] = x =>
        ShowBuilder("SSEDescription")
          .add(
            "inaccessibleEncryptionDateTime",
            x.inaccessibleEncryptionDateTime()
          )
          .add("kmsMasterKeyArn", x.kmsMasterKeyArn())
          .add("sseType", x.sseTypeAsString())
          .add("status", x.statusAsString())
          .build

      implicit val tableDescriptionShow: Show[TableDescription] = x =>
        ShowBuilder("TableDescription")
          .add("archivalSummary", x.archivalSummary())
          .add("attributeDefinitions", x.attributeDefinitions())
          .add("billingModeSummary", x.billingModeSummary())
          .add("creationDateTime", x.creationDateTime())
          .add("globalSecondaryIndexes", x.globalSecondaryIndexes())
          .add("globalTableVersion", x.globalTableVersion())
          .add("hasAttributeDefinitions", x.hasAttributeDefinitions())
          .add("hasGlobalSecondaryIndexes", x.hasGlobalSecondaryIndexes())
          .add("hasKeySchema", x.hasKeySchema())
          .add("hasLocalSecondaryIndexes", x.hasLocalSecondaryIndexes())
          .add("hasReplicas", x.hasReplicas())
          .add("itemCount", x.itemCount())
          .add("keySchema", x.keySchema())
          .add("latestStreamArn", x.latestStreamArn())
          .add("latestStreamLabel", x.latestStreamLabel())
          .add("localSecondaryIndexes", x.localSecondaryIndexes())
          .add("provisionedThroughput", x.provisionedThroughput())
          .add("replicas", x.replicas())
          .add("restoreSummary", x.restoreSummary())
          .add("sseDescription", x.sseDescription())
          .add("streamSpecification", x.streamSpecification())
          .add("tableArn", x.tableArn())
          .add("tableClassSummary", x.tableClassSummary())
          .add("tableId", x.tableId())
          .add("tableName", x.tableName())
          .add("tableSizeBytes", x.tableSizeBytes())
          .add("tableStatus", x.tableStatusAsString())
          .build

      implicit val sdkBytesShow: Show[SdkBytes] = x =>
        Show[ByteBuffer].show(x.asByteBuffer())

      def attributeValueMapShowImpl(
          x: List[(String, AttributeValue)],
          res: String = "Map(",
          empty: Boolean = true
      ): Eval[String] = Eval.defer(x match {
        case Nil => Eval.now(s"$res)")
        case (k, v) :: t =>
          val vStr = attributeValueShowImpl(v)
          val newRes = if (empty) s"$res$k=$vStr" else s"$res,$k=$vStr"
          attributeValueMapShowImpl(t, newRes, empty = true)
      })

      def attributeValueListShowImpl(
          x: List[AttributeValue],
          res: String = "List(",
          empty: Boolean = true
      ): Eval[String] = Eval.defer(x match {
        case Nil => Eval.now(s"$res)")
        case v :: t =>
          val vStr = attributeValueShowImpl(v)
          val newRes = if (empty) s"$res$vStr" else s"$res,$vStr"
          attributeValueListShowImpl(t, newRes, empty = true)
      })

      def attributeValueShowImpl(x: AttributeValue): Eval[String] =
        for {
          prefix <- Eval.now("AttributeValue(")
          t = Option(x.`type`)
          value <- t match {
            case Some(AttributeValue.Type.BOOL) =>
              Eval.now(s"bool=${Show[Boolean].show(x.bool())}")
            case Some(AttributeValue.Type.B) =>
              Eval.now(s"b=${sdkBytesShow.show(x.b())}")
            case Some(AttributeValue.Type.BS) =>
              Eval.now(s"bs=${Show[java.util.List[SdkBytes]].show(x.bs())}")
            case Some(AttributeValue.Type.S) => Eval.now(s"s=${x.s()}")
            case Some(AttributeValue.Type.SS) =>
              Eval.now(s"ss=${Show[java.util.List[String]].show(x.ss())}")
            case Some(AttributeValue.Type.N) => Eval.now(s"n=${x.n()}")
            case Some(AttributeValue.Type.NS) =>
              Eval.now(s"ns=${Show[java.util.List[String]].show(x.ns())}")
            case Some(AttributeValue.Type.NUL) =>
              Eval.now(s"nul=${Show[Boolean].show(x.nul())}")
            case Some(AttributeValue.Type.UNKNOWN_TO_SDK_VERSION) =>
              Eval.now("UNKNOWN_TO_SDK_VERSION")
            case Some(AttributeValue.Type.M) =>
              attributeValueMapShowImpl(x.m().asScala.toList)
            case Some(AttributeValue.Type.L) =>
              attributeValueListShowImpl(x.l().asScala.toList)
            case None => Eval.now("")
          }
          tStr = t.fold("")(y => s",type=${y.name()}")
        } yield s"$prefix$value$tStr)"

      implicit val attributeValueShow: Show[AttributeValue] = x =>
        attributeValueShowImpl(x).value

      implicit val attributeValueUpdateShow: Show[AttributeValueUpdate] =
        x =>
          ShowBuilder("AttributeValueUpdate")
            .add("action", x.actionAsString())
            .add("value", x.value())
            .build

      implicit val capacityShow: Show[Capacity] = x =>
        ShowBuilder("Capacity")
          .add("capacityUnits", x.capacityUnits())
          .add("readCapacityUnits", x.readCapacityUnits())
          .add("writeCapacityUnits", x.writeCapacityUnits())
          .build

      implicit val consumedCapacityShow: Show[ConsumedCapacity] = x =>
        ShowBuilder("ConsumedCapacity")
          .add("capacityUnits", x.capacityUnits())
          .add("globalSecondaryIndexes", x.globalSecondaryIndexes())
          .add("hasGlobalSecondaryIndexes", x.hasGlobalSecondaryIndexes())
          .add("hasLocalSecondaryIndexes", x.hasLocalSecondaryIndexes())
          .add("localSecondaryIndexes", x.localSecondaryIndexes())
          .add("readCapacityUnits", x.readCapacityUnits())
          .add("table", x.table())
          .add("tableName", x.tableName())
          .add("writeCapacityUnits", x.writeCapacityUnits())
          .build

      implicit val ddbConditionShow: Show[Condition] = x =>
        ShowBuilder("Condition")
          .add("attributeValueList", x.attributeValueList())
          .add("comparisonOperator", x.comparisonOperatorAsString())
          .add("hasAttributeValueList", x.hasAttributeValueList())
          .build

      implicit val expectedAttributeValueShow: Show[ExpectedAttributeValue] =
        x =>
          ShowBuilder("ExpectedAttributeValue")
            .add("attributeValueList", x.attributeValueList())
            .add("comparisonOperator", x.comparisonOperatorAsString())
            .add("exists", x.exists())
            .add("hasAttributeValueList", x.hasAttributeValueList())
            .add("value", x.value())
            .build

      implicit val itemCollectionMetricsShow: Show[ItemCollectionMetrics] =
        x =>
          ShowBuilder("ItemCollectionMetrics")
            .add("hasItemCollectionKey", x.hasItemCollectionKey())
            .add("hasSizeEstimateRangeGB", x.hasSizeEstimateRangeGB())
            .add("itemCollectionKey", x.itemCollectionKey())
            .add("sizeEstimateRangeGB", x.sizeEstimateRangeGB())
            .build

      implicit val createTableRequestShow: Show[CreateTableRequest] = x =>
        ShowBuilder("CreateTableRequest")
          .add("attributeDefinitions", x.attributeDefinitions())
          .add("billingMode", x.billingModeAsString())
          .add("globalSecondaryIndexes", x.globalSecondaryIndexes())
          .add("hasAttributeDefinitions", x.hasAttributeDefinitions())
          .add("hasGlobalSecondaryIndexes", x.hasGlobalSecondaryIndexes())
          .add("hasKeySchema", x.hasKeySchema())
          .add("hasLocalSecondaryIndexes", x.hasLocalSecondaryIndexes())
          .add("hasTags", x.hasTags())
          .add("keySchema", x.keySchema())
          .add("localSecondaryIndexes", x.localSecondaryIndexes())
          .add("provisionedThroughput", x.provisionedThroughput())
          .add("sseSpecification", x.sseSpecification())
          .add("streamSpecification", x.streamSpecification())
          .add("tableClass", x.tableClassAsString())
          .add("tableName", x.tableName())
          .add("tags", x.tags())
          .build

      implicit val createTableResponseShow: Show[CreateTableResponse] = x =>
        ShowBuilder("CreateTableResponse")
          .add("tableDescription", x.tableDescription())
          .build

      implicit val describeTableRequestShow: Show[DescribeTableRequest] =
        x =>
          ShowBuilder("DescribeTableRequest")
            .add("tableName", x.tableName())
            .build

      implicit val describeTableResponseShow: Show[DescribeTableResponse] =
        x =>
          ShowBuilder("DescribeTableResponse")
            .add("table", x.table())
            .build

      implicit val scanRequestShow: Show[ScanRequest] = x =>
        ShowBuilder("ScanRequest")
          .add("attributesToGet", x.attributesToGet())
          .add("conditionalOperator", x.conditionalOperatorAsString())
          .add("consistentRead", x.consistentRead())
          .add("exclusiveStartKey", x.exclusiveStartKey())
          .add("expressionAttributeNames", x.expressionAttributeNames())
          .add("expressionAttributeValues", x.expressionAttributeValues())
          .add("hasAttributesToGet", x.hasAttributesToGet())
          .add("hasExclusiveStartKey", x.hasExclusiveStartKey())
          .add("hasExpressionAttributeNames", x.hasExpressionAttributeNames())
          .add("hasExpressionAttributeValues", x.hasExpressionAttributeValues())
          .add("hasScanFilter", x.hasScanFilter())
          .add("indexName", x.indexName())
          .add("limit", x.limit())
          .add("projectionExpression", x.projectionExpression())
          .add("returnConsumedCapacity", x.returnConsumedCapacityAsString())
          .add("scanFilter", x.scanFilter())
          .add("segment", x.segment())
          .add("select", x.selectAsString())
          .add("tableName", x.tableName())
          .add("totalSegments", x.totalSegments())
          .build

      implicit val scanResponseShow: Show[ScanResponse] = x =>
        ShowBuilder("ScanResponse")
          .add("consumedCapacity", x.consumedCapacity())
          .add("count", x.count())
          .add("hasItems", x.hasItems())
          .add("hasLastEvaluatedKey", x.hasLastEvaluatedKey())
          .add("items", x.items())
          .add("lastEvaluatedKey", x.lastEvaluatedKey())
          .add("scannedCount", x.scannedCount())
          .build

      implicit val updateItemRequestShow: Show[UpdateItemRequest] = x =>
        ShowBuilder("UpdateItemRequest")
          .add("attributeUpdates", x.attributeUpdates())
          .add("conditionExpression", x.conditionExpression())
          .add("conditionalOperator", x.conditionalOperatorAsString())
          .add("expected", x.expected())
          .add("expressionAttributeNames", x.expressionAttributeNames())
          .add("expressionAttributeValues", x.expressionAttributeValues())
          .add("hasAttributeUpdates", x.hasAttributeUpdates())
          .add("hasExpected", x.hasExpected())
          .add("hasExpressionAttributeNames", x.hasExpressionAttributeNames())
          .add("hasExpressionAttributeValues", x.hasExpressionAttributeValues())
          .add("hasKey", x.hasKey())
          .add("key", x.key())
          .add("returnConsumedCapacity", x.returnConsumedCapacityAsString())
          .add(
            "returnItemCollectionMetrics",
            x.returnItemCollectionMetricsAsString()
          )
          .add("returnValues", x.returnValuesAsString())
          .add("tableName", x.tableName())
          .add("updateExpression", x.updateExpression())
          .build

      implicit val updateItemResponseShow: Show[UpdateItemResponse] = x =>
        ShowBuilder("UpdateItemResponse")
          .add("attributes", x.attributes())
          .add("consumedCapacity", x.consumedCapacity())
          .add("hasAttributes", x.hasAttributes())
          .add("itemCollectionMetrics", x.itemCollectionMetrics())
          .build

      implicit val putItemRequestShow: Show[PutItemRequest] = x =>
        ShowBuilder("PutItemRequest")
          .add("conditionExpression", x.conditionExpression())
          .add("conditionalOperator", x.conditionalOperatorAsString())
          .add("expected", x.expected())
          .add("expressionAttributeNames", x.expressionAttributeNames())
          .add("expressionAttributeValues", x.expressionAttributeValues())
          .add("hasExpected", x.hasExpected())
          .add("hasExpressionAttributeNames", x.hasExpressionAttributeNames())
          .add("hasExpressionAttributeValues", x.hasExpressionAttributeValues())
          .add("item", x.item())
          .add("returnConsumedCapacity", x.returnConsumedCapacityAsString())
          .add(
            "returnItemCollectionMetrics",
            x.returnItemCollectionMetricsAsString()
          )
          .add("returnValues", x.returnValuesAsString())
          .add("tableName", x.tableName())
          .build

      implicit val putItemResponseShow: Show[PutItemResponse] = x =>
        ShowBuilder("PutItemResponse")
          .add("attributes", x.attributes())
          .add("consumedCapacity", x.consumedCapacity())
          .add("hasAttributes", x.hasAttributes())
          .add("itemCollectionMetrics", x.itemCollectionMetrics())
          .build

      implicit val deleteTableRequestShow: Show[DeleteTableRequest] = x =>
        ShowBuilder("DeleteTableRequest")
          .add("tableName", x.tableName())
          .build

      implicit val deleteTableResponseShow: Show[DeleteTableResponse] = x =>
        ShowBuilder("DeleteTableResponse")
          .add("tableDescription", x.tableDescription())
          .build
      new LogEncoders()
    }
  }
}
