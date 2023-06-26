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
import software.amazon.awssdk.services.kinesis.model._

import kinesis4cats.client.KinesisClient

object MyApp extends IOApp {
    override def run(args: List[String]) = 
        KinesisClient.Builder.default[IO].build.use(client => 
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
import cats.data.NonEmptyList
import cats.effect._

import kinesis4cats.client.producer.KinesisProducer
import kinesis4cats.producer._
import kinesis4cats.models.StreamNameOrArn

object MyApp extends IOApp {
  override def run(args: List[String]) = 
    KinesisProducer.Builder
      .default[IO](StreamNameOrArn.Name("my-stream"))
      .build
      .use(producer =>
        for {
          _ <- producer.put(
            NonEmptyList.of(
              Record("my-data".getBytes(), "some-partition-key"),
              Record("my-data-2".getBytes(), "some-partition-key-2"),
              Record("my-data-3".getBytes(), "some-partition-key-3"),
            )
          )
        } yield ExitCode.Success
      )
}
```

### Failures vs Exceptions

The Producer interface works with the [PutRecords](https://docs.aws.amazon.com/kinesis/latest/APIReference/API_PutRecords.html) API. This API can yield two possible error paths:

1. **Exceptions:** Exceptions raised when interacting with the API. These are rare.
2. **Failures:** Successful API responses, but with a partially-failed response.

In the 2nd case, the `put` method will return a result that contains:
- Successfully produced records
- Invalid records (records that do not conform to the Kinesis requirements for PutRecords)
- Failed records (records that could not be produced to the Kinesis stream)

The Producer offering here allows users to handle these failure paths in multiple ways.

#### Retrying failures

A user can supply a @:source(compat.src.main.scala.kinesis4cats.compat.retry.RetryPolicy) that can be used to retry both error paths until a fully successful response is received. 

In the event of a partially-failed response, the retry routine will only retry the failed records.

```scala mdoc:compile-only
import cats.effect._

import kinesis4cats.client.producer.KinesisProducer
import kinesis4cats.compat.retry._
import kinesis4cats.models.StreamNameOrArn

// Retry 5 times with no delay between retries
val policy = RetryPolicies.limitRetries[IO](5)

KinesisProducer.Builder
  .default[IO](StreamNameOrArn.Name("my-stream"))
  .transformConfig(x => x.copy(retryPolicy = policy))
  .build
```

#### Raising exceptions

A user can configure the producer to raise an exception if any of the error paths are detected (including partially failed records).

```scala mdoc:compile-only
import cats.effect._

import kinesis4cats.client.producer.KinesisProducer
import kinesis4cats.models.StreamNameOrArn

KinesisProducer.Builder
  .default[IO](StreamNameOrArn.Name("my-stream"))
  .transformConfig(x => x.copy(raiseOnFailures = true))
  .build
```

This can be done along with configuring a retry policy, in which case an error would be raised after all retries are exhausted.

```scala mdoc:compile-only
import cats.effect._

import kinesis4cats.client.producer.KinesisProducer
import kinesis4cats.compat.retry._
import kinesis4cats.models.StreamNameOrArn

// Retry 5 times with no delay between retries
val policy = RetryPolicies.limitRetries[IO](5)

KinesisProducer.Builder
  .default[IO](StreamNameOrArn.Name("my-stream"))
  .transformConfig(x => x.copy(retryPolicy = policy, raiseOnFailures = true))
  .build
```

## FS2 Producer

This package provides a [KPL-like](https://github.com/awslabs/amazon-kinesis-producer) producer via implementing @:source(shared.src.main.scala.kinesis4cats.producer.fs2.FS2Producer). This interface receives records from a user, enqueues them into a Queue and puts them as batches to Kinesis on a configured interval. This leverages all of the functionality of the @:source(shared.src.main.scala.kinesis4cats.producer.Producer) interface, including batching, aggregation and retries. 

```scala mdoc:compile-only
import cats.effect._

import kinesis4cats.client.producer.fs2.FS2KinesisProducer
import kinesis4cats.models.StreamNameOrArn
import kinesis4cats.producer.Record

object MyApp extends IOApp {
    override def run(args: List[String]) = 
        FS2KinesisProducer.Builder
          .default[IO](StreamNameOrArn.Name("my-stream"))
          .build
          .use(producer =>
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

### Callbacks

Kinesis4Cats offers a nested effect as the return type for the FS2Producer's `put` method. This nested effect serves as a callback for users to interact with the put result and any errors that occurred with the puts. For example:


```scala mdoc:compile-only
import cats.effect._
import cats.syntax.all._

import kinesis4cats.client.producer.fs2.FS2KinesisProducer
import kinesis4cats.models.StreamNameOrArn
import kinesis4cats.producer.Record

object MyApp extends IOApp {
    override def run(args: List[String]) = 
        FS2KinesisProducer.Builder
          .default[IO](StreamNameOrArn.Name("my-stream"))
          .build
          .use { producer =>
            val records = List(
              Record("my-data".getBytes(), "some-partition-key"),
              Record("my-data-2".getBytes(), "some-partition-key-2"),
              Record("my-data-3".getBytes(), "some-partition-key-3")
            )
            for {
              callbacks <- records.traverse(producer.put)
              _ <- callbacks.traverse(cb => cb.attempt.flatMap(x => IO.println(s"Response: $x")))
            } yield ExitCode.Success
        }
}
```
