# Getting Started

This module intends to be a native Scala implementation of a Kinesis Client using [Smithy4s](https://disneystreaming.github.io/smithy4s/). The advantages of this are enormous, and we can provide interfaces that are more comfortable to every day Scala users.

## Experimental Project Warning

:warning: **WARNING This project is experimental. It should not be expected to be production ready at this time.**

Some known issues:

- [SubscribeToShard](https://docs.aws.amazon.com/kinesis/latest/APIReference/API_SubscribeToShard.html) will not work properly as it uses the http2 protocl.
- Updates to the smithy file(s) in this module are not intended to be backwards compatible.

## Installation

```scala
libraryDependencies += "io.github.etspaceman" %% "kinesis4cats-smithy4s-client" % "@VERSION@"
```

## Usage

```scala mdoc:compile-only
import cats.effect._
import com.amazonaws.kinesis._
import org.http4s.blaze.client.BlazeClientBuilder
import org.typelevel.log4cats.slf4j.Slf4jLogger
import smithy4s.aws._
import smithy4s.Blob

import kinesis4cats.smithy4s.client.KinesisClient

object MyApp extends IOApp {
    override def run(args: List[String]) = (for {
        underlying <- BlazeClientBuilder[IO].resource
        client <- KinesisClient.Builder
            .default[IO](underlying, AwsRegion.US_EAST_1)
            .withLogger(Slf4jLogger.getLogger)
            .build
    } yield client).use(client =>
        for {
            _ <- client.createStream(StreamName("my-stream"), Some(1))
            _ <- client.putRecord(
                Data(Blob("my-data".getBytes())),
                PartitionKey("some-partitionk-key"),
                Some(StreamName("my-stream"))
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

This module provides an implementation of that interface, backed by the @:source(smithy4s-client.src.main.scala.kinesis4cats.smithy4s.client.KinesisClient).


```scala mdoc:compile-only
import cats.data.NonEmptyList
import cats.effect._
import org.http4s.blaze.client.BlazeClientBuilder
import org.typelevel.log4cats.slf4j.Slf4jLogger
import smithy4s.aws._

import kinesis4cats.smithy4s.client.producer.KinesisProducer
import kinesis4cats.producer._
import kinesis4cats.models.StreamNameOrArn

object MyApp extends IOApp {
    override def run(args: List[String]) =
        BlazeClientBuilder[IO].resource.flatMap(client =>
            KinesisProducer.Builder
                .default[IO](
                    StreamNameOrArn.Name("my-stream"),
                    client,
                    AwsRegion.US_EAST_1
                )
                .withLogger(Slf4jLogger.getLogger)
                .build
        ).use(producer =>
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
import org.http4s.blaze.client.BlazeClientBuilder
import org.typelevel.log4cats.slf4j.Slf4jLogger
import smithy4s.aws._

import kinesis4cats.compat.retry._
import kinesis4cats.smithy4s.client.producer.KinesisProducer
import kinesis4cats.models.StreamNameOrArn

// Retry 5 times with no delay between retries
val policy = RetryPolicies.limitRetries[IO](5)

BlazeClientBuilder[IO].resource.flatMap(client =>
    KinesisProducer.Builder
        .default[IO](
            StreamNameOrArn.Name("my-stream"),
            client,
            AwsRegion.US_EAST_1
        )
        .transformConfig(x => x.copy(retryPolicy = policy))
        .withLogger(Slf4jLogger.getLogger)
        .build
)
```

#### Raising exceptions

A user can configure the producer to raise an exception if any of the error paths are detected (including partially failed records).

```scala mdoc:compile-only
import cats.effect._
import org.http4s.blaze.client.BlazeClientBuilder
import org.typelevel.log4cats.slf4j.Slf4jLogger
import smithy4s.aws._

import kinesis4cats.smithy4s.client.producer.KinesisProducer
import kinesis4cats.models.StreamNameOrArn

BlazeClientBuilder[IO].resource.flatMap(client =>
    KinesisProducer.Builder
        .default[IO](
            StreamNameOrArn.Name("my-stream"),
            client,
            AwsRegion.US_EAST_1
        )
        .transformConfig(x => x.copy(raiseOnFailures = true))
        .withLogger(Slf4jLogger.getLogger)
        .build
)
```

This can be done along with configuring a retry policy, in which case an error would be raised after all retries are exhausted.

```scala mdoc:compile-only
import cats.effect._
import org.http4s.blaze.client.BlazeClientBuilder
import org.typelevel.log4cats.slf4j.Slf4jLogger
import smithy4s.aws._

import kinesis4cats.compat.retry._
import kinesis4cats.smithy4s.client.producer.KinesisProducer
import kinesis4cats.models.StreamNameOrArn

// Retry 5 times with no delay between retries
val policy = RetryPolicies.limitRetries[IO](5)

BlazeClientBuilder[IO].resource.flatMap(client =>
    KinesisProducer.Builder
        .default[IO](
            StreamNameOrArn.Name("my-stream"),
            client,
            AwsRegion.US_EAST_1
        )
        .transformConfig(x => x.copy(retryPolicy = policy, raiseOnFailures = true))
        .withLogger(Slf4jLogger.getLogger)
        .build
)
```

## FS2

This package provides a [KPL-like](https://github.com/awslabs/amazon-kinesis-producer) producer via implementing @:source(shared.src.main.scala.kinesis4cats.producer.fs2.FS2Producer). This interface receives records from a user, enqueues them into a Queue and puts them as batches to Kinesis on a configured interval. This leverages all of the functionality of the @:source(shared.src.main.scala.kinesis4cats.producer.Producer) interface, including batching, aggregation and retries.

### Usage

```scala mdoc:compile-only
import cats.effect._
import org.http4s.blaze.client.BlazeClientBuilder
import org.typelevel.log4cats.slf4j.Slf4jLogger
import smithy4s.aws._

import kinesis4cats.smithy4s.client.producer.fs2.FS2KinesisProducer
import kinesis4cats.models.StreamNameOrArn
import kinesis4cats.producer.Record

object MyApp extends IOApp {
    override def run(args: List[String]) =
        BlazeClientBuilder[IO].resource.flatMap(client =>
            FS2KinesisProducer.Builder
                .default[IO](
                    StreamNameOrArn.Name("my-stream"),
                    client,
                    AwsRegion.US_EAST_1
                )
                .withLogger(Slf4jLogger.getLogger)
                .build
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

### Callbacks

Kinesis4Cats offers a nested effect as the return type for the FS2Producer's `put` method. This nested effect serves as a callback for users to interact with the put result and any errors that occurred with the puts. For example:


```scala mdoc:compile-only
import cats.effect._
import cats.syntax.all._
import org.http4s.blaze.client.BlazeClientBuilder
import org.typelevel.log4cats.slf4j.Slf4jLogger
import smithy4s.aws._

import kinesis4cats.smithy4s.client.producer.fs2.FS2KinesisProducer
import kinesis4cats.models.StreamNameOrArn
import kinesis4cats.producer.Record

object MyApp extends IOApp {
    override def run(args: List[String]) =
        BlazeClientBuilder[IO].resource.flatMap(client =>
            FS2KinesisProducer.Builder
                .default[IO](
                    StreamNameOrArn.Name("my-stream"),
                    client,
                    AwsRegion.US_EAST_1
                )
                .withLogger(Slf4jLogger.getLogger)
                .build
        ).use { producer =>
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
