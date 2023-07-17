# AWS Lambda

AWS offers the ability to consume Kinesis events via an [AWS Lambda](https://docs.aws.amazon.com/lambda/latest/dg/with-kinesis.html).

kinesis4cats offers an @:source(shared.src.main.scala.kinesis4cats.consumer.lambda.KinesisStreamRecordPayload) that maps to the event structure received in the lambda. This can be used in a [Feral](https://github.com/typelevel/feral) instance.

Feral does offer a similar event, in which you can see an example usage [here](https://github.com/typelevel/feral/blob/main/examples/src/main/scala/feral/examples/KinesisLambda.scala). However, one common problem that users have with Kinesis Lambda functions is that they do not perform [record deaggregation](https://github.com/awslabs/kinesis-aggregation#deaggregation). kinesis4cats offers a `deaggregate` function in its `KinesisStreamEvent` class, which users can leverage to resolve this problem.

## Usage

```scala mdoc:compile-only
import kinesis4cats.consumer.lambda._

val myEvent: KinesisStreamEvent = ???

myEvent.deaggregate
```
