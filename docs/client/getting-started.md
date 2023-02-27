# Getting Started

This module intends to be an enriched wrapper for the [KinesisAsyncClient](https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/kinesis/KinesisAsyncClient.html) class, offered by the Java SDK. 

## Installation

```scala
libraryDependencies += "io.github.etspaceman" %% "kinesis4cats-client" % "@VERSION@"
```

## Usage

```scala mdoc:compile-only
import cats.effect._
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient
import software.amazon.awssdk.services.kinesis.model._

import kinesis4cats.client.KinesisClient
import kinesis4cats.client.logging.instances.show._

object MyApp extends IOApp {
    override def run(args: List[String]) = 
        KinesisClient[IO](KinesisAsyncClient.builder().build()).use(client => 
            for {
                _ <- client.createStream(
                    CreateStreamRequest
                        .builder()
                        .streamName("my-stream")
                        .shardCount(1)
                        .build()
                )
                _ <- client.putRecord(
                    PutRecordRequest
                        .builder()
                        .partitionKey("some-partition-key")
                        .streamName("my-stream")
                        .data(SdkBytes.fromUtf8String("my-data"))
                        .build()
                )
            } yield ExitCode.Success
        )
}
```

## Producer

kinesis4cats offers a @:source(shared.src.main.scala.kinesis4cats.producer.Producer) interface that handles the following:

- Maintains a @:source(shared.src.main.scala.kinesis4cats.producer.ShardMapCache), which will routinely track the open shards for a Kinesis stream. It is used to predict which shard a record will be produced to.
- Aggregates records using the [KPL Aggregation Format](https://docs.aws.amazon.com/streams/latest/dev/kinesis-kpl-concepts.html#kinesis-kpl-concepts-aggretation) (if configured)
- Batches records against known Kinesis limits (or a user-defined set of configuration).
- Produces records to Kinesis
- Provides an Error interface for users to interact with failed records (e.g. retrying failures)

This module provides an implementation of that interface, backed by the @:source(kinesis-client.src.main.scala.kinesis4cats.client.KinesisClient).


```scala mdoc:compile-only
import scala.concurrent.duration._

import cats.data.NonEmptyList
import cats.effect._
import cats.syntax.all._
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient

import kinesis4cats.client.logging.instances.show._
import kinesis4cats.client.producer.KinesisProducer
import kinesis4cats.producer.logging.instances.show._
import kinesis4cats.producer._
import kinesis4cats.models.StreamNameOrArn

object MyApp extends IOApp {
    override def run(args: List[String]) = 
        KinesisProducer[IO](
            Producer.Config.default(StreamNameOrArn.Name("my-stream")), 
            KinesisAsyncClient.builder().build()
        ).use(producer =>
            for {
                _ <- producer.put(
                    NonEmptyList.of(
                        Record("my-data".getBytes(), "some-partition-key"),
                        Record("my-data-2".getBytes(), "some-partition-key-2"),
                        Record("my-data-3".getBytes(), "some-partition-key-3"),
                    )
                )
                // Retries failed records with a configured limit and duration.
                _ <- producer.putWithRetry(
                    NonEmptyList.of(
                        Record("my-data".getBytes(), "some-partition-key"),
                        Record("my-data-2".getBytes(), "some-partition-key-2"),
                        Record("my-data-3".getBytes(), "some-partition-key-3"),
                    ),
                    Some(5),
                    1.second
                )
            } yield ExitCode.Success
        )
}
```

## FS2 Producer

This package provides a [KPL-like](https://github.com/awslabs/amazon-kinesis-producer) producer via implementing @:source(shared.src.main.scala.kinesis4cats.producer.fs2.FS2Producer). This interface receives records from a user, enqueues them into a Queue and puts them as batches to Kinesis on a configured interval. This leverages all of the functionality of the @:source(shared.src.main.scala.kinesis4cats.producer.Producer) interface, including batching, aggregation and retries. 

```scala mdoc:compile-only
import cats.effect._
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient

import kinesis4cats.client.logging.instances.show._
import kinesis4cats.client.producer.fs2.FS2KinesisProducer
import kinesis4cats.producer.logging.instances.show._
import kinesis4cats.producer._
import kinesis4cats.producer.fs2._
import kinesis4cats.models.StreamNameOrArn

object MyApp extends IOApp {
    override def run(args: List[String]) = 
        FS2KinesisProducer[IO](
            FS2Producer.Config.default(StreamNameOrArn.Name("my-stream")), 
            KinesisAsyncClient.builder().build()
        ).use(producer =>
            for {
                _ <- producer.put(
                    Record("my-data".getBytes(), "some-partition-key")
                )
                _ <- producer.put(
                    Record("my-data-2".getBytes(), "some-partition-key-2")
                )
                _ <- producer.put(
                    Record("my-data-3".getBytes(), "some-partition-key-3")
                )
            } yield ExitCode.Success
        )
}
```
