# Getting started with the Kinesis Client

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
