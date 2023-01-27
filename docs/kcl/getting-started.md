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
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient

import kinesis4cats.kcl._
import kinesis4cats.kcl.logging.instances.show._
import kinesis4cats.syntax.bytebuffer._

object MyApp extends ResourceApp.Forever {
    override def run(args: List[String]) = for {
        kinesisClient <- Resource.fromAutoCloseable(IO(KinesisAsyncClient.builder().build()))
        dynamoClient <- Resource.fromAutoCloseable(IO(DynamoDbAsyncClient.builder().build()))
        cloudWatchClient <- Resource.fromAutoCloseable(IO(CloudWatchAsyncClient.builder().build()))
        consumer <- KCLConsumer.configsBuilder[IO](
            kinesisClient, 
            dynamoClient, 
            cloudWatchClient, 
            "my-stream", 
            "my-app-name"
        )((records: List[CommittableRecord[IO]]) => records.traverse_(r => IO.println(r.data.asString)))()
        _ <- consumer.run()
    } yield ()
}
```
