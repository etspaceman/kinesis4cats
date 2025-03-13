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

package kinesis4cats.consumer

import java.time.Instant

import cats.syntax.all._
import scodec.bits.ByteVector

import kinesis4cats.producer

final class RecordSpec extends munit.FunSuite {
  val config = producer.batching.Batcher.Config.default

  test("It should be able to deaggregate") {
    val data1 = Array.fill[Byte](500)(1)
    val partitionKey1 = "foo"
    val data2 = Array.fill[Byte](500)(1)
    val partitionKey2 = "wazzle"

    val batch = producer.batching.AggregatedBatch
      .create(
        producer.Record.WithShard
          .fromOption(producer.Record(data1, partitionKey1), None),
        producer.batching.Batcher.Config.default
      )
      .add(
        producer.Record.WithShard
          .fromOption(producer.Record(data2, partitionKey2), None)
      )

    val testBytes = batch.asBytes

    val record = Record(
      "foo",
      Instant.now(),
      ByteVector(testBytes),
      batch.partitionKey,
      None,
      None,
      None
    )

    val res = Record.deaggregate(List(record)).get

    assert(res.head.data.toArray.sameElements(data1))
    assert(res.head.partitionKey === partitionKey1)
    assert(res(1).data.toArray.sameElements(data2))
    assert(res(1).partitionKey === partitionKey2)
  }

  test("Deaggregate should not fail for non-aggregated records") {
    val data1 = "foo".getBytes()
    val partitionKey1 = "foo"

    val record = Record(
      "foo",
      Instant.now(),
      ByteVector(data1),
      partitionKey1,
      None,
      None,
      None
    )

    val res = Record.deaggregate(List(record)).get

    assert(res.head.data.toArray.sameElements(data1))
    assert(res.head.partitionKey === partitionKey1)
  }
}
