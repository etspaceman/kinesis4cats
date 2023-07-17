# Lambda Circe Instances

Some circe instances are provided in the `shared-circe` module, which can be used to decode the event in [Feral](https://github.com/typelevel/feral/tree/main).

## Installation

```scala
libraryDependencies += "io.github.etspaceman" %% "kinesis4cats-shared-circe" % "@VERSION@"
```

## Usage

```scala mdoc:compile-only
import kinesis4cats.consumer.lambda.instances.circe._
```
