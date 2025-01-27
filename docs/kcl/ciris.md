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

import kinesis4cats.kcl._
import kinesis4cats.kcl.ciris.KCLCiris
import kinesis4cats.syntax.bytebuffer._

object MyApp extends ResourceApp.Forever {
    override def run(args: List[String]) = for {
        consumer <- KCLCiris.consumer[IO](){ 
            case records: List[CommittableRecord[IO]] => 
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
| `KCL_LEASE_WORKER_ID` | `kcl.lease.worker.id` | No | `Utils.randomUUIDString` | Used to distinguish different workers/processes of a KCL application. |
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
| `KCL_LEASE_TABLE_DELETION_PROTECTION_ENABLED` | `kcl.lease.table.deletion.protection.enabled` | No | `false` | Whether to enable deletion protection on the DynamoDB lease table created by KCL. This does not update already existing tables. |
| `KCL_LEASE_TABLE_PITR_ENABLED` | `kcl.lease.table.pitr.enabled` | No | `false` | Whether to enable PITR (point in time recovery) on the DynamoDB lease table created by KCL. If true, this can update existing table's PITR. |
| `KCL_LEASE_IN_MEMORY_WORKER_METRICS_CAPTURE_FREQUENCY` | `kcl.lease.in.memory.worker.metrics.capture.frequency` | No | `1 second` | This defines the frequency of capturing worker metric stats in memory. |
| `KCL_LEASE_WORKER_METRICS_REPORTER_FREQ` | `kcl.lease.in.memory.worker.metrics.reporter.freq` | No | `30 seconds` | This defines the frequency of reporting worker metric stats to storage. |
| `KCL_LEASE_NO_OF_PERSISTED_METRICS_PER_WORKER_METRICS` | `kcl.lease.no.of.persisted.metrics.per.worker.metrics` | No | `10` | These are the no. of metrics that are persisted in storage in WorkerMetricStats ddb table. |
| `KCL_LEASE_DISABLE_WORKER_METRICS` | `kcl.lease.disable.worker.metrics` | No | `false` | Option to disable workerMetrics to use in lease balancing. |
| `KCL_LEASE_MAX_THROUGHPUT_PER_HOST_KBPS` | `kcl.lease.max.throughput.per.host.kbps` | No | `Double.MAX_VALUE` | Max throughput per host KBps, default is unlimited. |
| `KCL_LEASE_DAMPENING_PERCENTAGE` | `kcl.lease.dampening.percentage` | No | `60` | Percentage of value to achieve critical dampening during this case |
| `KCL_LEASE_REBALANCE_THRESHOLD_PERCENTAGE` | `kcl.lease.rebalance.threshold.percentage` | No | `10` | Percentage value used to trigger reBalance. If fleet has workers which are have metrics value more or less than 10% of fleet level average then reBalance is triggered. Leases are taken from workers with metrics value more than fleet level average. The load to take from these workers is determined by evaluating how far they are with respect to fleet level average. |
| `KCL_LEASE_ALLOW_THROUGHPUT_OVERSHOOT` | `kcl.lease.allow.throughput.overshoot` | No | `true` | The allowThroughputOvershoot flag determines whether leases should still be taken even if it causes the total assigned throughput to exceed the desired throughput to take for re-balance. Enabling this flag provides more flexibility for the LeaseAssignmentManager to explore additional assignment possibilities, which can lead to faster throughput convergence. |
| `KCL_LEASE_STABLE_WORKER_METRICS_ENTRY_CLEANUP_DURATION` | `kcl.lease.stable.worker.metrics.entry.cleanup.duration` | No | `1 day` | Duration after which workerMetricStats entry from WorkerMetricStats table will be cleaned up. When an entry's lastUpdateTime is older than staleWorkerMetricsEntryCleanupDuration from current time, entry will be removed from the table. |
| `KCL_LEASE_VARIANCE_BALANCING_FREQUENCY` | `kcl.lease.variance.balancing.frequency` | No | `3` | Frequency to perform worker variance balancing. This value is used with respect to the LAM frequency, that is every third (as default) iteration of LAM the worker variance balancing will be performed. Setting it to 1 will make varianceBalancing run on every iteration of LAM and 2 on every 2nd iteration and so on. |
| `KCL_LEASE_WORKER_METRICS_EMA_ALPHA` | `kcl.lease.worker.metrics.ema.alpha` | No | `0.5` | Alpha value used for calculating exponential moving average of worker's metricStats. Selecting higher alpha value gives more weightage to recent value and thus low smoothing effect on computed average and selecting smaller alpha values gives more weightage to past value and high smoothing effect. |
| `KCL_LEASE_WORKER_METRICS_TABLE_BILLING_MODE` | `kcl.lease.worker.metrics.table.billing.mode` | No | `PAY_PER_REQUEST` | Billing mode used to create the DDB table for worker metrics |
| `KCL_LEASE_WORKER_METRICS_TABLE_READ_CAPACITY` | `kcl.lease.worker.metrics.table.read.capacity` | No | | Read capacity to provision during DDB table creation for worker metrics, if billing mode is PROVISIONED.DDB |
| `KCL_LEASE_WORKER_METRICS_TABLE_WRITE_CAPACITY` | `kcl.lease.worker.metrics.table.write.capacity` | No | | Write capacity to provision during DDB table creation for worker metrics, if billing mode is PROVISIONED.DDB |
| `KCL_LEASE_WORKER_METRICS_TABLE_PITR_ENABLED` | `kcl.lease.worker.metrics.table.pitr.enabled` | No | false | Flag to enable Point in Time Recovery on the DDB table for worker metrics. |
| `KCL_LEASE_WORKER_METRICS_TABLE_DELETION_PROTECTION_ENABLED` | `kcl.lease.worker.metrics.table.deletion.protection.enabled` | No | false | Flag to enable deletion protection on the DDB table for worker metrics. |
| `KCL_LEASE_TABLE_TAGS` | `kcl.lease.table.tags` | No | | Tags to add to the DDB table. In the format of `key:value,key2:value2` |
| `KCL_LEASE_WORKER_METRICS_TABLE_TAGS` | `kcl.lease.worker.metrics.table.tags` | No | | Tags to add to the DDB table for worker metrics. In the format of `key:value,key2:value2` |

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

## FS2 Configuration

Standard environment variables and system properties for configuring a @:source(kcl.src.main.scala.kinesis4cats.kcl.fs2.KCLConsumerFS2), via [Ciris](https://cir.is/)

### Usage

```scala mdoc:compile-only
import cats.effect._

import kinesis4cats.kcl.fs2.ciris.KCLCirisFS2
import kinesis4cats.syntax.bytebuffer._

object MyApp extends ResourceApp.Forever {
    override def run(args: List[String]) = for {
        consumer <- KCLCirisFS2.consumer[IO]()
        _ <- consumer.stream()
            .flatMap(stream =>
                stream
                .evalTap(x => IO.println(x.data.asString))
                .through(consumer.commitRecords)
                .compile
                .resource
                .drain
            )
    } yield ()
}
```

### Configuration

The configuration below is intended to only work with single-stream consumers at this time.

| Environment Variable | System Property | Required | Default | Description |
| - | - | - | - | - |
| `KCL_FS2_QUEUE_SIZE` | `kcl.fs2.queue.size` | No | 100 | Size of the global records queue. If full, backpressure will occur. |
| `KCL_FS2_COMMIT_MAX_CHUNK` | `kcl.fs2.commit.max.chunk` | No | 1000 | Max size of records in the commit queue before commits are made. |
| `KCL_FS2_COMMIT_MAX_WAIT` | `kcl.fs2.commit.max.wait` | No | 10 seconds | Max interval between commit batch evaluation. |
| `KCL_FS2_COMMIT_MAX_RETRIES` | `kcl.fs2.commit.max.retries` | No | 5 | Max retries for running commits |
| `KCL_FS2_COMMIT_RETRY_INTERVAL` | `kcl.fs2.commit.retry.interval` | No | 0 seconds | Interval between commit retries |
