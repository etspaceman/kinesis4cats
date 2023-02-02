import sbtbuildinfo.BuildInfoKey

object KPLCirisSpecVars {

  val propsAndEnvs = PropsAndEnvs(
    List(
      CirisUtil.propAndEnv(List("kpl", "glue", "enable"), "true"),
      CirisUtil.propAndEnv(List("kpl", "glue", "region"), "us-east-1"),
      CirisUtil.propAndEnv(
        List("kpl", "glue", "compression", "type"),
        "ZLIB"
      ),
      CirisUtil.propAndEnv(List("kpl", "glue", "endpoint"), "localhost"),
      CirisUtil.propAndEnv(List("kpl", "glue", "ttl"), "2 days"),
      CirisUtil.propAndEnv(List("kpl", "glue", "cache", "size"), "201"),
      CirisUtil.propAndEnv(
        List("kpl", "glue", "avro", "record", "type"),
        "GENERIC_RECORD"
      ),
      CirisUtil.propAndEnv(
        List("kpl", "glue", "protobuf", "message", "type"),
        "DYNAMIC_MESSAGE"
      ),
      CirisUtil
        .propAndEnv(List("kpl", "glue", "registry", "name"), "foo"),
      CirisUtil
        .propAndEnv(List("kpl", "glue", "compatibility"), "BACKWARD"),
      CirisUtil.propAndEnv(List("kpl", "glue", "description"), "bar"),
      CirisUtil
        .propAndEnv(
          List("kpl", "glue", "schema", "auto", "registration", "enabled"),
          "true"
        ),
      CirisUtil
        .propAndEnv(List("kpl", "glue", "tags"), "tag1:value1,tag2:value2"),
      CirisUtil
        .propAndEnv(List("kpl", "glue", "metadata"), "meta1:val1,meta2:val2"),
      CirisUtil.propAndEnv(
        List("kpl", "glue", "secondary", "deserializer"),
        "wozzle"
      ),
      CirisUtil
        .propAndEnv(
          List("kpl", "glue", "user", "agent", "app"),
          "wizzle"
        ),
      CirisUtil.propAndEnv(List("kpl", "ca", "cert", "path"), "./cert"),
      CirisUtil
        .propAndEnv(List("kpl", "aggregation", "enabled"), "false"),
      CirisUtil.propAndEnv(
        List("kpl", "aggregation", "max", "count"),
        "1"
      ),
      CirisUtil
        .propAndEnv(
          List("kpl", "aggregation", "max", "size"),
          "65"
        ),
      CirisUtil
        .propAndEnv(List("kpl", "cloudwatch", "endpoint"), "localhost"),
      CirisUtil.propAndEnv(List("kpl", "cloudwatch", "port"), "8080"),
      CirisUtil
        .propAndEnv(
          List("kpl", "collection", "max", "count"),
          "1"
        ),
      CirisUtil
        .propAndEnv(
          List("kpl", "collection", "max", "size"),
          "52225"
        ),
      CirisUtil.propAndEnv(List("kpl", "connect", "timeout"), "1 second"),
      CirisUtil.propAndEnv(
        List("kpl", "credentials", "refresh", "delay"),
        "1 second"
      ),
      CirisUtil
        .propAndEnv(List("kpl", "enable", "core", "dumps"), "true"),
      CirisUtil
        .propAndEnv(List("kpl", "fail", "if", "throttled"), "true"),
      CirisUtil.propAndEnv(List("kpl", "kinesis", "endpoint"), "localhost"),
      CirisUtil.propAndEnv(List("kpl", "kinesis", "port"), "8080"),
      CirisUtil.propAndEnv(List("kpl", "log", "level"), "error"),
      CirisUtil.propAndEnv(List("kpl", "max", "connections"), "1"),
      CirisUtil
        .propAndEnv(List("kpl", "metrics", "granularity"), "stream"),
      CirisUtil.propAndEnv(List("kpl", "metrics", "level"), "summary"),
      CirisUtil
        .propAndEnv(List("kpl", "metrics", "namespace"), "namespace"),
      CirisUtil
        .propAndEnv(
          List("kpl", "metrics", "upload", "delay"),
          "1 second"
        ),
      CirisUtil.propAndEnv(List("kpl", "min", "connections"), "1"),
      CirisUtil.propAndEnv(List("kpl", "rate", "limit"), "100"),
      CirisUtil.propAndEnv(
        List("kpl", "record", "max", "buffered", "time"),
        "1 second"
      ),
      CirisUtil.propAndEnv(List("kpl", "record", "ttl"), "1 second"),
      CirisUtil.propAndEnv(List("kpl", "aws", "region"), "us-west-2"),
      CirisUtil.propAndEnv(List("kpl", "request", "timeout"), "1 second"),
      CirisUtil.propAndEnv(List("kpl", "temp", "directory"), "temp"),
      CirisUtil
        .propAndEnv(List("kpl", "verify", "certificate"), "false"),
      CirisUtil.propAndEnv(List("kpl", "proxy", "host"), "localhost"),
      CirisUtil.propAndEnv(List("kpl", "proxy", "port"), "8081"),
      CirisUtil.propAndEnv(List("kpl", "proxy", "user", "name"), "user"),
      CirisUtil.propAndEnv(List("kpl", "proxy", "password"), "pass"),
      CirisUtil.propAndEnv(List("kpl", "threading", "model"), "POOLED"),
      CirisUtil
        .propAndEnv(List("kpl", "thread", "pool", "size"), "1"),
      CirisUtil
        .propAndEnv(
          List("kpl", "user", "record", "timeout"),
          "1 second"
        ),
      CirisUtil.propAndEnv(
        List("kpl", "native", "executable"),
        "foo.exe"
      )
    )
  )

  val env: Map[String, String] = propsAndEnvs.envs
  val prop: Seq[String] = propsAndEnvs.props
  val buildInfoKeys: Seq[BuildInfoKey] = propsAndEnvs.buildInfoKeys
}
