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

package kinesis4cats.kcl
package processor

import scala.jdk.CollectionConverters._

import java.nio.ByteBuffer
import java.time.Instant

import cats.effect.std.Queue
import cats.effect.{Deferred, IO}
import software.amazon.kinesis.lifecycle.events.{InitializationInput, ProcessRecordsInput}
import software.amazon.kinesis.retrieval.KinesisClientRecord
import software.amazon.kinesis.retrieval.kpl.ExtendedSequenceNumber

import kinesis4cats.kcl.CommittableRecord
import kinesis4cats.kcl.logging.instances.show._
import kinesis4cats.syntax.bytebuffer._

class RecordProcessorSpec extends munit.CatsEffectSuite {

  test("It should run successfully") {
    for {
      deferredException <- Deferred[IO, Throwable]
      resultsQueue <- Queue.bounded[IO, CommittableRecord[IO]](50)
      commitResultsQueue <- Queue.bounded[IO, MockCheckpoint](50)
      _ <- RecordProcessorFactory[IO](
        RecordProcessorConfig.default,
        deferredException,
        true
      )(recs => resultsQueue.tryOfferN(recs).void).use { factory =>
        for {
          processor <- IO(factory.shardRecordProcessor())
          _ <- IO(
            processor.initialize(
              InitializationInput
                .builder()
                .shardId("000000000001")
                .extendedSequenceNumber(ExtendedSequenceNumber.LATEST)
                .build()
            )
          )
          _ <- IO(
            processor.processRecords(
              ProcessRecordsInput
                .builder()
                .checkpointer(
                  MockRecordProcessorCheckpointer(commitResultsQueue)
                )
                .isAtShardEnd(false)
                .millisBehindLatest(0L)
                .records(
                  List(
                    KinesisClientRecord
                      .builder()
                      .aggregated(false)
                      .approximateArrivalTimestamp(Instant.now())
                      .data(ByteBuffer.wrap("foo".getBytes()))
                      .partitionKey("123")
                      .sequenceNumber("1")
                      .subSequenceNumber(1L)
                      .build()
                  ).asJava
                )
                .build()
            )
          )
        } yield ()
      }
      result <- resultsQueue.tryTake.map(_.map(_.record.data().asString))
      commitResult <- commitResultsQueue.tryTake
    } yield {
      assertEquals(result, Some("foo"))
      assertEquals(commitResult, Some(MockCheckpoint("1", 1L)))
    }
  }

  test("It should raise an error successfully") {
    for {
      deferredException <- Deferred[IO, Throwable]
      commitResultsQueue <- Queue.bounded[IO, MockCheckpoint](50)
      expected = new RuntimeException("This is an expected error")
      _ <- RecordProcessorFactory[IO](
        RecordProcessorConfig.default,
        deferredException,
        true
      )(_ => IO.raiseError(expected)).use { factory =>
        for {
          processor <- IO(factory.shardRecordProcessor())
          _ <- IO(
            processor.initialize(
              InitializationInput
                .builder()
                .shardId("000000000001")
                .extendedSequenceNumber(ExtendedSequenceNumber.LATEST)
                .build()
            )
          )
          _ <- IO(
            processor.processRecords(
              ProcessRecordsInput
                .builder()
                .checkpointer(
                  MockRecordProcessorCheckpointer(commitResultsQueue)
                )
                .isAtShardEnd(false)
                .millisBehindLatest(0L)
                .records(
                  List(
                    KinesisClientRecord
                      .builder()
                      .aggregated(false)
                      .approximateArrivalTimestamp(Instant.now())
                      .data(ByteBuffer.wrap("foo".getBytes()))
                      .partitionKey("123")
                      .sequenceNumber("1")
                      .subSequenceNumber(1L)
                      .build()
                  ).asJava
                )
                .build()
            )
          )
        } yield ()
      }
      result <- deferredException.get
    } yield {
      assertEquals(result, expected)
    }
  }

}
