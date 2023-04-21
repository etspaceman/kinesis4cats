# Getting Started

This module intends to be an enriched wrapper for the [KCL](https://docs.aws.amazon.com/streams/latest/dev/shared-throughput-kcl-consumers.html), offered by AWS.

## Installation

```scala
libraryDependencies += "io.github.etspaceman" %% "kinesis4cats-kcl" % "@VERSION@"
```

## Usage

```scala mdoc:compile-only
import cats.effect._
import cats.syntax.all._
import software.amazon.kinesis.processor.SingleStreamTracker

import kinesis4cats.kcl._
import kinesis4cats.syntax.bytebuffer._

object MyApp extends ResourceApp.Forever {
    override def run(args: List[String]) = for {
        consumerBuilder <- KCLConsumer.Builder.default[IO](
            new SingleStreamTracker("my-stream"),
            "my-app-name",
        )
        consumer <- consumerBuilder.withCallback(
            (records: List[CommittableRecord[IO]]) => 
                records.traverse_(r => IO.println(r.data.asString))
        ).build
        _ <- consumer.run()
    } yield ()
}
```

## Usage - Multi Stream

The KCL introduced the capability to consume from multiple streams within the same application on an experimental basis. This module offers some helpers for constructing consumers for this.

It is not recommended to use this in production as scaling the application becomes more difficult when you have to consider more than one stream.

```scala mdoc:compile-only
import cats.effect._
import cats.effect.syntax.all._
import cats.syntax.all._
import software.amazon.kinesis.common._

import kinesis4cats.client._
import kinesis4cats.models._
import kinesis4cats.kcl._
import kinesis4cats.kcl.multistream._
import kinesis4cats.syntax.bytebuffer._

object MyApp extends ResourceApp.Forever {
    override def run(args: List[String]) = { 
        val streamArn1 = StreamArn(AwsRegion.US_EAST_1, "my-stream-1", "123456789012")
        val streamArn2 = StreamArn(AwsRegion.US_EAST_1, "my-stream-2", "123456789012")
        val position = InitialPositionInStreamExtended
            .newInitialPosition(InitialPositionInStream.TRIM_HORIZON)
        for {
            kinesisClient <- KinesisClient.Builder.default[IO].build
            tracker <- MultiStreamTracker.noLeaseDeletionFromArns[IO](
                kinesisClient,
                Map(streamArn1 -> position, streamArn2 -> position)
            ).toResource
            consumerBuilder <- KCLConsumer.Builder
                .default[IO](tracker, "my-app-name")
            consumer <- consumerBuilder.withCallback(
                (records: List[CommittableRecord[IO]]) => 
                    records.traverse_(r => IO.println(r.data.asString))
            ).build
            _ <- consumer.run()
        } yield ()
    }
}
```

## FS2

This package intends to be an enriched wrapper for the [KCL](https://docs.aws.amazon.com/streams/latest/dev/shared-throughput-kcl-consumers.html), offered by AWS. This specific wrapper uses an [FS2 Stream](https://fs2.io/#/guide?id=building-streams)

### Usage

```scala mdoc:compile-only
import cats.effect._
import software.amazon.kinesis.processor.SingleStreamTracker

import kinesis4cats.kcl.fs2.KCLConsumerFS2
import kinesis4cats.syntax.bytebuffer._

object MyApp extends ResourceApp.Forever {
    override def run(args: List[String]) = for {
        consumerBuilder <- KCLConsumerFS2.Builder.default[IO](
            new SingleStreamTracker("my-stream"), 
            "my-app-name",
        )
        consumer <- consumerBuilder.build
        _ <- consumer
            .stream()
            .flatMap(stream =>
                stream
                .evalTap(x => IO.println(x.data.asString))
                .through(consumer.commitRecords)
                .compile
                .resource
                .drain
            )
    } yield ()
}
```
