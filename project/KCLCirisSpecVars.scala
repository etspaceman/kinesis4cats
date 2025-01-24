import sbtbuildinfo.BuildInfoKey

object KCLCirisSpecVars {

  object Common {
    val propsAndEnvs = PropsAndEnvs(
      List(
        CirisUtil.propAndEnv(List("kcl", "app", "name"), "app-name"),
        CirisUtil.propAndEnv(List("kcl", "stream", "name"), "stream-name"),
        CirisUtil.propAndEnv(List("kcl", "initial", "position"), "TRIM_HORIZON")
      )
    )
  }

  object Coordinator {
    val propsAndEnvs = PropsAndEnvs(
      List(
        CirisUtil.propAndEnv(
          List("kcl", "coordinator", "max", "initialization", "attempts"),
          "21"
        ),
        CirisUtil.propAndEnv(
          List("kcl", "coordinator", "parent", "shard", "poll", "interval"),
          "15 seconds"
        ),
        CirisUtil.propAndEnv(
          List(
            "kcl",
            "coordinator",
            "skip",
            "shard",
            "sync",
            "at",
            "initialization",
            "if",
            "leases",
            "exist"
          ),
          "true"
        ),
        CirisUtil.propAndEnv(
          List(
            "kcl",
            "coordinator",
            "shard",
            "consumer",
            "dispatch",
            "poll",
            "interval"
          ),
          "2 seconds"
        ),
        CirisUtil.propAndEnv(
          List(
            "kcl",
            "coordinator",
            "scheduler",
            "initialization",
            "backoff",
            "time"
          ),
          "2 seconds"
        )
      )
    )
  }

