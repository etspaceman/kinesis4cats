# FS2

This module provides a [KPL-like](https://github.com/awslabs/amazon-kinesis-producer) producer via implementing @:source(shared.src.main.scala.kinesis4cats.producer.fs2.FS2Producer). This interface receives records from a user, enqueues them into a Queue and puts them as batches to Kinesis on a configured interval. This leverages all of the functionality of the @:source(shared.src.main.scala.kinesis4cats.producer.Producer) interface, including batching, aggregation and retries. 

## Installation

```scala
libraryDependencies += "io.github.etspaceman" %% "kinesis4cats-client-fs2" % "@VERSION@"
```

## Usage

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
