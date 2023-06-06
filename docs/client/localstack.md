# Localstack

The ability to provide a @:source(kinesis-client.src.main.scala.kinesis4cats.client.KinesisClient) that is compliant with Localstack

## Installation

```scala
libraryDependencies += "io.github.etspaceman" %% "kinesis4cats-kinesis-client-localstack" % "@VERSION@"
```

## Usage

```scala mdoc:compile-only
import cats.effect.IO

import kinesis4cats.client.localstack.LocalstackKinesisClient
import kinesis4cats.client.producer.localstack.LocalstackKinesisProducer
import kinesis4cats.client.producer.fs2.localstack.LocalstackFS2KinesisProducer
import kinesis4cats.models.StreamNameOrArn
import kinesis4cats.localstack.TestStreamConfig

// Load a KinesisClient as a Resource
LocalstackKinesisClient.Builder
    .default[IO]()
    .toResource
    .flatMap(_.build)

// Load a KinesisClient as a Resource.
// Also creates and deletes streams during it's usage. Useful for tests.
LocalstackKinesisClient.Builder
    .default[IO]()
    .toResource
    .flatMap(x => 
        x.withStreamsToCreate(
            List(
                TestStreamConfig.default[IO]("my-stream", 1),
                TestStreamConfig.default[IO]("my-stream-2", 1),
            )
        )
        .build    
    )

// Load a KinesisProducer as a resource
LocalstackKinesisProducer.Builder
    .default[IO](StreamNameOrArn.Name("my-stream"))
    .toResource
    .flatMap(_.build)

// Load a FS2KinesisProducer as a resource
LocalstackFS2KinesisProducer.Builder
    .default[IO](StreamNameOrArn.Name("my-stream"))
    .toResource
    .flatMap(_.build)
```