  object Lease {
    val propsAndEnvs = PropsAndEnvs(
      List(
        CirisUtil
          .propAndEnv(List("kcl", "lease", "table", "name"), "table-name"),
        CirisUtil.propAndEnv(List("kcl", "lease", "worker", "id"), "worker-id"),
        CirisUtil
          .propAndEnv(List("kcl", "lease", "failover", "time"), "15 seconds"),
        CirisUtil
          .propAndEnv(
            List("kcl", "lease", "shard", "sync", "interval"),
            "2 minutes"
          ),
        CirisUtil.propAndEnv(
          List(
            "kcl",
            "lease",
            "cleanup",
            "leases",
            "upon",
            "shard",
            "completion"
          ),
          "false"
        ),
        CirisUtil.propAndEnv(
          List("kcl", "lease", "max", "leases", "for", "worker"),
          "5"
        ),
        CirisUtil.propAndEnv(
          List(
            "kcl",
            "lease",
            "max",
            "leases",
            "to",
            "steal",
            "at",
            "one",
            "time"
          ),
          "2"
        ),
        CirisUtil.propAndEnv(
          List("kcl", "lease", "initial", "lease", "table", "read", "capacity"),
          "20"
        ),
        CirisUtil.propAndEnv(
          List(
            "kcl",
            "lease",
            "initial",
            "lease",
            "table",
            "write",
            "capacity"
          ),
          "20"
        ),
        CirisUtil.propAndEnv(
          List("kcl", "lease", "max", "lease", "renewal", "threads"),
          "15"
        ),
        CirisUtil.propAndEnv(
          List("kcl", "lease", "ignore", "unexpected", "child", "shards"),
          "true"
        ),
        CirisUtil
          .propAndEnv(List("kcl", "lease", "consistent", "reads"), "true"),
        CirisUtil.propAndEnv(
          List("kcl", "lease", "list", "shards", "backoff", "time"),
          "1 second"
        ),
        CirisUtil.propAndEnv(
          List("kcl", "lease", "max", "list", "shards", "retry", "attempts"),
          "10"
        ),
        CirisUtil.propAndEnv(List("kcl", "lease", "epsilon"), "30 ms"),
        CirisUtil
          .propAndEnv(
            List("kcl", "lease", "dynamo", "request", "timeout"),
            "2 minutes"
          ),
        CirisUtil
          .propAndEnv(List("kcl", "lease", "billing", "mode"), "PROVISIONED"),
        CirisUtil.propAndEnv(
          List(
            "kcl",
            "lease",
            "leases",
            "recovery",
            "auditor",
            "execution",
            "frequency"
          ),
          "1 hour"
        ),
        CirisUtil.propAndEnv(
          List(
            "kcl",
            "lease",
            "leases",
            "recovery",
            "auditor",
            "inconsistency",
            "confidence",
            "threshold"
          ),
          "2"
        ),
        CirisUtil.propAndEnv(
          List("kcl", "lease", "max", "cache", "misses", "before", "reload"),
          "1001"
        ),
        CirisUtil.propAndEnv(
          List("kcl", "lease", "list", "shards", "cache", "allowed", "age"),
          "10 seconds"
        ),
        CirisUtil.propAndEnv(
          List("kcl", "lease", "cache", "miss", "warning", "modulus"),
          "251"
        ),
        CirisUtil.propAndEnv(
          List("kcl", "lease", "table", "deletion", "protection", "enabled"),
          "true"
        ),
        CirisUtil.propAndEnv(
          List("kcl", "lease", "table", "pitr", "enabled"),
          "true"
        ),
        CirisUtil.propAndEnv(
          List(
            "kcl",
            "lease",
            "in",
            "memory",
            "worker",
            "metrics",
            "capture",
            "frequency"
          ),
          "2 seconds"
        ),
        CirisUtil.propAndEnv(
          List("kcl", "lease", "worker", "metrics", "reporter", "freq"),
          "10 seconds"
        ),
        CirisUtil.propAndEnv(
          List(
            "kcl",
            "lease",
            "no",
            "of",
            "persisted",
            "metrics",
            "per",
            "worker",
            "metrics"
          ),
          "5"
        ),
        CirisUtil.propAndEnv(
          List(
            "kcl",
            "lease",
            "disable",
            "worker",
            "metrics"
          ),
          "true"
        ),
        CirisUtil.propAndEnv(
          List(
            "kcl",
            "lease",
            "max",
            "throughput",
            "per",
            "host",
            "kbps"
          ),
          "1.0"
        ),
        CirisUtil.propAndEnv(
          List(
            "kcl",
            "lease",
            "dampening",
            "percentage"
          ),
          "30"
        ),
        CirisUtil.propAndEnv(
          List(
            "kcl",
            "lease",
            "rebalance",
            "threshold",
            "percentage"
          ),
          "5"
        ),
        CirisUtil.propAndEnv(
          List(
            "kcl",
            "lease",
            "allow",
            "throughput",
            "overshoot"
          ),
          "false"
        ),
        CirisUtil.propAndEnv(
          List(
            "kcl",
            "lease",
            "stale",
            "worker",
            "metrics",
            "entry",
            "cleanup",
            "duration"
          ),
          "12 hours"
        ),
        CirisUtil.propAndEnv(
          List(
            "kcl",
            "lease",
            "variance",
            "balancing",
            "frequency"
          ),
          "2"
        ),
        CirisUtil.propAndEnv(
          List(
            "kcl",
            "lease",
            "worker",
            "metrics",
            "ema",
            "alpha"
          ),
          "0.3"
        ),
        CirisUtil.propAndEnv(
          List(
            "kcl",
            "lease",
            "worker",
            "metrics",
            "table",
            "billing",
            "mode"
          ),
          "PROVISIONED"
        ),
        CirisUtil.propAndEnv(
          List(
            "kcl",
            "lease",
            "worker",
            "metrics",
            "table",
            "read",
            "capacity"
          ),
          "20"
        ),
        CirisUtil.propAndEnv(
          List(
            "kcl",
            "lease",
            "worker",
            "metrics",
            "table",
            "write",
            "capacity"
          ),
          "20"
        ),
        CirisUtil.propAndEnv(
          List(
            "kcl",
            "lease",
            "worker",
            "metrics",
            "table",
            "point",
            "in",
            "time",
            "recovery",
            "enabled"
          ),
          "true"
        ),
        CirisUtil.propAndEnv(
          List(
            "kcl",
            "lease",
            "worker",
            "metrics",
            "table",
            "deletion",
            "protection",
            "enabled"
          ),
          "true"
        ),
        CirisUtil.propAndEnv(
          List("kcl", "lease", "table", "tags"),
          "tag1:value1,tag2:value2"
        ),
        CirisUtil.propAndEnv(
          List("kcl", "lease", "worker", "metrics", "table", "tags"),
          "tag1:value1,tag2:value2"
        ),
        CirisUtil.propAndEnv(
          List(
            "kcl",
            "lease",
            "recovery",
            "auditor",
            "execution",
            "frequency"
          ),
          "1 hour"
        )
      )
    )
  }

