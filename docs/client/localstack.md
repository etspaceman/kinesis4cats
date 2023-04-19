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

// Load a KinesisClient as a Resource
LocalstackKinesisClient.clientResource[IO]()

// Load a KinesisClient as a Resource.
// Also creates and deletes a stream during it's usage. Useful for tests.
LocalstackKinesisClient.streamResource[IO]("my-stream", 1)

// Load a KinesisProducer as a resource
LocalstackKinesisProducer.resource[IO]("my-stream")

// Load a FS2KinesisProducer as a resource
LocalstackFS2KinesisProducer.resource[IO]("my-stream")
```
