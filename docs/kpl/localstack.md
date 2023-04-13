# Localstack

The ability to provide a @:source(modules.kpl.src.main.scala.kinesis4cats.kpl.KPLProducer) that is compliant with Localstack

## Installation

```scala
libraryDependencies += "io.github.etspaceman" %% "kinesis4cats-kpl-localstack" % "@VERSION@"
```

## Usage

```scala mdoc:compile-only
import cats.effect.IO

import kinesis4cats.kpl.logging.instances.show._
import kinesis4cats.kpl.localstack.LocalstackKPLProducer

// Load a KPLProducer as a resource
LocalstackKPLProducer.producer[IO]()

// Load a KPLProducer as a resource.
// Also creates and deletes a stream during it's usage. Useful for tests.
LocalstackKPLProducer.producerWithStream[IO]("my-stream", 1)
```
