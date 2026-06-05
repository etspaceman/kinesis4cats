# Metrics

The @:source(shared.src.main.scala.kinesis4cats.producer.Producer) records [OpenTelemetry](https://opentelemetry.io/) metrics on the produce path via [otel4s](https://typelevel.org/otel4s/). Metrics are **off by default** (a no-op instrument bundle), so you only pay for what you opt into.

There are two ways to enable them:

1. **Bring your own `MeterProvider`** — wire the producer into any OpenTelemetry backend you already run (`withMetrics`).
2. **Export to CloudWatch** — a batteries-included module that ships metrics straight to Amazon CloudWatch (`buildWithCloudWatch`), with no separate OpenTelemetry Collector to operate.

## Emitted metrics

The instrumentation scope is `kinesis4cats`. Metric names are prefixed with a namespace that defaults to `kinesis4cats.producer` and is configurable (see [Namespace](#namespace)).

### Produce path (`Producer` / `KinesisProducer`)

| Metric | Instrument | Unit | Attributes | Description |
| --- | --- | --- | --- | --- |
| `<ns>.user_records.received` | Counter | `{record}` | `stream.name` | User records received by the producer (emitted once per `put`, not on retries) |
| `<ns>.user_records.bytes` | Counter | `By` | `stream.name` | Payload bytes of user records received |
| `<ns>.kinesis_records.put` | Counter | `{record}` | `stream.name`, `shard.id` | Records sent to Kinesis in shard batches |
| `<ns>.kinesis_records.bytes` | Counter | `By` | `stream.name`, `shard.id` | Payload bytes sent to Kinesis in shard batches |
| `<ns>.request.duration` | Histogram | `s` | `stream.name`, `shard.id` | Latency of a single Kinesis put request |
| `<ns>.retries` | Histogram | `{retry}` | `stream.name` | Retries performed for a put |
| `<ns>.errors` | Counter | `{error}` | `stream.name`, `error.code` | Failed and invalid records, keyed by `error.code` (client-side invalid records use the sentinel `InvalidRecord`) |

### Buffer path (`FS2Producer` / `FS2KinesisProducer`)

The FS2 producer additionally records metrics for its in-memory buffer:

| Metric | Instrument | Unit | Attributes | Description |
| --- | --- | --- | --- | --- |
| `<ns>.buffer.records.pending` | UpDownCounter | `{record}` | `stream.name` | Records buffered but not yet handed to a put |
| `<ns>.buffer.time` | Histogram | `s` | `stream.name` | Time a record waited in the buffer before being put |
| `<ns>.buffer.records.dropped` | Counter | `{record}` | `stream.name`, `reason` | Records rejected by the buffer, keyed by `reason` (`queue-full` or `shutdown`) |

## Bring your own `MeterProvider`

Both the `KinesisProducer` and `FS2KinesisProducer` builders expose `withMetrics`, which takes any otel4s [`MeterProvider`](https://typelevel.org/otel4s/instrumentation/metrics.html). Use this to emit into whatever OpenTelemetry pipeline you already operate. The example below uses `MeterProvider.noop` as a stand-in — swap in your real provider (e.g. from `otel4s-oteljava` or `otel4s-sdk`).

```scala mdoc:compile-only
import cats.effect._
import org.typelevel.otel4s.metrics.MeterProvider

import kinesis4cats.client.producer.KinesisProducer
import kinesis4cats.models.StreamNameOrArn

KinesisProducer.Builder
  .default[IO](StreamNameOrArn.Name("my-stream"))
  .withMetrics(MeterProvider.noop[IO])
  .build
```

## Export to CloudWatch

The `kinesis-client-opentelemetry` module bundles a CloudWatch-exporting `MeterProvider` with the producer so you can ship metrics to CloudWatch directly.

```scala
libraryDependencies += "io.github.etspaceman" %% "kinesis4cats-kinesis-client-opentelemetry" % "@VERSION@"
```

Import the syntax and replace `build` with `buildWithCloudWatch`:

```scala mdoc:compile-only
import cats.effect._

import kinesis4cats.client.producer.KinesisProducer
import kinesis4cats.client.producer.opentelemetry.syntax.cloudwatch._
import kinesis4cats.models.StreamNameOrArn

KinesisProducer.Builder
  .default[IO](StreamNameOrArn.Name("my-stream"))
  .buildWithCloudWatch()
```

The same syntax is available on the `FS2KinesisProducer` builder:

```scala mdoc:compile-only
import cats.effect._

import kinesis4cats.client.producer.fs2.FS2KinesisProducer
import kinesis4cats.client.producer.opentelemetry.syntax.cloudwatch._
import kinesis4cats.models.StreamNameOrArn

FS2KinesisProducer.Builder
  .default[IO](StreamNameOrArn.Name("my-stream"))
  .buildWithCloudWatch()
```

### Backends

`buildWithCloudWatch` supports two transports, selected via the `backend` parameter:

- `CloudWatchBackend.PutMetricData` *(default)* — the GA [`PutMetricData`](https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_PutMetricData.html) API. Works in all regions and requires no extra infrastructure.
- `CloudWatchBackend.Otlp` *(public preview)* — CloudWatch's native OTLP endpoint. Availability varies by region.

```scala mdoc:compile-only
import cats.effect._

import kinesis4cats.client.producer.KinesisProducer
import kinesis4cats.client.producer.opentelemetry.syntax.cloudwatch._
import kinesis4cats.models.StreamNameOrArn
import kinesis4cats.producer.metrics.cloudwatch.CloudWatchBackend

KinesisProducer.Builder
  .default[IO](StreamNameOrArn.Name("my-stream"))
  .buildWithCloudWatch(backend = CloudWatchBackend.Otlp)
```

### Configuration

`buildWithCloudWatch` accepts the following parameters, all with sensible defaults:

| Parameter | Default | Description |
| --- | --- | --- |
| `region` | AWS SDK default region provider chain | The region to export metrics to |
| `namespace` | `kinesis4cats.producer` | The OpenTelemetry metric-name prefix (see [Namespace](#namespace)) |
| `cloudWatchNamespace` | `KinesisProducerLibrary` | The CloudWatch namespace the metrics land under (matches the KPL for drop-in dashboards) |
| `credentials` | AWS SDK default credentials provider chain | The credentials used to call CloudWatch |
| `backend` | `CloudWatchBackend.PutMetricData` | The export transport (see [Backends](#backends)) |

For non-standard endpoints (Localstack, VPC/FIPS, a custom TLS trust store), build your own `CloudWatchAsyncClient` and use `PutMetricDataMeterProvider.fromClientResource` together with `withMetrics`.

## Namespace

The `namespace` controls the prefix of the emitted metric names (e.g. `kinesis4cats.producer.user_records.received`). It is independent of the `kinesis4cats` instrumentation scope. Override it on `withMetrics` or `buildWithCloudWatch`:

```scala mdoc:compile-only
import cats.effect._
import org.typelevel.otel4s.metrics.MeterProvider

import kinesis4cats.client.producer.KinesisProducer
import kinesis4cats.models.StreamNameOrArn

KinesisProducer.Builder
  .default[IO](StreamNameOrArn.Name("my-stream"))
  .withMetrics(MeterProvider.noop[IO], namespace = "my.app.kinesis")
  .build
```
