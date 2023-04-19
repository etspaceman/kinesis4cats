# Circe Logging

Circe instances for the KCL LogEncoders. Offers a richer experience with log ingestors that can work with JSON data easily.

## Installation

```scala
libraryDependencies += "io.github.etspaceman" %% "kinesis4cats-kcl-logging-circe" % "@VERSION@"
```

## Usage

```scala mdoc:compile-only
import cats.effect._
import cats.syntax.all._
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient
import software.amazon.kinesis.processor.SingleStreamTracker

import kinesis4cats.kcl._
import kinesis4cats.kcl.logging.instances.circe._
import kinesis4cats.syntax.bytebuffer._

object MyApp extends ResourceApp.Forever {
    override def run(args: List[String]) = for {
        kinesisClient <- Resource.fromAutoCloseable(
            IO(KinesisAsyncClient.builder().build())
        )
        dynamoClient <- Resource.fromAutoCloseable(
            IO(DynamoDbAsyncClient.builder().build())
        )
        cloudWatchClient <- Resource.fromAutoCloseable(
            IO(CloudWatchAsyncClient.builder().build())
        )
        consumer <- KCLConsumer.configsBuilder[IO](
            kinesisClient, 
            dynamoClient, 
            cloudWatchClient, 
            new SingleStreamTracker("my-stream"), 
            "my-app-name",
            encoders = kclCirceEncoders
        )((records: List[CommittableRecord[IO]]) => 
            records.traverse_(r => IO.println(r.data.asString))
        )()
        _ <- consumer.run()
    } yield ()
}
```
