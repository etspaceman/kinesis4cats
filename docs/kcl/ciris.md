# Ciris

Standard environment variables and system properties for configuring a @:source(kcl.src.main.scala.kinesis4cats.kcl.KCLConsumer), via [Ciris](https://cir.is/)

## Installation

```scala
libraryDependencies += "io.github.etspaceman" %% "kinesis4cats-kcl-ciris" % "@VERSION@"
```

## Usage

```scala mdoc:compile-only
import cats.effect._
import cats.syntax.all._
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient

import kinesis4cats.kcl._
import kinesis4cats.kcl.ciris.KCLCiris
import kinesis4cats.kcl.logging.instances.show._
import kinesis4cats.syntax.bytebuffer._

object MyApp extends ResourceApp.Forever {
    override def run(args: List[String]) = for {
        kinesisClient <- Resource.fromAutoCloseable(
            IO(KinesisAsyncClient.builder().build())
        )
        dynamoClient <- Resource.fromAutoCloseable(
            IO(DynamoDbAsyncClient.builder().build())
        )
        cloudWatchClient <- Resource.fromAutoCloseable(
            IO(CloudWatchAsyncClient.builder().build())
        )
        consumer <- KCLCiris.consumer[IO](
            kinesisClient, 
            dynamoClient, 
            cloudWatchClient
        ){ case records: List[CommittableRecord[IO]] => 
            records.traverse_(r => IO.println(r.data.asString)) 
        }
        _ <- consumer.run()
    } yield ()
}
```

## Configuration

The configuration below is intended to only work with single-stream consumers at this time.

### Common

| Environment Variable | System Property | Required | Default | Description |
| - | - | - | - | - |
| `KCL_APP_NAME` | `kcl.app.name` | Yes | | A unique name to give your consumer application. |
| `KCL_STREAM_NAME` | `kcl.stream.name` | Yes | | The name of the stream to consume |
| `KPL_INITIAL_POSITION`| `kcl.initial.position` | No | `LATEST` | The position to start stream consumption. Valid values are `TRIM_HORIZON`, `LATEST` and `AT_TIMESTAMP:mytimestamp` (timestamp must be parsable by `java.time.Instant.parse`) |

### Checkpoint

Currently the [CheckpointConfig](https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/checkpoint/CheckpointConfig.java) does not have any configuration, but this section is here in case it is provided at a later date.

| Environment Variable | System Property | Required | Default | Description |
| - | - | - | - | - |

### Coordinator

[CoordinatorConfig](https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/coordinator/CoordinatorConfig.java) values.

| Environment Variable | System Property | Required | Default | Description |
| - | - | - | - | - |
| `KCL_COORDINATOR_MAX_INITIALIZATION_ATTEMPTS` | `kcl.coordinator.max.initialization.attempts` | No | 20 | The maximum number of attempts to initialize the Scheduler |
| `KCL_COORDINATOR_PARENT_SHARD_POLL_INTERVAL` | `kcl.coordinator.parent.shard.poll.interval` | No | 10 seconds | Interval between polling to check for parent shard completion |
| `KCL_COORDINATOR_SKIP_SHARD_SYNC_AT_INITIALIZATION_IF_LEASES_EXIST` | `kcl.coordinator.skip.shard.sync.at.initialization.if.leases.exist` | No | false | If true, the Scheduler will skip shard sync during initialization if there are one or more leases in the lease table. |
| `KCL_COORDINATOR_SHARD_CONSUMER_DISPATCH_POLL_INTERVAL` | `kcl.coordinator.shard.consumer.dispatch.poll.interval` | No | 1 second | The duration between polling of the shard consumer for triggering state changes, and health checks. |
| `KCL_COORDINATOR_SCHEDULER_INITIALIZATION_BACKOFF_TIME` | `kcl.coordinator.scheduler.initialization.backoff.time` | No | 1 second | Interval between retrying the scheduler initialization. |

### Lease

[LeaseManagementConfig](https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/leases/LeaseManagementConfig.java) values.

| Environment Variable | System Property | Required | Default | Description |
| - | - | - | - | - |
| `KCL_LEASE_TABLE_NAME` | `kcl.lease.table.name` | No | `KCL_APP_NAME`/`kcl.app.name` | Name of the table to use in DynamoDB |
| `KCL_LEASE_WORKER_ID` | `kcl.lease.worker.id` | No | `Utils.randomUUID.toString` | Used to distinguish different workers/processes of a KCL application. |
| `KCL_LEASE_FAILOVER_TIME` | `kcl.lease.failover.time` | No | `10 seconds` | A worker which does not renew it's lease within this time interval will be regarded as having problems and it's shards will be assigned to other workers. |
| `KCL_LEASE_SHARD_SYNC_INTERVAL` | `kcl.lease.shard.sync.interval` | No | `60 seconds` | Shard sync interval |
| `KCL_LEASE_CLEANUP_LEASES_UPON_SHARD_COMPLETION` | `kcl.lease.cleanup.leases.upon.shard.completion` | No | true | Cleanup leases upon shards completion. |
| `KCL_LEASE_MAX_LEASES_FOR_WORKER` | `kcl.lease.max.leases.for.worker` | No | `Integer.MAX_VALUE` | The max number of leases (shards) this worker should process. |
| `KCL_LEASE_MAX_LEASES_TO_STEAL_AT_ONE_TIME` | `kcl.lease.max.leases.to.steal.at.one.time` | No | `1` | Max leases to steal from another worker at one time (for load balancing). |
| `KCL_LEASE_INITIAL_LEASE_TABLE_READ_CAPACITY` | `kcl.lease.initial.lease.table.read.capacity` | No | `10` | The Amazon DynamoDB table used for tracking leases will be provisioned with this read capacity if the billing mode is set to `PROVISIONED` |
| `KCL_LEASE_INITIAL_LEASE_TABLE_WRITE_CAPACITY` | `kcl.lease.initial.lease.table.write.capacity` | No | `10` | The Amazon DynamoDB table used for tracking leases will be provisioned with this write capacity if the billing mode is set to `PROVISIONED` |
| `KCL_LEASE_MAX_LEASE_RENEWAL_THREADS` | `kcl.lease.max.lease.renewal.threads` | No | `20` | The size of the thread pool to create for the lease renewer to use. |
| `KCL_LEASE_IGNORE_UNEXPECTED_CHILD_SHARDS` | `kcl.lease.ignore.unexpected.child.shards` | No | `false` | If true, ignores child shards that are received unexpectedly. |
| `KCL_LEASE_CONSISTENT_READS` | `kcl.lease.consistent.reads` | No | `false` | If true, ensures that reads from dynamo are consistent. |
| `KCL_LEASE_LIST_SHARDS_BACKOFF_TIME` | `kcl.lease.list.shards.backoff.time` | No | `1.5 seconds` | Duration to wait between list-shards calls. |
| `KCL_LEASE_MAX_LIST_SHARDS_RETRY_ATTEMPTS` | `kcl.lease.max.list.shards.retry.attempts` | No | `50` | Amount of retries possible for list-shards calls. |
| `KCL_LEASE_EPSILON` | `kcl.lease.epsilon` | No | `25 ms` | Applies variance when calculating lease expirations |
| `KCL_LEASE_DYNAMO_REQUEST_TIMEOUT` | `kcl.lease.dynamo.request.timeout` | No | `1 minute` | Timeout for requests to dynamo |
| `KCL_LEASE_BILLING_MODE` | `kcl.lease.billing.mode` | No | `PAY_PER_REQUEST` | Billing mode to use for the dynamo table upon its creation. Valid values are `PAY_PER_REQUEST` and `PROVISIONED` |
| `KCL_LEASE_LEASES_RECOVERY_AUDITOR_EXECUTION_FREQUENCY` | `kcl.lease.leases.auditor.execution.frequency` | No | `2 minutes` | Frequency of the auditor job to scan for partial leases in the lease table. |
| `KCL_LEASE_LEASES_RECOVERY_AUDITOR_INCONSISTENCY_CONFIDENCE_THRESHOLD` | `kcl.lease.leases.auditor.inconsistency.confidence.threshold` | No | `3` | Confidence threshold for the periodic auditor job to determine if leases for a stream in the lease table is inconsistent. |
| `KCL_LEASE_MAX_CACHE_MISSES_BEFORE_RELOAD` | `kcl.lease.max.cache.misses.before.reload` | No | `1000` | Amount of cache misses before reloading the lease cache. |
| `KCL_LEASE_LIST_SHARDS_CACHE_ALLOWED_AGE` | `kcl.lease.list.shards.cache.allowed.age` | No | `30 seconds` | Duration that the list shards cache is valid. |
| `KCL_LEASE_CACHE_MISS_WARNING_MODULUS` | `kcl.lease.cache.miss.warning.modulus` | No | `250` | Modulus to use against the number of cache misses before a warning is logged. |

### Lifecycle

[LifecycleConfig](https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/lifecycle/LifecycleConfig.java) values.

| Environment Variable | System Property | Required | Default | Description |
| - | - | - | - | - |
| `KCL_LIFECYCLE_LOG_WARNING_FOR_TASK_AFTER` | `kcl.lifecycle.log.warning.for.task.after` | No | None | Logs warn message if as task is held in a task for more than the set time. |
| `KCL_LIFECYCLE_TASK_BACKOFF_TIME` | `kcl.lifecycle.task.backoff.time` | No | `500 ms` | Backoff time for Amazon Kinesis Client Library tasks (in the event of failures). |
| `KCL_LIFECYCLE_READ_TIMEOUTS_TO_IGNORE_BEFORE_WARNING` | `kcl.lifecycle.read.timeouts.to.ignore.before.warning` | No | `0` | Number of consecutive ReadTimeouts to ignore before logging warning messages. |

### Metrics

[MetricsConfig](https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/metrics/MetricsConfig.java) values.

| Environment Variable | System Property | Required | Default | Description |
| - | - | - | - | - |
| `KCL_METRICS_NAMESPACE` | `kcl.metrics.namespace` | No | `KCL_APP_NAME`/`kcl.app.name` | Namespace for KCL metrics. |
| `KCL_METRICS_BUFFER_TIME` | `kcl.metrics.buffer.time` | No | `10 seconds` | Buffer metrics for at most this long before publishing to CloudWatch. |
| `KCL_METRICS_MAX_QUEUE_SIZE` | `kcl.metrics.max.queue.size` | No | `10000` | Buffer at most this many metrics before publishing to CloudWatch. |
| `KCL_METRICS_LEVEL` | `kcl.metrics.level` | No | `DETAILED` | Metrics level for which to enable CloudWatch metrics. Valid values are `DETAILED`, `SUMMARY` and `NONE` |
| `KCL_METRICS_ENABLED_DIMENSIONS` | `kcl.metrics.enabled.dimensions` | No | `Set("ALL")` | Allowed dimensions for CloudWatchMetrics. |
| `KCL_METRICS_PUBLISHER_FLUSH_BUFFER` | `kcl.metrics.publisher.flush.buffer` | No | `200` | Buffer size for MetricDatums before publishing. |

### Retrieval

[RetrievalConfig](https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/retrieval/RetrievalConfig.java) values.

| Environment Variable | System Property | Required | Default | Description |
| - | - | - | - | - |
| `KCL_RETRIEVAL_TYPE` | `kcl.retrieval.type` | Yes | `fanout` | Type of retrieval for the KCL to use. Valid values are `fanout` and `polling` |
| `KCL_RETRIEVAL_LIST_SHARDS_BACKOFF_TIME` | `kcl.retrieval.list.shards.backoff.time` | No | `1.5 seconds` | Backoff time between consecutive ListShards calls. |
| `KCL_RETRIEVAL_MAX_LIST_SHARDS_RETRY_ATTEMPTS` | `kcl.retrieval.max.list.shards.retry.attempts` | No | `50` | Max number of retries for ListShards when throttled/exception is thrown. |

#### FanOut

Configuration values if `KCL_RETRIEVAL_TYPE`/`kcl.retrieval.type` is `fanout`. Used for constructing the [FanOutConfig](https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/retrieval/fanout/FanOutConfig.java)

| Environment Variable | System Property | Required | Default | Description |
| - | - | - | - | - |
| `KCL_RETRIEVAL_FANOUT_CONSUMER_ARN` | `kcl.retrieval.fanout.consumer.arn` | No | | The ARN of an already created consumer, if this is set no automatic consumer creation will be attempted. |
| `KCL_RETRIEVAL_FANOUT_CONSUMER_NAME` | `kcl.retrieval.fanout.consumer.name` | No | `KCL_APP_NAME`/`kcl.app.name` | The name of the consumer to create. |
| `KCL_RETRIEVAL_FANOUT_MAX_DESCRIBE_STREAM_SUMMARY_RETRIES` | `kcl.retrieval.fanout.max.describe.stream.summary.retries` | No | `10` | The maximum number of retries for calling describe stream summary. |
| `KCL_RETRIEVAL_FANOUT_MAX_DESCRIBE_STREAM_CONSUMER_RETRIES` | `kcl.retrieval.fanout.max.describe.stream.consumer.retries` | No | `10` | The maximum number of retries for calling DescribeStreamConsumer. |
| `KCL_RETRIEVAL_FANOUT_REGISTER_STREAM_CONSUMER_RETRIES` | `kcl.retrieval.fanout.register.stream.consumer.retries` | No | `10` | The maximum number of retries for calling DescribeStreamConsumer. |
| `KCL_RETRIEVAL_FANOUT_RETRY_BACKOFF` | `kcl.retrieval.fanout.retry.backoff` | No | `1 second` | The maximum amount of time that will be made between failed calls. |

#### Polling

Configuration values if `KCL_RETRIEVAL_TYPE`/`kcl.retrieval.type` is `polling`. Used for constructing the [PollingConfig](https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/retrieval/polling/PollingConfig.java)

| Environment Variable | System Property | Required | Default | Description |
| - | - | - | - | - |
| `KCL_RETRIEVAL_POLLING_MAX_RECORDS` | `kcl.retrieval.polling.max.records` | No | `10000` | Max records to fetch from Kinesis in a single GetRecords call. |
| `KCL_RETRIEVAL_POLLING_IDLE_TIME_BETWEEN_READS` | `kcl.retrieval.polling.idle.time.between.reads` | No | `1 second` | The value for how long the ShardConsumer should sleep in between calls to GetRecords |
| `KCL_RETRIEVAL_POLLING_RETRY_GET_RECORDS_INTERVAL` | `kcl.retrieval.polling.retry.get.records.interval` | No | `None` | Time to wait in seconds before the worker retries to get a record. If None, retries immediately |
| `KCL_RETRIEVAL_POLLING_MAX_GET_RECORDS_THREAD_POOL` | `kcl.retrieval.polling.max.get.records.thread.pool` | No | `None` | The max number of threads in the records thread pool. If None, there is no limit. |

### Processor

[ProcessorConfig](https://github.com/awslabs/amazon-kinesis-client/blob/master/amazon-kinesis-client/src/main/java/software/amazon/kinesis/processor/ProcessorConfig.java) values.

| Environment Variable | System Property | Required | Default | Description |
| - | - | - | - | - |
| `KCL_PROCESSOR_CALL_PROCESS_RECORDS_EVEN_FOR_EMPTY_LIST` | `kcl.processor.call.process.records.even.for.empty.list` | No | `false` | If true, process records will be invoked on empty batches. |
| `KCL_PROCESSOR_RAISE_ON_ERROR` | `kcl.processor.raise.on.error` | No | `true` | If true, the application will raise an error and be killed if processRecords also throws an error. Otherwise, the error will be logged but the consumer will receive a new batch of records. |
| `KCL_PROCESSOR_CHECKPOINT_RETRIES` | `kcl.processor.checkpoint.retries` | No | `5` | Max retries for retrying commits if autoCommit is `true` |
| `KCL_PROCESSOR_CHECKPOINT_RETRY_INTERVAL` | `kcl.processor.checkpoint.retry.interval` | No | `0 seconds` | Checkpoint retry interval for retrying commits if autoCommit is `true` |
| `KCL_PROCESSOR_AUTO_COMMIT` | `kcl.processor.auto.commit` | No | `0 seconds` | If true, the record processor will automatically commit records after the user defined callback is complete. |

### Duration

Some of these environment variables are loaded as a [Scala Duration](https://www.scala-lang.org/api/2.13.10/scala/concurrent/duration/Duration$.html), and are parsed through its apply method.

For example, if you wanted to set the duration to a day, you could use:
    - `1d`
    - `1 day`
    - `24 hours`
    - `24h`
