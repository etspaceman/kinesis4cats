# Circe Logging

Circe instances for the KPL LogEncoders. Offers a richer experience with log ingestors that can work with JSON data easily.

## Installation

```scala
libraryDependencies += "io.github.etspaceman" %% "kinesis4cats-kpl-logging-circe" % "@VERSION@"
```

## Usage

```scala mdoc:compile-only
import cats.effect._

import kinesis4cats.kpl.logging.instances.circe
import kinesis4cats.kpl.KPLProducer

KPLProducer[IO](encoders = circe.kplProducer)
```
