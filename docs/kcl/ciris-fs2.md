# Ciris FS2

Standard environment variables and system properties for configuring a @:source(kcl.src.main.scala.kinesis4cats.kcl.fs2.KCLConsumerFS2), via [Ciris](https://cir.is/)

## Installation

```scala
libraryDependencies += "io.github.etspaceman" %% "kinesis4cats-kcl-fs2-ciris" % "@VERSION@"
```

## Usage

```scala mdoc:compile-only
import cats.effect._
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient

import kinesis4cats.kcl.fs2.ciris.KCLCirisFS2
import kinesis4cats.kcl.logging.instances.show._
import kinesis4cats.syntax.bytebuffer._

object MyApp extends ResourceApp.Forever {
    override def run(args: List[String]) = for {
        kinesisClient <- Resource.fromAutoCloseable(IO(KinesisAsyncClient.builder().build()))
        dynamoClient <- Resource.fromAutoCloseable(IO(DynamoDbAsyncClient.builder().build()))
        cloudWatchClient <- Resource.fromAutoCloseable(IO(CloudWatchAsyncClient.builder().build()))
        consumer <- KCLCirisFS2.consumer[IO](kinesisClient, dynamoClient, cloudWatchClient)
        _ <- consumer.stream()
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

## Configuration

The configuration below is intended to only work with single-stream consumers at this time.

### FS2

| Environment Variable | System Property | Required | Default | Description |
| - | - | - | - | - |
| `KCL_FS2_QUEUE_SIZE` | `kcl.fs2.queue.size` | No | 100 | Size of the global records queue. If full, backpressure will occur. |
| `KCL_FS2_COMMIT_MAX_CHUNK` | `kcl.fs2.commit.max.chunk` | No | 1000 | Max size of records in the commit queue before commits are made. |
| `KCL_FS2_COMMIT_MAX_WAIT` | `kcl.fs2.commit.max.wait` | No | 10 seconds | Max interval between commit batch evaluation. |
| `KCL_FS2_COMMIT_MAX_RETRIES` | `kcl.fs2.commit.max.retries` | No | 5 | Max retries for running commits |
| `KCL_FS2_COMMIT_RETRY_INTERVAL` | `kcl.fs2.commit.retry.interval` | No | 0 seconds | Interval between commit retries |
