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

import cats.Eq
import cats.syntax.all._
import com.amazonaws.services.kinesis.producer.KinesisProducerConfiguration
import com.amazonaws.services.schemaregistry.common.configs.GlueSchemaRegistryConfiguration

import kinesis4cats.kpl.KPLProducer

object eq {
  implicit val glueSchemaRegistryConfigurationEq
      : Eq[GlueSchemaRegistryConfiguration] = (x, y) =>
    x.getAvroRecordType() == y.getAvroRecordType() &&
      x.getCacheSize() === y.getCacheSize() &&
      x.getCompatibilitySetting() == y.getCompatibilitySetting() &&
      x.getCompressionType() == y.getCompressionType() &&
      x.getDescription() === y.getDescription() &&
      x.getEndPoint() === y.getEndPoint() &&
      x.getMetadata() == y.getMetadata() &&
      x.getProtobufMessageType() == y.getProtobufMessageType() &&
      x.getRegion() === y.getRegion() &&
      x.getRegistryName() === y.getRegistryName() &&
      x.getSecondaryDeserializer() === y.getSecondaryDeserializer() &&
      x.getTags() == y.getTags() &&
      x.getTimeToLiveMillis() === y.getTimeToLiveMillis() &&
      x.getUserAgentApp() === y.getUserAgentApp()

  implicit val kinesisProducerConfigurationEq
      : Eq[KinesisProducerConfiguration] = (x, y) =>
    x.getAggregationMaxCount() === y.getAggregationMaxCount() &&
      x.getAggregationMaxSize() === y.getAggregationMaxSize() &&
      x.getCaCertPath() === y.getCaCertPath() &&
      x.getCloudwatchEndpoint() === y.getCloudwatchEndpoint() &&
      x.getCloudwatchPort() === y.getCloudwatchPort() &&
      x.getCollectionMaxCount() === y.getCollectionMaxCount() &&
      x.getCollectionMaxSize() === y.getCollectionMaxSize() &&
      x.getConnectTimeout() === y.getConnectTimeout() &&
      x.getKinesisEndpoint() === y.getKinesisEndpoint() &&
      x.getKinesisPort() === y.getKinesisPort() &&
      x.getLogLevel() === y.getLogLevel() &&
      x.getMaxConnections() === y.getMaxConnections() &&
      x.getMetricsGranularity() === y.getMetricsGranularity() &&
      x.getMetricsLevel() === y.getMetricsLevel() &&
      x.getMetricsNamespace() === y.getMetricsNamespace() &&
      x.getMetricsUploadDelay() === y.getMetricsUploadDelay() &&
      x.getMinConnections() === y.getMinConnections() &&
      x.getNativeExecutable() === y.getNativeExecutable() &&
      x.getProxyHost() === y.getProxyHost() &&
      x.getProxyPassword() === y.getProxyPassword() &&
      x.getProxyUserName() === y.getProxyUserName() &&
      x.getProxyPort() === y.getProxyPort() &&
      x.getRateLimit() === y.getRateLimit() &&
      x.getRecordMaxBufferedTime() === y.getRecordMaxBufferedTime() &&
      x.getRecordTtl() === y.getRecordTtl() &&
      x.getRegion() === y.getRegion() &&
      x.getRequestTimeout() === y.getRequestTimeout() &&
      x.getTempDirectory() === y.getTempDirectory() &&
      x.getThreadPoolSize() === y.getThreadPoolSize() &&
      x.getThreadingModel() == y.getThreadingModel() &&
      x.getUserRecordTimeoutInMillis() === y.getUserRecordTimeoutInMillis() &&
      x.isAggregationEnabled() === y.isAggregationEnabled() &&
      x.isEnableCoreDumps() === y.isEnableCoreDumps() &&
      x.isFailIfThrottled() === y.isFailIfThrottled() &&
      x.isVerifyCertificate() === y.isVerifyCertificate() &&
      Option(x.getGlueSchemaRegistryConfiguration()) ===
      Option(y.getGlueSchemaRegistryConfiguration())

  implicit val kplProducerConfigEq: Eq[KPLProducer.Config] = (x, y) =>
    x.gracefulShutdown == y.gracefulShutdown &&
      x.kpl === y.kpl
}
