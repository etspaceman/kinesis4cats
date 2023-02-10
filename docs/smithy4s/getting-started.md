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
import org.http4s.ember.client.EmberClientBuilder
import org.typelevel.log4cats.slf4j.Slf4jLogger
import smithy4s.aws._
import smithy4s.ByteArray

import kinesis4cats.smithy4s.client.KinesisClient
import kinesis4cats.smithy4s.client.logging.instances.show._

object MyApp extends IOApp {
    override def run(args: List[String]) = (for {
        underlying <- EmberClientBuilder.default[IO].build
        client <- KinesisClient[IO](
            underlying, 
            AwsRegion.US_EAST_1, 
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