  object Lifecycle {
    val propsAndEnvs = PropsAndEnvs(
      List(
        CirisUtil.propAndEnv(
          List("kcl", "lifecycle", "log", "warning", "for", "task", "after"),
          "1 second"
        ),
        CirisUtil
          .propAndEnv(
            List("kcl", "lifecycle", "task", "backoff", "time"),
            "1 second"
          ),
        CirisUtil.propAndEnv(
          List(
            "kcl",
            "lifecycle",
            "read",
            "timeouts",
            "to",
            "ignore",
            "before",
            "warning"
          ),
          "1"
        )
      )
    )
  }

  object Metrics {
    val propsAndEnvs = PropsAndEnvs(
      List(
        CirisUtil.propAndEnv(List("kcl", "metrics", "namespace"), "namespace"),
        CirisUtil
          .propAndEnv(List("kcl", "metrics", "buffer", "time"), "5 seconds"),
        CirisUtil
          .propAndEnv(List("kcl", "metrics", "max", "queue", "size"), "10001"),
        CirisUtil.propAndEnv(List("kcl", "metrics", "level"), "SUMMARY"),
        CirisUtil
          .propAndEnv(List("kcl", "metrics", "enabled", "dimensions"), "ALL"),
        CirisUtil.propAndEnv(
          List("kcl", "metrics", "publisher", "flush", "buffer"),
          "201"
        )
      )
    )
  }

  object Retrieval {
    object Polling {
      val propsAndEnvs = PropsAndEnvs(
        List(
          CirisUtil.propAndEnv(
            List("kcl", "stream", "name"),
            "stream-name",
            "polling.prop.",
            "POLLING_ENV_",
            Some("polling")
          ),
          CirisUtil.propAndEnv(
            List("kcl", "app", "name"),
            "app-name",
            "polling.prop.",
            "POLLING_ENV_",
            Some("polling")
          ),
          CirisUtil.propAndEnv(
            List("kcl", "initial", "position"),
            "TRIM_HORIZON",
            "polling.prop.",
            "POLLING_ENV_",
            Some("polling")
          ),
          CirisUtil.propAndEnv(
            List("kcl", "retrieval", "type"),
            "polling",
            "polling.prop.",
            "POLLING_ENV_",
            Some("polling")
          ),
          CirisUtil.propAndEnv(
            List("kcl", "retrieval", "polling", "max", "records"),
            "10000",
            "polling.prop.",
            "POLLING_ENV_",
            Some("polling")
          ),
          CirisUtil.propAndEnv(
            List(
              "kcl",
              "retrieval",
              "polling",
              "idle",
              "time",
              "between",
              "reads"
            ),
            "2 seconds",
            "polling.prop.",
            "POLLING_ENV_",
            Some("polling")
          ),
          CirisUtil.propAndEnv(
            List(
              "kcl",
              "retrieval",
              "polling",
              "retry",
              "get",
              "records",
              "interval"
            ),
            "1 second",
            "polling.prop.",
            "POLLING_ENV_",
            Some("polling")
          ),
          CirisUtil.propAndEnv(
            List(
              "kcl",
              "retrieval",
              "polling",
              "max",
              "get",
              "records",
              "thread",
              "pool"
            ),
            "1",
            "polling.prop.",
            "POLLING_ENV_"
          ),
          CirisUtil.propAndEnv(
            List(
              "kcl",
              "retrieval",
              "polling",
              "use",
              "polling",
              "config",
              "idle",
              "time",
              "value"
            ),
            "true",
            "polling.prop.",
            "POLLING_ENV_",
            Some("polling")
          ),
          CirisUtil.propAndEnv(
            List("kcl", "retrieval", "list", "shards", "backoff", "time"),
            "2 seconds",
            "polling.prop.",
            "POLLING_ENV_",
            Some("polling")
          ),
          CirisUtil.propAndEnv(
            List(
              "kcl",
              "retrieval",
              "max",
              "list",
              "shards",
              "retry",
              "attempts"
            ),
            "51",
            "polling.prop.",
            "POLLING_ENV_",
            Some("polling")
          )
        )
      )
    }
    object FanOut {
      val propsAndEnvs = PropsAndEnvs(
        List(
          CirisUtil.propAndEnv(
            List("kcl", "stream", "name"),
            "stream-name",
            "fanout.prop.",
            "FANOUT_ENV_",
            Some("fanout")
          ),
          CirisUtil.propAndEnv(
            List("kcl", "app", "name"),
            "app-name",
            "fanout.prop.",
            "FANOUT_ENV_",
            Some("fanout")
          ),
          CirisUtil.propAndEnv(
            List("kcl", "initial", "position"),
            "TRIM_HORIZON",
            "fanout.prop.",
            "FANOUT_ENV_",
            Some("fanout")
          ),
          CirisUtil.propAndEnv(
            List("kcl", "retrieval", "type"),
            "fanout",
            "fanout.prop.",
            "FANOUT_ENV_",
            Some("fanout")
          ),
          CirisUtil.propAndEnv(
            List("kcl", "retrieval", "fanout", "consumer", "arn"),
            "arn:aws:kinesis:us-east-1:123456789012:stream/stream-name/consumer/consumer-name:1675445613",
            "fanout.prop.",
            "FANOUT_ENV_",
            Some("fanout")
          ),
          CirisUtil.propAndEnv(
            List("kcl", "retrieval", "fanout", "consumer", "name"),
            "consumer-name",
            "fanout.prop.",
            "FANOUT_ENV_",
            Some("fanout")
          ),
          CirisUtil.propAndEnv(
            List(
              "kcl",
              "retrieval",
              "fanout",
              "max",
              "describe",
              "stream",
              "summary",
              "retries"
            ),
            "5",
            "fanout.prop.",
            "FANOUT_ENV_",
            Some("fanout")
          ),
          CirisUtil.propAndEnv(
            List(
              "kcl",
              "retrieval",
              "fanout",
              "max",
              "describe",
              "stream",
              "consumer",
              "retries"
            ),
            "5",
            "fanout.prop.",
            "FANOUT_ENV_",
            Some("fanout")
          ),
          CirisUtil.propAndEnv(
            List(
              "kcl",
              "retrieval",
              "fanout",
              "register",
              "stream",
              "consumer",
              "retries"
            ),
            "5",
            "fanout.prop.",
            "FANOUT_ENV_",
            Some("fanout")
          ),
          CirisUtil.propAndEnv(
            List("kcl", "retrieval", "fanout", "retry", "backoff"),
            "2 seconds",
            "fanout.prop.",
            "FANOUT_ENV_",
            Some("fanout")
          ),
          CirisUtil.propAndEnv(
            List("kcl", "retrieval", "list", "shards", "backoff", "time"),
            "3 seconds",
            "fanout.prop.",
            "FANOUT_ENV_",
            Some("fanout")
          ),
          CirisUtil.propAndEnv(
            List(
              "kcl",
              "retrieval",
              "max",
              "list",
              "shards",
              "retry",
              "attempts"
            ),
            "2",
            "fanout.prop.",
            "FANOUT_ENV_",
            Some("fanout")
          )
        )
      )
    }

