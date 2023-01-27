# Localstack

[Localstack](https://localstack.cloud/) is the primary tool of choice for localized AWS development. It offers a mock that allows users to write integration tests for AWS services without deploying anything to the cloud. This is extremely useful.

There are some specific ways that we need to configure clients to work with [Localstack's Docker offering](https://docs.localstack.cloud/getting-started/installation/#docker). This library offers tooling to make this configuration easy.

## Installation

```scala
libraryDependencies += "io.github.etspaceman" %% "kinesis4cats-shared-localstack" % "@VERSION@"
```

## Configuration

Configuration is loaded through [Ciris](https://cir.is/)

| Environment Variable      | System Property           | Default   | Description                                           |
| ------------------------- | ------------------------- | --------- | ----------------------------------------------------- |
| `LOCALSTACK_SERVICE_PORT` | `localstack.service.port` | 4566      | Localstack port                                       |
| `LOCALSTACK_PROTOCOL`     | `localstack.protocol`     | https     | Either https or http. Both work on the same host/port |
| `LOCALSTACK_AWS_REGION`   | `localstack.aws.region`   | us-east-1 | Default region used for API calls to Localstack       |
| `LOCALSTACK_HOST`         | `localstack.host    `     | localhost | Localstack hostname                                   |

## Usage

```scala mdoc:compile-only
import cats.effect.IO

import kinesis4cats.localstack.LocalstackConfig

// Load as a Ciris ConfigValue
LocalstackConfig.read()

// Load as an effect
LocalstackConfig.load[IO]()

// Load as a resource
LocalstackConfig.resource[IO]()
```
