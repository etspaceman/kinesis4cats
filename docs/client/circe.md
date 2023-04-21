# Circe Logging

Circe instances for the KinesisClient LogEncoders. Offers a richer experience with log ingestors that can work with JSON data easily.

## Installation

```scala
libraryDependencies += "io.github.etspaceman" %% "kinesis4cats-client-logging-circe" % "@VERSION@"
```

## Usage

```scala mdoc:compile-only
import cats.effect._

import kinesis4cats.client.KinesisClient
import kinesis4cats.client.logging.instances.circe._

KinesisClient.Builder.default[IO].withLogEncoders(kinesisClientCirceEncoders).build
```
