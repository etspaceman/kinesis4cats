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
import software.amazon.kinesis.processor.SingleStreamTracker

import kinesis4cats.kcl._
import kinesis4cats.kcl.logging.instances.circe._
import kinesis4cats.syntax.bytebuffer._

object MyApp extends ResourceApp.Forever {
    override def run(args: List[String]) = for {
        consumerBuilder <- KCLConsumer.Builder.default[IO](
            new SingleStreamTracker("my-stream"),
            "my-app-name",
        )
        consumer <- consumerBuilder
            .withCallback(
                (records: List[CommittableRecord[IO]]) => 
                    records.traverse_(r => IO.println(r.data.asString))
            )
            .configure(_.withLogEncoders(kclCirceEncoders))
            .build
        _ <- consumer.run()
    } yield ()
}
```
