# Localstack

The ability to provide a @:source(smithy4s-client.src.main.scala.kinesis4cats.smithy4s.client.KinesisClient) that is compliant with Localstack

## Installation

```scala
libraryDependencies += "io.github.etspaceman" %% "kinesis4cats-smithy4s-client-localstack" % "@VERSION@"
```

## Usage

```scala mdoc:compile-only
import cats.effect._
import org.http4s.ember.client.EmberClientBuilder
import org.typelevel.log4cats.slf4j.Slf4jLogger
import smithy4s.aws._

import kinesis4cats.logging.instances.show._
import kinesis4cats.smithy4s.client.localstack.LocalstackKinesisClient
import kinesis4cats.smithy4s.client.logging.instances.show._
// Load a KinesisClient as a Resource
val kinesisClientResource = for {
    underlying <- EmberClientBuilder
        .default[IO]
        .withoutCheckEndpointAuthentication
        .build
    client <- LocalstackKinesisClient.clientResource[IO](
        underlying,
        AwsRegion.US_EAST_1,
        loggerF = (_: Async[IO]) => Slf4jLogger.create[IO]
    )
} yield client
```
