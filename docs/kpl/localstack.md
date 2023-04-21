# Localstack

The ability to provide a @:source(kpl.src.main.scala.kinesis4cats.kpl.KPLProducer) that is compliant with Localstack

## Installation

```scala
libraryDependencies += "io.github.etspaceman" %% "kinesis4cats-kpl-localstack" % "@VERSION@"
```

## Usage

```scala mdoc:compile-only
import cats.effect.IO
import cats.effect.syntax.all._

import kinesis4cats.localstack.TestStreamConfig
import kinesis4cats.kpl.localstack.LocalstackKPLProducer

// Load a KPLProducer as a resource
LocalstackKPLProducer.Builder
  .default[IO]()
  .toResource
  .flatMap(_.build)

// Load a KPLProducer as a resource.
// Also creates and deletes streams during it's usage. Useful for tests.
LocalstackKPLProducer.Builder
  .default[IO]()
  .toResource
  .flatMap(x => 
    x.withStreamsToCreate(
      List(
        TestStreamConfig.default[IO]("my-stream", 1),
        TestStreamConfig.default[IO]("my-stream-2", 1)
      )
    ).build
  )
```
