import sbtbuildinfo.BuildInfoKey
object KPLCirisSpecVars {

  val glueEnable = "true"
  val glueRegion = "us-east-1"
  val glueCompression = "ZLIB"
  val glueEndpoint = "localhost"
  val glueTtl = "2 days"
  val glueCacheSize = "201"
  val glueAvroRecordType = "GENERIC_RECORD"
  val glueProtobufMessageType = "DYNAMIC_MESSAGE"
  val glueRegistryName = "foo"
  val glueCompatibility = "BACKWARD"
  val glueDescription = "bar"
  val glueSchemaAutoRegistrationEnabled = "true"
  val glueTags = "tag1:value1,tag2:value2"
  val glueMetadata = "meta1:val1,meta2:val2"
  val glueSecondaryDeserializer = "wozzle"
  val glueUserAgentApp = "wizzle"
  val caCertPath = "./cert"
  val aggregationEnabled = "false"
  val aggregationMaxCount = "1"
  val aggregationMaxSize = "65"
  val cloudwatchEndpoint = "localhost"
  val cloudwatchPort = "8080"
  val connectTimeout = "1 second"
  val collectionMaxSize = "52225"
  val collectionMaxCount = "1"
  val credentialsRefreshDelay = "1 second"
  val enableCoreDumps = "true"
  val failIfThrottled = "true"
  val kinesisEndpoint = "localhost"
  val kinesisPort = "8080"
  val logLevel = "error"
  val maxConnections = "1"
  val metricsGranularity = "stream"
  val metricsLevel = "summary"
  val metricsNamespace = "namespace"
  val metricsUploadDelay = "1 second"
  val minConnections = "1"
  val rateLimit = "100"
  val recordMaxBufferedTime = "1 second"
  val recordTtl = "1 second"
  val awsRegion = "us-west-2"
  val requestTimeout = "1 second"
  val tempDirectory = "temp"
  val verifyCertificate = "false"
  val proxyHost = "localhost"
  val proxyPort = "8081"
  val proxyUserName = "user"
  val proxyPassword = "pass"
  val threadingModel = "POOLED"
  val threadPoolSize = "1"
  val userRecordTimeout = "1 second"
  val nativeExecutable = "foo.exe"

