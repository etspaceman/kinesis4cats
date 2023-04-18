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

import kinesis4cats.smithy4s.client.producer.KinesisProducer
import kinesis4cats.smithy4s.client.logging.instances.circe._
import kinesis4cats.producer._
import kinesis4cats.producer.logging.instances.circe._
import kinesis4cats.models.StreamNameOrArn

BlazeClientBuilder[IO].resource.flatMap(client =>
            KinesisProducer[IO](
                Producer.Config.default(StreamNameOrArn.Name("my-stream")),
                client,
                IO.pure(AwsRegion.US_EAST_1),
                loggerF = (_: Async[IO]) => Slf4jLogger.create[IO],
                encoders = producer,
                shardMapEncoders = shardmap,
                kinesisClientEncoders = kinesisClient[IO]
            )
        )
```
