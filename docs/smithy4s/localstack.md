# Localstack

The ability to provide a @:source(modules.smithy4s-client.src.main.scala.kinesis4cats.smithy4s.client.KinesisClient) that is compliant with Localstack

## Installation

```scala
libraryDependencies += "io.github.etspaceman" %% "kinesis4cats-smithy4s-client-localstack" % "@VERSION@"
```

## Usage

```scala mdoc:compile-only
import cats.effect._
import org.http4s.blaze.client.BlazeClientBuilder
import org.typelevel.log4cats.slf4j.Slf4jLogger
import smithy4s.aws._

import kinesis4cats.logging.instances.show._
import kinesis4cats.smithy4s.client.localstack.LocalstackKinesisClient
import kinesis4cats.smithy4s.client.logging.instances.show._
import kinesis4cats.smithy4s.client.producer.localstack.LocalstackKinesisProducer
import kinesis4cats.smithy4s.client.producer.fs2.localstack.LocalstackFS2KinesisProducer
import kinesis4cats.producer.logging.instances.show._
// Load a KinesisClient as a Resource
val kinesisClientResource = for {
    underlying <- BlazeClientBuilder[IO]
        .withCheckEndpointAuthentication(false)
        .resource
    client <- LocalstackKinesisClient.clientResource[IO](
        underlying,
        IO.pure(AwsRegion.US_EAST_1),
        loggerF = (_: Async[IO]) => Slf4jLogger.create[IO]
    )
} yield client

// Load a KinesisProducer as a Resource
val kinesisProducerResource = for {
    underlying <- BlazeClientBuilder[IO]
        .withCheckEndpointAuthentication(false)
        .resource
    producer <- LocalstackKinesisProducer.resource[IO](
        underlying,
        "my-stream",
        IO.pure(AwsRegion.US_EAST_1),
        loggerF = (_: Async[IO]) => Slf4jLogger.create[IO]
    )
} yield producer

// Load a FS2KinesisProducer as a Resource
val fs2KinesisProducerResource = for {
    underlying <- BlazeClientBuilder[IO]
        .withCheckEndpointAuthentication(false)
        .resource
    producer <- LocalstackFS2KinesisProducer.resource[IO](
        underlying,
        "my-stream",
        IO.pure(AwsRegion.US_EAST_1),
        loggerF = (_: Async[IO]) => Slf4jLogger.create[IO]
    )
} yield producer
```
