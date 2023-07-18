# Feral

AWS offers the ability to consume Kinesis events via an [AWS Lambda](https://docs.aws.amazon.com/lambda/latest/dg/with-kinesis.html).

kinesis4cats offers an @:source(feral.src.main.scala.kinesis4cats.consumer.feral.KinesisStreamEvent) that maps to the event structure received in the lambda. This can be used in a [Feral](https://github.com/typelevel/feral) instance.

Feral does offer a similar event, in which you can see an example usage [here](https://github.com/typelevel/feral/blob/main/examples/src/main/scala/feral/examples/KinesisLambda.scala). However, one common problem that users have with Kinesis Lambda functions is that they do not perform [record deaggregation](https://github.com/awslabs/kinesis-aggregation#deaggregation). kinesis4cats offers a `deaggregate` function in its `KinesisStreamEvent` class, which users can leverage to resolve this problem.

## Installation

```scala
libraryDependencies += "io.github.etspaceman" %% "kinesis4cats-feral" % "@VERSION@"
```

## Usage

```scala mdoc:compile-only
import cats.effect._
import feral.lambda._

import kinesis4cats.consumer.feral.KinesisStreamEvent

object kinesisHandler extends IOLambda.Simple[KinesisStreamEvent, INothing] {
  type Init
  def apply(event: KinesisStreamEvent, context: Context[IO], init: Init) = for {
    records <- IO.fromTry(event.deaggregate)
    _ <- IO.println(s"Received event with ${records.size} records").as(None)
  } yield None
}
```
