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

import scala.concurrent.duration.Duration
import scala.jdk.CollectionConverters._

import cats.effect.IO
import cats.syntax.all._
import com.amazonaws.services.kinesis.producer.KinesisProducerConfiguration
import com.amazonaws.services.schemaregistry.common.configs.GlueSchemaRegistryConfiguration
import com.amazonaws.services.schemaregistry.utils.AWSSchemaRegistryConstants.COMPRESSION
import com.amazonaws.services.schemaregistry.utils._
import software.amazon.awssdk.services.glue.model.Compatibility

import kinesis4cats.kpl.instances.eq._
import kinesis4cats.kpl.instances.show._
import kinesis4cats.syntax.id._

class KPLCirisSpec extends munit.CatsEffectSuite {

  test(
    "It should load the environment variables the same as system properties"
  ) {
    // format: off
    val expected = new KinesisProducerConfiguration()
      .setGlueSchemaRegistryConfiguration(
        new GlueSchemaRegistryConfiguration("us-east-1")
          .runUnsafe(COMPRESSION.valueOf(BuildInfo.glueCompression))(
            _.setCompressionType(_)
          )
          .runUnsafe(BuildInfo.glueEndpoint)(_.setEndPoint(_))
          .runUnsafe(Duration(BuildInfo.glueTtl).toMillis)(
            _.setTimeToLiveMillis(_)
          )
          .runUnsafe(BuildInfo.glueCacheSize.toInt)(_.setCacheSize(_))
          .runUnsafe(AvroRecordType.valueOf(BuildInfo.glueAvroRecordType))(
            _.setAvroRecordType(_)
          )
          .runUnsafe(
            ProtobufMessageType.valueOf(BuildInfo.glueProtobufMessageType)
          )(
            _.setProtobufMessageType(_)
          )
          .runUnsafe(BuildInfo.glueRegistryName)(_.setRegistryName(_))
          .runUnsafe(Compatibility.valueOf(BuildInfo.glueCompatibility))(
            _.setCompatibilitySetting(_)
          )
          .runUnsafe(BuildInfo.glueDescription)(_.setDescription(_))
          .runUnsafe(BuildInfo.glueSchemaAutoRegistrationEnabled.toBoolean)(
            _.setSchemaAutoRegistrationEnabled(_)
          )
          .runUnsafe(
            BuildInfo.glueTags
              .split(",")
              .map(_.split(":").toList match {
                case key :: value :: Nil => key -> value
                case _                   => fail("Couldn't parse tags map")
              })
              .toMap
              .asJava
          )(_.setTags(_))
          .runUnsafe(
            BuildInfo.glueMetadata
              .split(",")
              .map(_.split(":").toList match {
                case key :: value :: Nil => key -> value
                case _                   => fail("Couldn't parse metadata map")
              })
              .toMap
              .asJava
          )(_.setMetadata(_))
          .runUnsafe(BuildInfo.glueSecondaryDeserializer)(
            _.setSecondaryDeserializer(_)
          )
          .runUnsafe(BuildInfo.glueUserAgentApp)(_.setUserAgentApp(_))
      )
      .safeTransform(BuildInfo.caCertPath)(_.setCaCertPath(_))
      .safeTransform(BuildInfo.aggregationEnabled.toBoolean)(_.setAggregationEnabled(_))
      .safeTransform(BuildInfo.aggregationMaxCount.toLong)(_.setAggregationMaxCount(_))
      .safeTransform(BuildInfo.aggregationMaxSize.toLong)(_.setAggregationMaxSize(_))
      .safeTransform(BuildInfo.cloudwatchEndpoint)(_.setCloudwatchEndpoint(_))
      .safeTransform(BuildInfo.cloudwatchPort.toLong)(_.setCloudwatchPort(_))
      .safeTransform(BuildInfo.collectionMaxCount.toLong)(_.setCollectionMaxCount(_))
      .safeTransform(BuildInfo.collectionMaxSize.toLong)(_.setCollectionMaxSize(_))
      .safeTransform(Duration(BuildInfo.connectTimeout).toMillis)(_.setConnectTimeout(_))
      .safeTransform(Duration(BuildInfo.credentialsRefreshDelay).toMillis)(
        _.setCredentialsRefreshDelay(_)
      )
      .safeTransform(BuildInfo.enableCoreDumps.toBoolean)(_.setEnableCoreDumps(_))
      .safeTransform(BuildInfo.failIfThrottled.toBoolean)(_.setFailIfThrottled(_))
      .safeTransform(BuildInfo.kinesisEndpoint)(_.setKinesisEndpoint(_))
      .safeTransform(BuildInfo.kinesisPort.toLong)(_.setKinesisPort(_))
      .safeTransform(BuildInfo.logLevel)(_.setLogLevel(_))
      .safeTransform(BuildInfo.maxConnections.toLong)(_.setMaxConnections(_))
      .safeTransform(BuildInfo.metricsGranularity)(_.setMetricsGranularity(_))
      .safeTransform(BuildInfo.metricsLevel)(_.setMetricsLevel(_))
      .safeTransform(BuildInfo.metricsNamespace)(_.setMetricsNamespace(_))
      .safeTransform(Duration(BuildInfo.metricsUploadDelay).toMillis)(_.setMetricsUploadDelay(_))
      .safeTransform(BuildInfo.minConnections.toLong)(_.setMinConnections(_))
      .safeTransform(BuildInfo.nativeExecutable)(_.setNativeExecutable(_))
      .safeTransform(BuildInfo.rateLimit.toLong)(_.setRateLimit(_))
      .safeTransform(Duration(BuildInfo.recordMaxBufferedTime).toMillis)(
        _.setRecordMaxBufferedTime(_)
      )
      .safeTransform(Duration(BuildInfo.recordTtl).toMillis)(_.setRecordTtl(_))
      .safeTransform(BuildInfo.awsRegion)(_.setRegion(_))
      .safeTransform(Duration(BuildInfo.requestTimeout).toMillis)(_.setRequestTimeout(_))
      .safeTransform(BuildInfo.tempDirectory)(_.setTempDirectory(_))
      .safeTransform(BuildInfo.verifyCertificate.toBoolean)(_.setVerifyCertificate(_))
      .safeTransform(BuildInfo.proxyHost)(_.setProxyHost(_))
      .safeTransform(BuildInfo.proxyPort.toLong)(_.setProxyPort(_))
      .safeTransform(BuildInfo.proxyUserName)(_.setProxyUserName(_))
      .safeTransform(BuildInfo.proxyPassword)(_.setProxyPassword(_))
      .safeTransform(BuildInfo.threadingModel)(_.setThreadingModel(_))
      .safeTransform(BuildInfo.threadPoolSize.toInt)(_.setThreadPoolSize(_))
      .safeTransform(Duration(BuildInfo.userRecordTimeout).toMillis)(
        _.setUserRecordTimeoutInMillis(_)
      )
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
