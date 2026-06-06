# Metrics

The @:source(shared.src.main.scala.kinesis4cats.producer.Producer) records [OpenTelemetry](https://opentelemetry.io/) metrics on the produce path via [otel4s](https://typelevel.org/otel4s/). Metrics are **off by default** (a no-op instrument bundle), so you only pay for what you opt into.

There are two ways to enable them:

1. **Bring your own `MeterProvider`** — wire the producer into any OpenTelemetry backend you already run (`withMetrics`).
2. **Export to CloudWatch** — a batteries-included module that ships metrics straight to Amazon CloudWatch (`buildWithCloudWatch`). Unlike the Java-SDK client, this path is **cross-platform** (JVM/JS/Native): the CloudWatch client is the pure-Scala smithy4s client and the exporter is `otel4s-sdk`.

The emitted metric names, units, and attributes are identical to the Java-SDK client — see the [client metrics reference](../client/metrics.md#emitted-metrics) for the full table. The instrumentation scope is `kinesis4cats` and the metric-name namespace defaults to `kinesis4cats.producer`.

## Bring your own `MeterProvider`

Both the `KinesisProducer` and `FS2KinesisProducer` builders expose `withMetrics`, which takes any otel4s [`MeterProvider`](https://typelevel.org/otel4s/instrumentation/metrics.html). The example below uses `MeterProvider.noop` as a stand-in — swap in your real provider.

```scala mdoc:compile-only
import cats.effect._
import org.http4s.blaze.client.BlazeClientBuilder
import org.typelevel.otel4s.metrics.MeterProvider
import smithy4s.aws._

import kinesis4cats.smithy4s.client.producer.KinesisProducer
import kinesis4cats.models.StreamNameOrArn

BlazeClientBuilder[IO].resource.flatMap(client =>
  KinesisProducer.Builder
    .default[IO](StreamNameOrArn.Name("my-stream"), client, AwsRegion.US_EAST_1)
    .withMetrics(MeterProvider.noop[IO])
    .build
)
```

## Export to CloudWatch

The `smithy4s-client-opentelemetry` module bundles a CloudWatch-exporting `MeterProvider` with the producer.

```scala
libraryDependencies += "io.github.etspaceman" %% "kinesis4cats-smithy4s-client-opentelemetry" % "@VERSION@"
```

Import the syntax and add `withCloudWatchMetrics()` to the builder before `build`. By default it reuses the builder's own http4s client and region:

```scala mdoc:compile-only
import cats.effect._
import org.http4s.blaze.client.BlazeClientBuilder
import smithy4s.aws._

import kinesis4cats.smithy4s.client.producer.KinesisProducer
import kinesis4cats.smithy4s.client.producer.opentelemetry.syntax.cloudwatch._
import kinesis4cats.models.StreamNameOrArn

BlazeClientBuilder[IO].resource.flatMap(client =>
  KinesisProducer.Builder
    .default[IO](StreamNameOrArn.Name("my-stream"), client, AwsRegion.US_EAST_1)
    .withCloudWatchMetrics()
    .build
)
```

The same syntax is available on the `FS2KinesisProducer` builder:

```scala mdoc:compile-only
import cats.effect._
import org.http4s.blaze.client.BlazeClientBuilder
import smithy4s.aws._

import kinesis4cats.smithy4s.client.producer.fs2.FS2KinesisProducer
import kinesis4cats.smithy4s.client.producer.opentelemetry.syntax.cloudwatch._
import kinesis4cats.models.StreamNameOrArn

BlazeClientBuilder[IO].resource.flatMap(client =>
  FS2KinesisProducer.Builder
    .default[IO](StreamNameOrArn.Name("my-stream"), client, AwsRegion.US_EAST_1)
    .withCloudWatchMetrics()
    .build
)
```

### Backends

`withCloudWatchMetrics` supports two transports, selected via the `backend` parameter:

- `CloudWatchBackend.PutMetricData` *(default)* — the GA [`PutMetricData`](https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_PutMetricData.html) API. Works in all regions and requires no extra infrastructure.
- `CloudWatchBackend.Otlp` *(public preview)* — CloudWatch's native OTLP endpoint. Availability varies by region.

```scala mdoc:compile-only
import cats.effect._
import org.http4s.blaze.client.BlazeClientBuilder
import smithy4s.aws._

import kinesis4cats.smithy4s.client.producer.KinesisProducer
import kinesis4cats.smithy4s.client.producer.opentelemetry.syntax.cloudwatch._
import kinesis4cats.models.StreamNameOrArn
import kinesis4cats.producer.metrics.cloudwatch.CloudWatchBackend

BlazeClientBuilder[IO].resource.flatMap(client =>
  KinesisProducer.Builder
    .default[IO](StreamNameOrArn.Name("my-stream"), client, AwsRegion.US_EAST_1)
    .withCloudWatchMetrics(backend = CloudWatchBackend.Otlp)
    .build
)
```

### Configuration

`withCloudWatchMetrics` accepts the following parameters, all with sensible defaults:

| Parameter | Default | Description |
| --- | --- | --- |
| `region` | the builder's region | The region to export metrics to |
| `httpClient` | the builder's http4s client | The client used to call CloudWatch |
| `namespace` | `kinesis4cats.producer` | The OpenTelemetry metric-name prefix |
| `cloudWatchNamespace` | `KinesisProducerLibrary` | The CloudWatch namespace the metrics land under (matches the KPL for drop-in dashboards) |
| `credentials` | smithy4s-aws default credentials provider | The credentials used to call CloudWatch |
| `backend` | `CloudWatchBackend.PutMetricData` | The export transport (see [Backends](#backends)) |
