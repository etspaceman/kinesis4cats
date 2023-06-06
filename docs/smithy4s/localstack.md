# Localstack

The ability to provide a @:source(smithy4s-client.src.main.scala.kinesis4cats.smithy4s.client.KinesisClient) that is compliant with Localstack

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

import kinesis4cats.models.StreamNameOrArn
import kinesis4cats.smithy4s.client.localstack.LocalstackKinesisClient
import kinesis4cats.smithy4s.client.producer.localstack.LocalstackKinesisProducer
import kinesis4cats.smithy4s.client.producer.fs2.localstack.LocalstackFS2KinesisProducer
// Load a KinesisClient as a Resource
val kinesisClientResource = for {
    underlying <- BlazeClientBuilder[IO]
        .withCheckEndpointAuthentication(false)
        .resource
    client <- LocalstackKinesisClient.Builder    
        .default[IO](underlying, AwsRegion.US_EAST_1)
        .toResource
        .flatMap(x => 
            x.withLogger(Slf4jLogger.getLogger).build
        )
} yield client

// Load a KinesisProducer as a Resource
val kinesisProducerResource = for {
    underlying <- BlazeClientBuilder[IO]
        .withCheckEndpointAuthentication(false)
        .resource
    producer <- LocalstackKinesisProducer.Builder    
        .default[IO](
            underlying, 
            AwsRegion.US_EAST_1,
            StreamNameOrArn.Name("my-stream")
        )
        .toResource
        .flatMap(x => 
            x.withLogger(Slf4jLogger.getLogger).build
        )
} yield producer

// Load a FS2KinesisProducer as a Resource
val fs2KinesisProducerResource = for {
    underlying <- BlazeClientBuilder[IO]
        .withCheckEndpointAuthentication(false)
        .resource
    producer <- LocalstackFS2KinesisProducer.Builder    
        .default[IO](
            underlying, 
            AwsRegion.US_EAST_1,
            StreamNameOrArn.Name("my-stream")
        )
        .toResource
        .flatMap(x => 
            x.withLogger(Slf4jLogger.getLogger).build
        )
} yield producer
```
