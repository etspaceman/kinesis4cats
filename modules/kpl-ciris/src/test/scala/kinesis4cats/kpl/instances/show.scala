/*
 * Copyright 2023-2023 etspaceman
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kinesis4cats.kpl.instances

import cats.Show
import com.amazonaws.services.kinesis.producer.KinesisProducerConfiguration
import com.amazonaws.services.schemaregistry.common.configs.GlueSchemaRegistryConfiguration

import kinesis4cats.ShowBuilder
import kinesis4cats.logging.instances.show._

object show {
  implicit val glueSchemaRegistryConfigurationShow
      : Show[GlueSchemaRegistryConfiguration] = x =>
    ShowBuilder("GlueSchemaRegistryConfiguration")
      .add("avroRecordType", x.getAvroRecordType().name())
      .add("cacheSize", x.getCacheSize())
      .add("compatibilitySetting", x.getCompatibilitySetting().name())
      .add("compressionType", x.getCompressionType().name())
      .add("description", x.getDescription())
      .add("endPoint", x.getEndPoint())
      .add("metadata", x.getMetadata())
      .add("protobufMessageType", x.getProtobufMessageType().name())
      .add("region", x.getRegion())
      .add("registryName", x.getRegistryName())
      .add("secondaryDeserializer", x.getSecondaryDeserializer())
      .add("tags", x.getTags())
      .add("timeToLiveMillis", x.getTimeToLiveMillis())
      .add("userAgentApp", x.getUserAgentApp())
      .build

  implicit val kinesisProducerConfigurationShow
      : Show[KinesisProducerConfiguration] = x =>
    ShowBuilder("KinesisProducerConfiguration")
      .add("aggregationMaxCount", x.getAggregationMaxCount())
      .add("aggregationMaxSize", x.getAggregationMaxSize())
      .add("caCertPath", x.getCaCertPath())
      .add("cloudwatchEndpoint", x.getCloudwatchEndpoint())
      .add("cloudwatchPort", x.getCloudwatchPort())
      .add("collectionMaxCount", x.getCollectionMaxCount())
      .add("collectionMaxSize", x.getCollectionMaxSize())
      .add("connectTimeout", x.getConnectTimeout())
      .add("kinesisEndpoint", x.getKinesisEndpoint())
      .add("kinesisPort", x.getKinesisPort())
      .add("logLevel", x.getLogLevel())
      .add("maxConnections", x.getMaxConnections())
      .add("metricsGranularity", x.getMetricsGranularity())
      .add("metricsLevel", x.getMetricsLevel())
      .add("metricsNamespace", x.getMetricsNamespace())
      .add("metricsUploadDelay", x.getMetricsUploadDelay())
      .add("minConnections", x.getMinConnections())
      .add("nativeExecutable", x.getNativeExecutable())
      .add("proxyHost", x.getProxyHost())
      .add("proxyPassword", x.getProxyPassword())
      .add("proxyUserName", x.getProxyUserName())
      .add("proxyPort", x.getProxyPort())
      .add("rateLimit", x.getRateLimit())
      .add("recordMaxBufferedTime", x.getRecordMaxBufferedTime())
      .add("recordTtl", x.getRecordTtl())
      .add("region", x.getRegion())
      .add("requestTimeout", x.getRequestTimeout())
      .add("tempDirectory", x.getTempDirectory())
      .add("threadPoolSize", x.getThreadPoolSize())
      .add("threadingModel", x.getThreadingModel().name())
      .add("userRecordTimeoutInMillis", x.getUserRecordTimeoutInMillis())
      .add("aggregationEnabled", x.isAggregationEnabled())
      .add("enableCoreDumps", x.isEnableCoreDumps())
      .add("failIfThrottled", x.isFailIfThrottled())
      .add("isVerifyCertificate", x.isVerifyCertificate())
      .add(
        "glueSchemaRegistryConfiguration",
        x.getGlueSchemaRegistryConfiguration()
      )
      .build

}
