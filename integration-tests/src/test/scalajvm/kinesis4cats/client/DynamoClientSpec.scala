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

import cats.effect.{IO, SyncIO}
import software.amazon.awssdk.services.dynamodb.model._

import kinesis4cats.Utils
import kinesis4cats.client.localstack.LocalstackDynamoClient

class DynamoClientSpec extends munit.CatsEffectSuite {
  def fixture: SyncIO[FunFixture[DynamoClient[IO]]] =
    ResourceFunFixture(
      LocalstackDynamoClient.clientResource[IO]()
    )

  val tableName = s"dynamo-client-spec-${Utils.randomUUIDString}"

  fixture.test("It should work through all commands") { client =>
    for {
      _ <- client.createTable(
        CreateTableRequest
          .builder()
          .tableName(tableName)
          .keySchema(
            KeySchemaElement
              .builder()
              .keyType(KeyType.HASH)
              .attributeName("key")
              .build()
          )
          .attributeDefinitions(
            AttributeDefinition
              .builder()
              .attributeType(ScalarAttributeType.S)
              .attributeName("key")
              .build()
          )
          .billingMode(BillingMode.PAY_PER_REQUEST)
          .build()
      )
      _ <- client.describeTable(
        DescribeTableRequest
          .builder()
          .tableName(tableName)
          .build()
      )
      _ <- client.putItem(
        PutItemRequest
          .builder()
          .tableName(tableName)
          .item(Map("key" -> AttributeValue.fromS("bar")).asJava)
          .build()
      )
      _ <- client.updateItem(
        UpdateItemRequest
          .builder()
          .tableName(tableName)
          .attributeUpdates(
            Map(
              "foo" -> AttributeValueUpdate
                .builder()
                .action(AttributeAction.PUT)
                .value(AttributeValue.fromS("wozzle"))
                .build()
            ).asJava
          )
          .key(Map("key" -> AttributeValue.fromS("bar")).asJava)
          .build()
      )
      _ <- client.deleteTable(
        DeleteTableRequest.builder().tableName(tableName).build()
      )
    } yield assert(true)
  }

}