    val propsAndEnvs = FanOut.propsAndEnvs ++ Polling.propsAndEnvs
  }

  object Processor {
    val propsAndEnvs = PropsAndEnvs(
      List(
        CirisUtil
          .propAndEnv(
            List("kcl", "processor", "shard", "end", "timeout"),
            "1 second"
          ),
        CirisUtil
          .propAndEnv(List("kcl", "processor", "checkpoint", "retries"), "4"),
        CirisUtil.propAndEnv(
          List("kcl", "processor", "checkpoint", "retry", "interval"),
          "1 second"
        ),
        CirisUtil
          .propAndEnv(List("kcl", "processor", "auto", "commit"), "false"),
        CirisUtil.propAndEnv(
          List(
            "kcl",
            "processor",
            "call",
            "process",
            "records",
            "even",
            "for",
            "empty",
            "list"
          ),
          "true"
        ),
        CirisUtil.propAndEnv(
          List("kcl", "processor", "raise", "on", "error"),
          "false"
        )
      )
    )
  }

  val propsAndEnvs =
    Common.propsAndEnvs ++
      Coordinator.propsAndEnvs ++
      Lease.propsAndEnvs ++
      Lifecycle.propsAndEnvs ++
      Metrics.propsAndEnvs ++
      Retrieval.propsAndEnvs ++
      Processor.propsAndEnvs

  val env: Map[String, String] = propsAndEnvs.envs
  val prop: Seq[String] = propsAndEnvs.props
  val buildInfoKeys: Seq[BuildInfoKey] = propsAndEnvs.buildInfoKeys
}