  val env: Map[String, String] = Map(
    "ENV_KPL_GLUE_ENABLE" -> glueEnable,
    "ENV_KPL_GLUE_REGION" -> glueRegion,
    "ENV_KPL_GLUE_COMPRESSION_TYPE" -> glueCompression,
    "ENV_KPL_GLUE_ENDPOINT" -> glueEndpoint,
    "ENV_KPL_GLUE_TTL" -> glueTtl,
    "ENV_KPL_GLUE_CACHE_SIZE" -> glueCacheSize,
    "ENV_KPL_GLUE_AVRO_RECORD_TYPE" -> glueAvroRecordType,
    "ENV_KPL_GLUE_PROTOBUF_MESSAGE_TYPE" -> glueProtobufMessageType,
    "ENV_KPL_GLUE_REGISTRY_NAME" -> glueRegistryName,
    "ENV_KPL_GLUE_COMPATIBILITY" -> glueCompatibility,
    "ENV_KPL_GLUE_DESCRIPTION" -> glueDescription,
    "ENV_KPL_GLUE_SCHEMA_AUTO_REGISTRATION_ENABLED" -> glueSchemaAutoRegistrationEnabled,
    "ENV_KPL_GLUE_TAGS" -> glueTags,
    "ENV_KPL_GLUE_METADATA" -> glueMetadata,
    "ENV_KPL_GLUE_SECONDARY_DESERIALIZER" -> glueSecondaryDeserializer,
    "ENV_KPL_GLUE_USER_AGENT_APP" -> glueUserAgentApp,
    "ENV_KPL_CA_CERT_PATH" -> caCertPath,
    "ENV_KPL_AGGREGATION_ENABLED" -> aggregationEnabled,
    "ENV_KPL_AGGREGATION_MAX_COUNT" -> aggregationMaxCount,
    "ENV_KPL_AGGREGATION_MAX_SIZE" -> aggregationMaxSize,
    "ENV_KPL_CLOUDWATCH_ENDPOINT" -> cloudwatchEndpoint,
    "ENV_KPL_CLOUDWATCH_PORT" -> cloudwatchPort,
    "ENV_KPL_COLLECTION_MAX_COUNT" -> collectionMaxCount,
    "ENV_KPL_COLLECTION_MAX_SIZE" -> collectionMaxSize,
    "ENV_KPL_CONNECT_TIMEOUT" -> connectTimeout,
    "ENV_KPL_CREDENTIALS_REFRESH_DELAY" -> credentialsRefreshDelay,
    "ENV_KPL_ENABLE_CORE_DUMPS" -> enableCoreDumps,
    "ENV_KPL_FAIL_IF_THROTTLED" -> failIfThrottled,
    "ENV_KPL_KINESIS_ENDPOINT" -> kinesisEndpoint,
    "ENV_KPL_KINESIS_PORT" -> kinesisPort,
    "ENV_KPL_LOG_LEVEL" -> logLevel,
    "ENV_KPL_MAX_CONNECTIONS" -> maxConnections,
    "ENV_KPL_METRICS_GRANULARITY" -> metricsGranularity,
    "ENV_KPL_METRICS_LEVEL" -> metricsLevel,
    "ENV_KPL_METRICS_NAMESPACE" -> metricsNamespace,
    "ENV_KPL_METRICS_UPLOAD_DELAY" -> metricsUploadDelay,
    "ENV_KPL_MIN_CONNECTIONS" -> minConnections,
    "ENV_KPL_RATE_LIMIT" -> rateLimit,
    "ENV_KPL_RECORD_MAX_BUFFERED_TIME" -> recordMaxBufferedTime,
    "ENV_KPL_RECORD_TTL" -> recordTtl,
    "ENV_KPL_AWS_REGION" -> awsRegion,
    "ENV_KPL_REQUEST_TIMEOUT" -> requestTimeout,
    "ENV_KPL_TEMP_DIRECTORY" -> tempDirectory,
    "ENV_KPL_VERIFY_CERTIFICATE" -> verifyCertificate,
    "ENV_KPL_PROXY_HOST" -> proxyHost,
    "ENV_KPL_PROXY_PORT" -> proxyPort,
    "ENV_KPL_PROXY_USER_NAME" -> proxyUserName,
    "ENV_KPL_PROXY_PASSWORD" -> proxyPassword,
    "ENV_KPL_THREADING_MODEL" -> threadingModel,
    "ENV_KPL_THREAD_POOL_SIZE" -> threadPoolSize,
    "ENV_KPL_USER_RECORD_TIMEOUT" -> userRecordTimeout,
    "ENV_KPL_NATIVE_EXECUTABLE" -> nativeExecutable
  )
  val prop: Seq[String] = Seq(
    s"-Dprop.kpl.glue.enable=$glueEnable",
    s"-Dprop.kpl.glue.region=$glueRegion",
    s"-Dprop.kpl.glue.compression.type=$glueCompression",
    s"-Dprop.kpl.glue.endpoint=$glueEndpoint",
    s"-Dprop.kpl.glue.ttl=$glueTtl",
    s"-Dprop.kpl.glue.cache.size=$glueCacheSize",
    s"-Dprop.kpl.glue.avro.record.type=$glueAvroRecordType",
    s"-Dprop.kpl.glue.protobuf.message.type=$glueProtobufMessageType",
    s"-Dprop.kpl.glue.registry.name=$glueRegistryName",
    s"-Dprop.kpl.glue.compatibility=$glueCompatibility",
    s"-Dprop.kpl.glue.description=$glueDescription",
    s"-Dprop.kpl.glue.schema.auto.registration.enabled=$glueSchemaAutoRegistrationEnabled",
    s"-Dprop.kpl.glue.tags=$glueTags",
    s"-Dprop.kpl.glue.metadata=$glueMetadata",
    s"-Dprop.kpl.glue.secondary.deserializer=$glueSecondaryDeserializer",
    s"-Dprop.kpl.glue.user.agent.app=$glueUserAgentApp",
    s"-Dprop.kpl.ca.cert.path=$caCertPath",
    s"-Dprop.kpl.aggregation.enabled=$aggregationEnabled",
    s"-Dprop.kpl.aggregation.max.count=$aggregationMaxCount",
    s"-Dprop.kpl.aggregation.max.size=$aggregationMaxSize",
    s"-Dprop.kpl.cloudwatch.endpoint=$cloudwatchEndpoint",
    s"-Dprop.kpl.cloudwatch.port=$cloudwatchPort",
    s"-Dprop.kpl.collection.max.count=$collectionMaxCount",
    s"-Dprop.kpl.collection.max.size=$collectionMaxSize",
    s"-Dprop.kpl.connect.timeout=$connectTimeout",
    s"-Dprop.kpl.credentials.refresh.delay=$credentialsRefreshDelay",
    s"-Dprop.kpl.enable.core.dumps=$enableCoreDumps",
    s"-Dprop.kpl.fail.if.throttled=$failIfThrottled",
    s"-Dprop.kpl.kinesis.endpoint=$kinesisEndpoint",
    s"-Dprop.kpl.kinesis.port=$kinesisPort",
    s"-Dprop.kpl.log.level=$logLevel",
    s"-Dprop.kpl.max.connections=$maxConnections",
    s"-Dprop.kpl.metrics.granularity=$metricsGranularity",
    s"-Dprop.kpl.metrics.level=$metricsLevel",
    s"-Dprop.kpl.metrics.namespace=$metricsNamespace",
    s"-Dprop.kpl.metrics.upload.delay=$metricsUploadDelay",
    s"-Dprop.kpl.min.connections=$minConnections",
    s"-Dprop.kpl.rate.limit=$rateLimit",
    s"-Dprop.kpl.record.max.buffered.time=$recordMaxBufferedTime",
    s"-Dprop.kpl.record.ttl=$recordTtl",
    s"-Dprop.kpl.aws.region=$awsRegion",
    s"-Dprop.kpl.request.timeout=$requestTimeout",
    s"-Dprop.kpl.temp.directory=$tempDirectory",
    s"-Dprop.kpl.verify.certificate=$verifyCertificate",
    s"-Dprop.kpl.proxy.host=$proxyHost",
    s"-Dprop.kpl.proxy.port=$proxyPort",
    s"-Dprop.kpl.proxy.user.name=$proxyUserName",
    s"-Dprop.kpl.proxy.password=$proxyPassword",
    s"-Dprop.kpl.threading.model=$threadingModel",
    s"-Dprop.kpl.thread.pool.size=$threadPoolSize",
    s"-Dprop.kpl.user.record.timeout=$userRecordTimeout",
    s"-Dprop.kpl.native.executable=$nativeExecutable"
  )

