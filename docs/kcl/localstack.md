# Localstack

The ability to provide a @:source(kcl.src.main.scala.kinesis4cats.kcl.KCLConsumer) that is compliant with Localstack

## Installation

```scala
libraryDependencies += "io.github.etspaceman" %% "kinesis4cats-kcl-localstack" % "@VERSION@"
```

## Usage

```scala mdoc:compile-only
import cats.effect.IO
import cats.syntax.all._

import kinesis4cats.kcl._
import kinesis4cats.kcl.logging.instances.show._
import kinesis4cats.kcl.localstack.LocalstackKCLConsumer
import kinesis4cats.syntax.bytebuffer._

val processRecords = (records: List[CommittableRecord[IO]]) => records.traverse_(record => IO.println(record.data.asString))

// Runs a KCLConsumer as a Resource. Resource contains a Deferred value, which completes when the consumer has begun to process records.
LocalstackKCLConsumer.kclConsumer[IO]("my-stream", "my-app-name")(processRecords)

// Runs a KCLConsumer as a Resource. Resource contains 2 things: 
// - A Deferred value, which completes when the consumer has begun to process records. 
// - A results Queue, which contains records received by the consumer
LocalstackKCLConsumer.kclConsumerWithResults[IO]("my-stream", "my-app-name")(processRecords)
```
