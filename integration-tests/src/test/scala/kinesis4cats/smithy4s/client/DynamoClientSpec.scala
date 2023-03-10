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
package smithy4s.client

import _root_.smithy4s.aws.AwsRegion
import cats.effect._
import com.amazonaws.dynamodb._
import fs2.io.net.tls.TLSContext
import org.http4s.ember.client.EmberClientBuilder

import kinesis4cats.Utils
import kinesis4cats.logging.ConsoleLogger
import kinesis4cats.logging.instances.show._
import kinesis4cats.smithy4s.client.localstack.LocalstackDynamoClient
import kinesis4cats.smithy4s.client.logging.instances.show._

abstract class DynamoClientSpec extends munit.CatsEffectSuite {

  val region = AwsRegion.US_EAST_1
  def fixture: SyncIO[FunFixture[DynamoClient[IO]]] =
    ResourceFunFixture(
      for {
        tlsContext <- TLSContext.Builder.forAsync[IO].insecureResource
        underlying <- EmberClientBuilder
          .default[IO]
          .withTLSContext(tlsContext)
          .withoutCheckEndpointAuthentication
          .build
        client <- LocalstackDynamoClient.clientResource[IO](
          underlying,
          IO.pure(region),
          loggerF = (f: Async[IO]) => f.pure(new ConsoleLogger[IO])
        )
      } yield client
    )

  val tableName = s"dynamo-smithy4s-client-spec-${Utils.randomUUIDString}"

  fixture.test("It should work through all commands") { client =>
    for {
      _ <- client.createTable(
        List(
          AttributeDefinition(
            KeySchemaAttributeName("key"),
            ScalarAttributeType.S
          )
        ),
        TableName(tableName),
        List(KeySchemaElement(KeySchemaAttributeName("key"), KeyType.HASH)),
        billingMode = Some(BillingMode.PAY_PER_REQUEST)
      )
      _ <- client.describeTable(TableName(tableName))
      _ <- client.putItem(
        TableName(tableName),
        Map(AttributeName("key") -> AttributeValue.SCase(StringAttributeValue("bar")))
      )
      _ <- client.updateItem(
        TableName(tableName),
        Map(AttributeName("key") -> AttributeValue.SCase(StringAttributeValue("bar"))),
        Some(
          Map(
            AttributeName("foo") -> AttributeValueUpdate(
              Some(AttributeValue.SCase(StringAttributeValue("wozzle"))),
              Some(AttributeAction.PUT)
            )
          )
        )
      )
      _ <- client.deleteTable(
        TableName(tableName)
      )
    } yield assert(true)
  }

}
