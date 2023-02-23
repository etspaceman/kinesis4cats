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
import smithy4s.ByteArray

import kinesis4cats.smithy4s.client.KinesisClient
import kinesis4cats.smithy4s.client.logging.instances.show._

object MyApp extends IOApp {
    override def run(args: List[String]) = (for {
        underlying <- BlazeClientBuilder[IO].resource
        client <- KinesisClient[IO](
            underlying, 
            IO.pure(AwsRegion.US_EAST_1), 
            loggerF = (_: Async[IO]) => Slf4jLogger.create[IO]
        )
    } yield client).use(client =>
        for {
            _ <- client.createStream(StreamName("my-stream"), Some(1))
            _ <- client.putRecord(
                Data(ByteArray("my-data".getBytes())),
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
import scala.concurrent.duration._

import cats.data.NonEmptyList
import cats.effect._
import cats.syntax.all._
import org.http4s.blaze.client.BlazeClientBuilder
import org.typelevel.log4cats.slf4j.Slf4jLogger
import smithy4s.aws._

import kinesis4cats.smithy4s.client.logging.instances.show._
import kinesis4cats.smithy4s.client.producer.KinesisProducer
import kinesis4cats.producer.logging.instances.show._
import kinesis4cats.producer._
import kinesis4cats.models.StreamNameOrArn

object MyApp extends IOApp {
    override def run(args: List[String]) =
        BlazeClientBuilder[IO].resource.flatMap(client =>
            KinesisProducer[IO](
                Producer.Config.default(StreamNameOrArn.Name("my-stream")),
                client,
                IO.pure(AwsRegion.US_EAST_1),
                loggerF = (_: Async[IO]) => Slf4jLogger.create[IO]
            )
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
