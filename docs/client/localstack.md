# Localstack

The ability to provide a @:source(kinesis4cats.client.KinesisClient) that is compliant with Localstack

## Installation

```scala
libraryDependencies += "io.github.etspaceman" %% "kinesis4cats-kinesis-client-localstack" % "@VERSION@"
```

## Usage

```scala mdoc:compile-only
import cats.effect.IO

import kinesis4cats.client.localstack.LocalstackKinesisClient
import kinesis4cats.client.logging.instances.show._

// Load a KinesisClient as an effect
LocalstackKinesisClient.client[IO]()

// Load a KinesisClient as a Resource
LocalstackKinesisClient.clientResource[IO]()

// Load a KinesisClient as a Resource.
// Also creates and deletes a stream during it's usage. Useful for tests.
LocalstackKinesisClient.streamResource[IO]("my-stream", 1)
```
