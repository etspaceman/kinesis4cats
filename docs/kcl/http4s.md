# Http4s

Provides a coupling of a @:source(kcl.src.main.scala.kinesis4cats.kcl.KCLConsumer) with an [Http4s](https://http4s.org/) server, that services two routes:

- healthcheck (determines whether the http server is up)
- initialized (determines whether the KCL is up)

## Installation

```scala
libraryDependencies += "io.github.etspaceman" %% "kinesis4cats-kcl-http4s" % "@VERSION@"
```

## Usage

```scala mdoc:compile-only
import cats.effect._
import cats.syntax.all._
import com.comcast.ip4s._
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient
import software.amazon.kinesis.processor.SingleStreamTracker

import kinesis4cats.kcl._
import kinesis4cats.kcl.http4s.KCLService
import kinesis4cats.kcl.logging.instances.show._
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
            "my-app-name"
        )((records: List[CommittableRecord[IO]]) => 
            records.traverse_(r => IO.println(r.data.asString))
        )()
        _ <- KCLService.server[IO](consumer, port"8080", host"0.0.0.0")
    } yield ()
}
```
