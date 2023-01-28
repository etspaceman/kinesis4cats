# FS2

This module intends to be an enriched wrapper for the [KCL](https://docs.aws.amazon.com/streams/latest/dev/shared-throughput-kcl-consumers.html), offered by AWS. This specific wrapper uses an [FS2 Stream](https://fs2.io/#/guide?id=building-streams)

## Installation

```scala
libraryDependencies += "io.github.etspaceman" %% "kinesis4cats-kcl-fs2" % "@VERSION@"
```

## Usage

```scala mdoc:compile-only
import cats.effect._
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient

import kinesis4cats.kcl.fs2.KCLConsumerFS2
import kinesis4cats.kcl.logging.instances.show._
import kinesis4cats.syntax.bytebuffer._

object MyApp extends ResourceApp.Forever {
    override def run(args: List[String]) = for {
        kinesisClient <- Resource.fromAutoCloseable(IO(KinesisAsyncClient.builder().build()))
        dynamoClient <- Resource.fromAutoCloseable(IO(DynamoDbAsyncClient.builder().build()))
        cloudWatchClient <- Resource.fromAutoCloseable(IO(CloudWatchAsyncClient.builder().build()))
        consumer <- KCLConsumerFS2.configsBuilder[IO](
            kinesisClient, 
            dynamoClient, 
            cloudWatchClient, 
            "my-stream", 
            "my-app-name"
        )()
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
