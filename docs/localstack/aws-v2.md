# AWS V2

This module allows users to configure common clients for the [V2 AWS SDK](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/home.html) that are compliant with [Localstack](https://localstack.cloud/).

## Installation

```scala
libraryDependencies += "io.github.etspaceman" %% "kinesis4cats-aws-v2-localstack" % "@VERSION@"
```

## Usage

```scala mdoc:compile-only
import cats.effect.IO

import kinesis4cats.localstack.aws.v1.AwsClients
import kinesis4cats.localstack.TestStreamConfig

// Load a KinesisAsyncClient as an effect
AwsClients.kinesisClient[IO]()

// Load a KinesisAsyncClient as a resource
AwsClients.kinesisClientResource[IO]()

// Load a KinesisAsyncClient as a resource.
// Also creates and deletes streams during it's usage. Useful for tests.
AwsClients.kinesisStreamResource[IO](
  List(
    TestStreamConfig.default[IO]("my-stream", 1),
    TestStreamConfig.default[IO]("my-stream-2", 1)
  )
)
```
