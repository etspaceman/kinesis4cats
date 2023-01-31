# AWS V1

This module allows users to configure common clients for the [V1 AWS SDK](https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/welcome.html) that are compliant with [Localstack](https://localstack.cloud/).

## Installation

```scala
libraryDependencies += "io.github.etspaceman" %% "kinesis4cats-aws-v1-localstack" % "@VERSION@"
```

## Usage

```scala mdoc:compile-only
import cats.effect.IO

import kinesis4cats.localstack.aws.v1.AwsClients

// Load an AmazonKinesisAsync as an effect
AwsClients.kinesisClient[IO]()

// Load an AmazonKinesisAsync as a resource
AwsClients.kinesisClientResource[IO]()

// Load a AmazonKinesisAsync as a resource.
// Also creates and deletes a stream during it's usage. Useful for tests.
AwsClients.kinesisStreamResource[IO]("my-stream", 1)
```