  val buildInfoKeys: Seq[BuildInfoKey] = Seq(
    "glueEnable" -> glueEnable,
    "glueRegion" -> glueRegion,
    "glueCompression" -> glueCompression,
    "glueEndpoint" -> glueEndpoint,
    "glueTtl" -> glueTtl,
    "glueCacheSize" -> glueCacheSize,
    "glueAvroRecordType" -> glueAvroRecordType,
    "glueProtobufMessageType" -> glueProtobufMessageType,
    "glueRegistryName" -> glueRegistryName,
    "glueCompatibility" -> glueCompatibility,
    "glueDescription" -> glueDescription,
    "glueSchemaAutoRegistrationEnabled" -> glueSchemaAutoRegistrationEnabled,
    "glueTags" -> glueTags,
    "glueMetadata" -> glueMetadata,
    "glueSecondaryDeserializer" -> glueSecondaryDeserializer,
    "glueUserAgentApp" -> glueUserAgentApp,
    "caCertPath" -> caCertPath,
    "aggregationEnabled" -> aggregationEnabled,
    "aggregationMaxCount" -> aggregationMaxCount,
    "aggregationMaxSize" -> aggregationMaxSize,
    "cloudwatchEndpoint" -> cloudwatchEndpoint,
    "cloudwatchPort" -> cloudwatchPort,
    "connectTimeout" -> connectTimeout,
    "collectionMaxSize" -> collectionMaxSize,
    "collectionMaxCount" -> collectionMaxCount,
    "credentialsRefreshDelay" -> credentialsRefreshDelay,
    "enableCoreDumps" -> enableCoreDumps,
    "failIfThrottled" -> failIfThrottled,
    "kinesisEndpoint" -> kinesisEndpoint,
    "kinesisPort" -> kinesisPort,
    "logLevel" -> logLevel,
    "maxConnections" -> maxConnections,
    "metricsGranularity" -> metricsGranularity,
    "metricsLevel" -> metricsLevel,
    "metricsNamespace" -> metricsNamespace,
    "metricsUploadDelay" -> metricsUploadDelay,
    "minConnections" -> minConnections,
    "rateLimit" -> rateLimit,
    "recordMaxBufferedTime" -> recordMaxBufferedTime,
    "recordTtl" -> recordTtl,
    "awsRegion" -> awsRegion,
    "requestTimeout" -> requestTimeout,
    "tempDirectory" -> tempDirectory,
    "verifyCertificate" -> verifyCertificate,
    "proxyHost" -> proxyHost,
    "proxyPort" -> proxyPort,
    "proxyUserName" -> proxyUserName,
    "proxyPassword" -> proxyPassword,
    "threadingModel" -> threadingModel,
    "threadPoolSize" -> threadPoolSize,
    "userRecordTimeout" -> userRecordTimeout,
    "nativeExecutable" -> nativeExecutable
  )
}
