# Ciris

Standard environment variables and system properties for configuring a @:source(kpl.src.main.scala.kinesis4cats.kpl.KPLProducer), via [Ciris](https://cir.is/)

## Installation

```scala
libraryDependencies += "io.github.etspaceman" %% "kinesis4cats-kpl-ciris" % "@VERSION@"
```

## Usage

```scala mdoc:compile-only
import cats.effect.IO

import kinesis4cats.kpl.ciris.KPLCiris
import kinesis4cats.kpl.logging.instances.show._

KPLCiris.kpl[IO]()
```

## Configuration

| Environment Variable | System Property | Default | Description |
| - | - | - | - |
| `KPL_ADDITIONAL_METRICS_DIMENSIONS` | `kpl.additional.metrics.dimensions` | | Add additional, custom dimensions to the KPL metrics |
| `KPL_CA_CERT_PATH`| `kpl.ca.cert.path` | | Path to a provided cacert |
| `KPL_AGGREGATION_ENABLED` | `kpl.aggregation.enabled` | true | If true, the KPL will aggregate multiple records that share a partition key into a single record. |
| `KPL_AGGREGATION_MAX_COUNT`| `kpl.aggregation.max.count` | 4294967295 | Maximum number of items to pack into an aggregated record. Between 1-9223372036854775807 |
| `KPL_AGGREGATION_MAX_SIZE`| `kpl.aggregation.max.size` | 51200 | Maximum number of bytes to pack into an aggregated Kinesis record. Between 64-1048576 |
| `KPL_CLOUDWATCH_ENDPOINT`| `kpl.cloudwatch.endpoint` | | Custom endpoint for Cloudwatch interactions. Must accept TLS. |
| `KPL_CLOUDWATCH_PORT`| `kpl.cloudwatch.port` | 443 | Port for Cloudwatch interactions. Between 1-65535 |
| `KPL_COLLECTION_MAX_COUNT`| `kpl.collection.max.count` | 500 | Maximum number of items to pack into an PutRecords request. Between 1-500 |
| `KPL_COLLECTION_MAX_SIZE`| `kpl.collection.max.size` | 5242880 | Maximum amount of data to send with a PutRecords request. Between 52224-9223372036854775807 |
| `KPL_CONNECT_TIMEOUT`| `kpl.connect.timeout` | 6 seconds | Timeout for establishing TLS connections. Between 100 ms and 5 minutes |
| `KPL_CREDENTIALS_REFRESH_DELAY`| `kpl.credentials.refresh.delay` | 5 seconds | How often to refresh credentials. Between 1 ms and 5 minutes |
| `KPL_ENABLE_CORE_DUMPS` | `kpl.enable.core.dumps` | false | If set to true, the KPL native process will attempt to raise its own core file size soft limit to 128MB, or the hard limit, whichever is lower. No impact on Windows. |
| `KPL_FAIL_IF_THROTTLED` | `kpl.fail.if.throttled` | false | If true, throttled puts are not retried. |
| `KPL_KINESIS_ENDPOINT`| `kpl.kinesis.endpoint` | | Custom endpoint for Kinesis interactions. Must accept TLS. |
| `KPL_KINESIS_PORT`| `kpl.kinesis.port` | 443 | Port for Kinesis interactions. Between 1-65535 |
| `KPL_LOG_LEVEL`| `kpl.log.level` | info | Minimum level of logs. Valid values: trace, debug, info, warning, error |
| `KPL_MAX_CONNECTIONS`| `kpl.max.connections` | 24 | Maximum number of connections to open to the backend. HTTP requests are sent in parallel over multiple connections. Between 1-256 |
| `KPL_METRICS_GRANULARITY`| `kpl.metrics.granularity` | shard | Controls the granularity of metrics that are uploaded to CloudWatch. Valid values: global, stream, shard |
| `KPL_METRICS_LEVEL`| `kpl.metrics.level` | detailed | Controls the number of metrics that are uploaded to CloudWatch. Valid values: none, summary, detailed |
| `KPL_METRICS_NAMESPACE`| `kpl.metrics.namespace` | KinesisProducerLibrary | The namespace to upload metrics under |
| `KPL_METRICS_UPLOAD_DELAY`| `kpl.metrics.upload.delay` | 60 seconds | Delay between each metrics upload. Between 1 ms and 60 seconds |
| `KPL_MIN_CONNECTIONS`| `kpl.min.connections` | 1 | Minimum number of connections to open to the backend. Between 1-16 |
| `KPL_RATE_LIMIT`| `kpl.rate.limit` | 150 | Limits the maximum allowed put rate for a shard, as a percentage of the backend limits. Between 1-9223372036854775807 |
| `KPL_RECORD_MAX_BUFFERED_TIME`| `kpl.record.max.buffered.time` | 100ms | Maximum amount of time a record may spend being buffered before it gets sent. Between 0ms and 292271023 years |
| `KPL_RECORD_TTL`| `kpl.record.ttl` | 30 seconds | Set a time-to-live on records. Records that do not get successfully put within the limit are failed. Between 100ms and 292271023 years |
| `KPL_AWS_REGION`| `kpl.aws.region` | `AWS_REGION`/`aws.region` value | Which region to send records to, e.g. `us-east-1`. Defaults to the `AWS_REGION`/`aws.region` values if not set. These values would be set by an EC2 if deployed in AWS. |
| `KPL_REQUEST_TIMEOUT`| `kpl.request.timeout` | 6 seconds | The maximum total time elapsed between when we begin a HTTP request and eceiving all of the response. Between 100ms and 10 minutes |
| `KPL_TEMP_DIRECTORY`| `kpl.temp.directory` | 6 seconds | The maximum total time elapsed between when we begin a HTTP request and eceiving all of the response. Between 100ms and 10 minutes |
| `KPL_VERIFY_CERTIFICATE`| `kpl.verify.certificate` | true | If true, verifies SSL certificates. |
| `KPL_PROXY_HOST`| `kpl.proxy.host` | | If you have users going through a proxy, set the host here. |
| `KPL_PROXY_PORT`| `kpl.proxy.port` | 443 | If you have users going through a proxy, set the port here. Between 1-65535 |
| `KPL_PROXY_USER_NAME`| `kpl.proxy.user.name` | | If you have users going through a proxy, set the user-name here. |
| `KPL_PROXY_PASSWORD`| `kpl.proxy.password` | | If you have users going through a proxy, set the password here. |
| `KPL_THREADING_MODEL`| `kpl.threading.model` | PER_REQUEST | Sets the threading model that the native process will use. Valid values are PER_REQUEST, POOLED |
| `KPL_THREAD_POOL_SIZE`| `kpl.thread.pool.size` | | Sets the maximum number of threads that the native process' thread pool will be configured with. Must be a non-negative number. |
| `KPL_USER_RECORD_TIMEOUT`| `kpl.user.record.timeout` | | Set the value when the user submitted records will be timed out at the Java layer. Valid values are durations (e.g. `1 second`) |

### Duration

Some of these environment variables are loaded as a [Scala Duration](https://www.scala-lang.org/api/2.13.10/scala/concurrent/duration/Duration$.html), and are parsed through its apply method.

For example, if you wanted to set the duration to a day, you could use:
- `1d`
- `1 day`
- `24 hours`
- `24h`
