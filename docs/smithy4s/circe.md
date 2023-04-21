# Circe Logging

Circe instances for the KinesisClient LogEncoders. Offers a richer experience with log ingestors that can work with JSON data easily.

## Installation

```scala
libraryDependencies += "io.github.etspaceman" %% "kinesis4cats-smithy4s-client-logging-circe" % "@VERSION@"
```

## Usage
```scala mdoc:compile-only
import cats.effect._
import org.http4s.blaze.client.BlazeClientBuilder
import org.typelevel.log4cats.slf4j.Slf4jLogger
import smithy4s.aws._

import kinesis4cats.smithy4s.client.KinesisClient
import kinesis4cats.smithy4s.client.logging.instances.circe._

BlazeClientBuilder[IO].resource.flatMap(underlying =>
    KinesisClient.Builder
        .default[IO](underlying, AwsRegion.US_EAST_1)
        .withLogger(Slf4jLogger.getLogger)
        .withLogEncoders(kinesisClientCirceEncoders[IO])
        .build
)
```
