# Circe Logging

Circe instances for the KCL LogEncoders. Offers a richer experience with log ingestors that can work with JSON data easily.

## Installation

```scala
libraryDependencies += "io.github.etspaceman" %% "kinesis4cats-kcl-logging-circe" % "@VERSION@"
```

## Usage

```scala mdoc:compile-only
import cats.effect._
import cats.effect.syntax.all._
import cats.syntax.all._
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient
import software.amazon.kinesis.common._

import kinesis4cats.client._
import kinesis4cats.client.logging.instances.circe.{kinesisClient => circeKinesisClient}
import kinesis4cats.models._
import kinesis4cats.kcl._
import kinesis4cats.kcl.logging.instances.circe.{recordProcessor => circeRecordProcessor}
import kinesis4cats.kcl.multistream._
import kinesis4cats.syntax.bytebuffer._

object MyApp extends ResourceApp.Forever {
    override def run(args: List[String]) = for {
        kinesisClient <- KinesisClient[IO](
            KinesisAsyncClient.builder().build(),
            circeKinesisClient
        )
        dynamoClient <- Resource.fromAutoCloseable(
            IO(DynamoDbAsyncClient.builder().build())
        )
        cloudWatchClient <- Resource.fromAutoCloseable(
            IO(CloudWatchAsyncClient.builder().build())
        )
        streamArn1 = StreamArn(AwsRegion.US_EAST_1, "my-stream-1", "123456789012")
        streamArn2 = StreamArn(AwsRegion.US_EAST_1, "my-stream-2", "123456789012")
        position = InitialPositionInStreamExtended
            .newInitialPosition(InitialPositionInStream.TRIM_HORIZON)
        tracker <- MultiStreamTracker.noLeaseDeletionFromArns[IO](
            kinesisClient,
            Map(streamArn1 -> position, streamArn2 -> position)
        ).toResource
        consumer <- KCLConsumer.configsBuilder[IO](
            kinesisClient.client, 
            dynamoClient, 
            cloudWatchClient, 
            tracker, 
            "my-app-name",
            encoders = circeRecordProcessor
        )((records: List[CommittableRecord[IO]]) => 
            records.traverse_(r => IO.println(r.data.asString))
        )()
        _ <- consumer.run()
    } yield ()
}
```
