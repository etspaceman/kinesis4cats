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

package kinesis4cats.kpl.ciris

import scala.jdk.CollectionConverters._

import cats.effect.IO
import cats.syntax.all._
import com.amazonaws.services.kinesis.producer.KinesisProducerConfiguration
import com.amazonaws.services.schemaregistry.common.configs.GlueSchemaRegistryConfiguration
import com.amazonaws.services.schemaregistry.utils.AWSSchemaRegistryConstants.COMPRESSION
import com.amazonaws.services.schemaregistry.utils._
import software.amazon.awssdk.services.glue.model.Compatibility

import kinesis4cats.kpl.KPLProducer
import kinesis4cats.kpl.instances.eq._
import kinesis4cats.kpl.instances.show._
import kinesis4cats.syntax.id._
import kinesis4cats.syntax.string._

class KPLCirisSpec extends munit.CatsEffectSuite {

  test(
    "It should load the environment variables the same as system properties"
  ) {
    // format: off
    val expected = KPLProducer.Config(new KinesisProducerConfiguration()
      .setGlueSchemaRegistryConfiguration(
        new GlueSchemaRegistryConfiguration("us-east-1")
          .runUnsafe(COMPRESSION.valueOf(BuildInfo.kplGlueCompressionType))(
            _.setCompressionType(_)
          )
          .runUnsafe(BuildInfo.kplGlueEndpoint)(_.setEndPoint(_))
          .runUnsafe(BuildInfo.kplGlueTtl.asMillisUnsafe)(
            _.setTimeToLiveMillis(_)
          )
          .runUnsafe(BuildInfo.kplGlueCacheSize.toInt)(_.setCacheSize(_))
          .runUnsafe(AvroRecordType.valueOf(BuildInfo.kplGlueAvroRecordType))(
            _.setAvroRecordType(_)
          )
          .runUnsafe(
            ProtobufMessageType.valueOf(BuildInfo.kplGlueProtobufMessageType)
          )(
            _.setProtobufMessageType(_)
          )
          .runUnsafe(BuildInfo.kplGlueRegistryName)(_.setRegistryName(_))
          .runUnsafe(Compatibility.valueOf(BuildInfo.kplGlueCompatibility))(
            _.setCompatibilitySetting(_)
          )
          .runUnsafe(BuildInfo.kplGlueDescription)(_.setDescription(_))
          .runUnsafe(BuildInfo.kplGlueSchemaAutoRegistrationEnabled.toBoolean)(
            _.setSchemaAutoRegistrationEnabled(_)
          )
          .runUnsafe(BuildInfo.kplGlueTags.asMapUnsafe.asJava)(_.setTags(_))
          .runUnsafe(BuildInfo.kplGlueMetadata.asMapUnsafe.asJava)(_.setMetadata(_))
          .runUnsafe(BuildInfo.kplGlueSecondaryDeserializer)(
            _.setSecondaryDeserializer(_)
          )
          .runUnsafe(BuildInfo.kplGlueUserAgentApp)(_.setUserAgentApp(_))
      )
      .safeTransform(BuildInfo.kplCaCertPath)(_.setCaCertPath(_))
      .safeTransform(BuildInfo.kplAggregationEnabled.toBoolean)(_.setAggregationEnabled(_))
      .safeTransform(BuildInfo.kplAggregationMaxCount.toLong)(_.setAggregationMaxCount(_))
      .safeTransform(BuildInfo.kplAggregationMaxSize.toLong)(_.setAggregationMaxSize(_))
      .safeTransform(BuildInfo.kplCloudwatchEndpoint)(_.setCloudwatchEndpoint(_))
      .safeTransform(BuildInfo.kplCloudwatchPort.toLong)(_.setCloudwatchPort(_))
      .safeTransform(BuildInfo.kplCollectionMaxCount.toLong)(_.setCollectionMaxCount(_))
      .safeTransform(BuildInfo.kplCollectionMaxSize.toLong)(_.setCollectionMaxSize(_))
      .safeTransform(BuildInfo.kplConnectTimeout.asMillisUnsafe)(_.setConnectTimeout(_))
      .safeTransform(BuildInfo.kplCredentialsRefreshDelay.asMillisUnsafe)(
        _.setCredentialsRefreshDelay(_)
      )
      .safeTransform(BuildInfo.kplEnableCoreDumps.toBoolean)(_.setEnableCoreDumps(_))
      .safeTransform(BuildInfo.kplFailIfThrottled.toBoolean)(_.setFailIfThrottled(_))
      .safeTransform(BuildInfo.kplKinesisEndpoint)(_.setKinesisEndpoint(_))
      .safeTransform(BuildInfo.kplKinesisPort.toLong)(_.setKinesisPort(_))
      .safeTransform(BuildInfo.kplLogLevel)(_.setLogLevel(_))
      .safeTransform(BuildInfo.kplMaxConnections.toLong)(_.setMaxConnections(_))
      .safeTransform(BuildInfo.kplMetricsGranularity)(_.setMetricsGranularity(_))
      .safeTransform(BuildInfo.kplMetricsLevel)(_.setMetricsLevel(_))
      .safeTransform(BuildInfo.kplMetricsNamespace)(_.setMetricsNamespace(_))
      .safeTransform(BuildInfo.kplMetricsUploadDelay.asMillisUnsafe)(_.setMetricsUploadDelay(_))
      .safeTransform(BuildInfo.kplMinConnections.toLong)(_.setMinConnections(_))
      .safeTransform(BuildInfo.kplNativeExecutable)(_.setNativeExecutable(_))
      .safeTransform(BuildInfo.kplRateLimit.toLong)(_.setRateLimit(_))
      .safeTransform(BuildInfo.kplRecordMaxBufferedTime.asMillisUnsafe)(
        _.setRecordMaxBufferedTime(_)
      )
      .safeTransform(BuildInfo.kplRecordTtl.asMillisUnsafe)(_.setRecordTtl(_))
      .safeTransform(BuildInfo.kplAwsRegion)(_.setRegion(_))
      .safeTransform(BuildInfo.kplRequestTimeout.asMillisUnsafe)(_.setRequestTimeout(_))
      .safeTransform(BuildInfo.kplTempDirectory)(_.setTempDirectory(_))
      .safeTransform(BuildInfo.kplVerifyCertificate.toBoolean)(_.setVerifyCertificate(_))
      .safeTransform(BuildInfo.kplProxyHost)(_.setProxyHost(_))
      .safeTransform(BuildInfo.kplProxyPort.toLong)(_.setProxyPort(_))
      .safeTransform(BuildInfo.kplProxyUserName)(_.setProxyUserName(_))
      .safeTransform(BuildInfo.kplProxyPassword)(_.setProxyPassword(_))
      .safeTransform(BuildInfo.kplThreadingModel)(_.setThreadingModel(_))
      .safeTransform(BuildInfo.kplThreadPoolSize.toInt)(_.setThreadPoolSize(_))
      .safeTransform(BuildInfo.kplUserRecordTimeout.asMillisUnsafe)(
        _.setUserRecordTimeoutInMillis(_)
      ), KPLProducer.Config.GracefulShutdown(
        BuildInfo.kplGracefulShutdownFlushAttempts.toInt, 
        BuildInfo.kplGracefulShutdownFlushInterval.asFiniteDurationUnsafe
      ))
    // format: on
    for {
      kplConfigEnv <- KPLCiris.loadKplConfig[IO](prefix = Some("env"))
      kplConfigProp <- KPLCiris.loadKplConfig[IO](prefix = Some("prop"))
    } yield {
      assert(
        kplConfigEnv === kplConfigProp,
        s"envi: ${kplConfigEnv.show}\nprop: ${kplConfigProp.show}"
      )
      assert(
        kplConfigEnv === expected,
        s"read: ${kplConfigEnv.show}\nexpe: ${expected.show}"
      )
    }
  }
}
