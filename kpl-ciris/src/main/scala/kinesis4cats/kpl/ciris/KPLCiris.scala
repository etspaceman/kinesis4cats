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

package kinesis4cats.kpl
package ciris

import scala.concurrent.duration._

import _root_.ciris._
import cats.effect.Async
import cats.effect.Resource
import cats.syntax.all._
import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.regions.Regions
import com.amazonaws.services.kinesis.producer.KinesisProducerConfiguration
import com.amazonaws.services.kinesis.producer.KinesisProducerConfiguration.ThreadingModel
import com.amazonaws.services.schemaregistry.common.configs.GlueSchemaRegistryConfiguration
import com.amazonaws.services.schemaregistry.utils._
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.services.glue.model.Compatibility

import kinesis4cats.ciris.CirisReader
import kinesis4cats.instances.ciris._
import kinesis4cats.kpl.instances.ciris._
import kinesis4cats.syntax.id._

/** Standard configuration loader of env variables and system properties for
  * [[https://github.com/awslabs/amazon-kinesis-producer/blob/master/java/amazon-kinesis-producer/src/main/java/com/amazonaws/services/kinesis/producer/KinesisProducerConfiguration.java KinesisProducerConfiguration]]
  * via [[https://cir.is/ Ciris]].
  */
object KPLCiris {

  /** Reads environment variables and system properties to load
    * [[https://github.com/awslabs/aws-glue-schema-registry/blob/master/common/src/main/java/com/amazonaws/services/schemaregistry/common/configs/GlueSchemaRegistryConfiguration.java GlueSchemaRegistryConfiguration]]
    *
    * @see
    *   [[https://docs.aws.amazon.com/glue/latest/dg/schema-registry-integrations.html]]
    *
    * @param prefix
    *   Optional prefix to apply to configuration loaders. Default None
    * @return
    *   [[https://cir.is/docs/configurations ConfigValue]] containing
    *   [[https://github.com/awslabs/aws-glue-schema-registry/blob/master/common/src/main/java/com/amazonaws/services/schemaregistry/common/configs/GlueSchemaRegistryConfiguration.java GlueSchemaRegistryConfiguration]]
    */
  def readGlueConfig(
      prefix: Option[String] = None
  ): ConfigValue[Effect, GlueSchemaRegistryConfiguration] =
    for {
      region <- CirisReader.read[Regions](List("kpl", "glue", "region"), prefix)
      compressionType <- CirisReader
        .readOptional[AWSSchemaRegistryConstants.COMPRESSION](
          List("kpl", "glue", "compression", "type"),
          prefix
        )
      endpoint <- CirisReader
        .readOptional[String](List("kpl", "glue", "endpoint"), prefix)
      ttl <- CirisReader
        .readOptional[Duration](
          List("kpl", "glue", "ttl"),
          prefix
        )
        .map(_.map(_.toMillis))
      cacheSize <- CirisReader
        .readOptional[Int](List("kpl", "glue", "cache", "size"), prefix)
      avroRecordType <- CirisReader.readOptional[AvroRecordType](
        List("kpl", "glue", "avro", "record", "type"),
        prefix
      )
      protobufMessageType <- CirisReader
        .readOptional[ProtobufMessageType](
          List("kpl", "glue", "protobuf", "message", "type"),
          prefix
        )
      registryName <- CirisReader
        .readOptional[String](List("kpl", "glue", "registry", "name"), prefix)
      compatibilitySetting <- CirisReader
        .readOptional[Compatibility](
          List("kpl", "glue", "compatibility"),
          prefix
        )
      description <- CirisReader
        .readOptional[String](List("kpl", "glue", "description"), prefix)
      schemaAutoRegistrationEnabled <- CirisReader.readOptional[Boolean](
        List("kpl", "glue", "schema", "auto", "registration", "enabled"),
        prefix
      )
      tags <- CirisReader.readOptional[java.util.Map[String, String]](
        List("kpl", "glue", "tags"),
        prefix
      )
      metadata <- CirisReader.readOptional[java.util.Map[String, String]](
        List("kpl", "glue", "metadata"),
        prefix
      )
      secondaryDeserializer <- CirisReader.readOptional[String](
        List("kpl", "glue", "secondary", "deserializer"),
        prefix
      )
      userAgentApp <- CirisReader.readOptional[String](
        List("kpl", "glue", "user", "agent", "app"),
        prefix
      )
    } yield new GlueSchemaRegistryConfiguration(region.getName())
      .maybeRunUnsafe(compressionType)(_.setCompressionType(_))
      .maybeRunUnsafe(endpoint)(_.setEndPoint(_))
      .maybeRunUnsafe(ttl)(_.setTimeToLiveMillis(_))
      .maybeRunUnsafe(cacheSize)(_.setCacheSize(_))
      .maybeRunUnsafe(avroRecordType)(_.setAvroRecordType(_))
      .maybeRunUnsafe(protobufMessageType)(_.setProtobufMessageType(_))
      .maybeRunUnsafe(registryName)(_.setRegistryName(_))
      .maybeRunUnsafe(compatibilitySetting)(_.setCompatibilitySetting(_))
      .maybeRunUnsafe(description)(_.setDescription(_))
      .maybeRunUnsafe(schemaAutoRegistrationEnabled)(
        _.setSchemaAutoRegistrationEnabled(_)
      )
      .maybeRunUnsafe(tags)(_.setTags(_))
      .maybeRunUnsafe(metadata)(_.setMetadata(_))
      .maybeRunUnsafe(secondaryDeserializer)(_.setSecondaryDeserializer(_))
      .maybeRunUnsafe(userAgentApp)(_.setUserAgentApp(_))

  /** Reads environment variables and system properties to load
    * [[https://github.com/awslabs/amazon-kinesis-producer/blob/master/java/amazon-kinesis-producer/src/main/java/com/amazonaws/services/kinesis/producer/KinesisProducerConfiguration.java KinesisProducerConfiguration]]
    *
    * @param credentialsProvider
    *   Optional
    *   [[https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/auth/AWSCredentialsProvider.html AWSCredentialsProvider (V1)]]
    *   for Kinesis interactions. Defaults to
    *   [[https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/auth/DefaultAWSCredentialsProviderChain.html DefaultAWSCredentialsProviderChain]]
    * @param metricsCredentialsProvider
    *   Optional
    *   [[https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/auth/AWSCredentialsProvider.html AWSCredentialsProvider (V1)]]
    *   for Cloudwatch interactions. Default is the value of the
    *   credentialsProvider
    * @param glueSchemaRegistryCredentialsProvider
    *   Optional
    *   [[https://sdk.amazonaws.com/java/api/2.0.0/software/amazon/awssdk/auth/credentials/AwsCredentialsProvider.html AwsCredentialsProvider (V2)]]
    *   for the
    *   [[https://docs.aws.amazon.com/glue/latest/dg/schema-registry-integrations.html Glue]]
    *   schema registry. Defaults to
    *   [[https://sdk.amazonaws.com/java/api/2.0.0/software/amazon/awssdk/auth/credentials/DefaultCredentialsProvider.html DefaultCredentialsProvider]]
    * @param prefix
    *   Optional prefix to apply to configuration loaders. Default None
    * @return
    *   [[https://cir.is/docs/configurations ConfigValue]] containing
    *   [[https://github.com/awslabs/amazon-kinesis-producer/blob/master/java/amazon-kinesis-producer/src/main/java/com/amazonaws/services/kinesis/producer/KinesisProducerConfiguration.java KinesisProducerConfiguration]]
    */
  def readKplConfig(
      credentialsProvider: Option[AWSCredentialsProvider] = None,
      metricsCredentialsProvider: Option[AWSCredentialsProvider] = None,
      glueSchemaRegistryCredentialsProvider: Option[AwsCredentialsProvider] =
        None,
      prefix: Option[String] = None
  ): ConfigValue[Effect, KinesisProducerConfiguration] = for {
    additionalMetricsDimensions <- CirisReader
      .readOptional[List[AdditionalMetricsDimension]](
        List("kpl", "additional", "metrics", "dimensions"),
        prefix
      )
    enableGlue <- CirisReader
      .readDefaulted[Boolean](List("kpl", "glue", "enable"), false, prefix)
    glueConfig <-
      if (enableGlue) readGlueConfig(prefix).map(_.some)
      else ConfigValue.default(None)
    caCertPath <- CirisReader.readOptional[String](
      List("kpl", "ca", "cert", "path"),
      prefix
    )
    aggregationEnabled <- CirisReader.readOptional[Boolean](
      List("kpl", "aggregation", "enabled"),
      prefix
    )
    aggregationMaxCount <- CirisReader.readOptional[Long](
      List("kpl", "aggregation", "max", "count"),
      prefix
    )
    aggregationMaxSize <- CirisReader.readOptional[Long](
      List("kpl", "aggregation", "max", "size"),
      prefix
    )
    cloudwatchEndpoint <- CirisReader.readOptional[String](
      List("kpl", "cloudwatch", "endpoint"),
      prefix
    )
    cloudwatchPort <- CirisReader.readOptional[Long](
      List("kpl", "cloudwatch", "port"),
      prefix
    )
    collectionMaxCount <- CirisReader.readOptional[Long](
      List("kpl", "collection", "max", "count"),
      prefix
    )
    collectionMaxSize <- CirisReader.readOptional[Long](
      List("kpl", "collection", "max", "size"),
      prefix
    )
    connectTimeout <- CirisReader
      .readOptional[Duration](
        List("kpl", "connect", "timeout"),
        prefix
      )
      .map(_.map(_.toMillis))
    credentialsRefreshDelay <- CirisReader
      .readOptional[Duration](
        List("kpl", "credentials", "refresh", "delay"),
        prefix
      )
      .map(_.map(_.toMillis))
    enableCoreDumps <- CirisReader.readOptional[Boolean](
      List("kpl", "enable", "core", "dumps"),
      prefix
    )
    failIfThrottled <- CirisReader.readOptional[Boolean](
      List("kpl", "fail", "if", "throttled"),
      prefix
    )
    kinesisEndpoint <- CirisReader.readOptional[String](
      List("kpl", "kinesis", "endpoint"),
      prefix
    )
    kinesisPort <- CirisReader.readOptional[Long](
      List("kpl", "kinesis", "port"),
      prefix
    )
    logLevel <- CirisReader.readOptional[String](
      List("kpl", "log", "level"),
      prefix
    )
    maxConnections <- CirisReader.readOptional[Long](
      List("kpl", "max", "connections"),
      prefix
    )
    metricsGranularity <- CirisReader.readOptional[String](
      List("kpl", "metrics", "granularity"),
      prefix
    )
    metricsLevel <- CirisReader.readOptional[String](
      List("kpl", "metrics", "level"),
      prefix
    )
    metricsNamespace <- CirisReader.readOptional[String](
      List("kpl", "metrics", "namespace"),
      prefix
    )
    metricsUploadDelay <- CirisReader
      .readOptional[Duration](
        List("kpl", "metrics", "upload", "delay"),
        prefix
      )
      .map(_.map(_.toMillis))
    minConnections <- CirisReader.readOptional[Long](
      List("kpl", "min", "connections"),
      prefix
    )
    nativeExecutable <- CirisReader.readOptional[String](
      List("kpl", "native", "executable"),
      prefix
    )
    rateLimit <- CirisReader.readOptional[Long](
      List("kpl", "rate", "limit"),
      prefix
    )
    recordMaxBufferedTime <- CirisReader
      .readOptional[Duration](
        List("kpl", "record", "max", "buffered", "time"),
        prefix
      )
      .map(_.map(_.toMillis))
    recordTtl <- CirisReader
      .readOptional[Duration](
        List("kpl", "record", "ttl"),
        prefix
      )
      .map(_.map(_.toMillis))
    region <- CirisReader
      .readOptional[Regions](
        List("kpl", "aws", "region"),
        prefix
      )
      .or(
        CirisReader
          .readOptional[Regions](
            List("aws", "region"),
            prefix
          )
      )
      .map(_.map(_.getName()))
    requestTimeout <- CirisReader
      .readOptional[Duration](
        List("kpl", "request", "timeout"),
        prefix
      )
      .map(_.map(_.toMillis))
    tempDirectory <- CirisReader.readOptional[String](
      List("kpl", "temp", "directory"),
      prefix
    )
    verifyCertificate <- CirisReader.readOptional[Boolean](
      List("kpl", "verify", "certificate"),
      prefix
    )
    proxyHost <- CirisReader.readOptional[String](
      List("kpl", "proxy", "host"),
      prefix
    )
    proxyPort <- CirisReader.readOptional[Long](
      List("kpl", "proxy", "port"),
      prefix
    )
    proxyUserName <- CirisReader.readOptional[String](
      List("kpl", "proxy", "user", "name"),
      prefix
    )
    proxyPassword <- CirisReader.readOptional[String](
      List("kpl", "proxy", "password"),
      prefix
    )
    threadingModel <- CirisReader.readOptional[ThreadingModel](
      List("kpl", "threading", "model"),
      prefix
    )
    threadPoolSize <- CirisReader.readOptional[Int](
      List("kpl", "thread", "pool", "size"),
      prefix
    )
    userRecordTimeout <- CirisReader
      .readOptional[Duration](
        List("kpl", "user", "record", "timeout"),
        prefix
      )
      .map(_.map(_.toMillis))
  } yield new KinesisProducerConfiguration()
    .maybeTransform(credentialsProvider)(_.setCredentialsProvider(_))
    .maybeTransform(metricsCredentialsProvider)(
      _.setMetricsCredentialsProvider(_)
    )
    .maybeTransform(glueSchemaRegistryCredentialsProvider)(
      _.setGlueSchemaRegistryCredentialsProvider(_)
    )
    .maybeTransform(glueConfig)(_.setGlueSchemaRegistryConfiguration(_))
    .maybeTransform(caCertPath)(_.setCaCertPath(_))
    .maybeTransform(aggregationEnabled)(_.setAggregationEnabled(_))
    .maybeTransform(aggregationMaxCount)(_.setAggregationMaxCount(_))
    .maybeTransform(aggregationMaxSize)(_.setAggregationMaxSize(_))
    .maybeTransform(cloudwatchEndpoint)(_.setCloudwatchEndpoint(_))
    .maybeTransform(cloudwatchPort)(_.setCloudwatchPort(_))
    .maybeTransform(collectionMaxCount)(_.setCollectionMaxCount(_))
    .maybeTransform(collectionMaxSize)(_.setCollectionMaxSize(_))
    .maybeTransform(connectTimeout)(_.setConnectTimeout(_))
    .maybeTransform(credentialsRefreshDelay)(_.setCredentialsRefreshDelay(_))
    .maybeTransform(enableCoreDumps)(_.setEnableCoreDumps(_))
    .maybeTransform(failIfThrottled)(_.setFailIfThrottled(_))
    .maybeTransform(kinesisEndpoint)(_.setKinesisEndpoint(_))
    .maybeTransform(kinesisPort)(_.setKinesisPort(_))
    .maybeTransform(logLevel)(_.setLogLevel(_))
    .maybeTransform(maxConnections)(_.setMaxConnections(_))
    .maybeTransform(metricsGranularity)(_.setMetricsGranularity(_))
    .maybeTransform(metricsLevel)(_.setMetricsLevel(_))
    .maybeTransform(metricsNamespace)(_.setMetricsNamespace(_))
    .maybeTransform(metricsUploadDelay)(_.setMetricsUploadDelay(_))
    .maybeTransform(minConnections)(_.setMinConnections(_))
    .maybeTransform(nativeExecutable)(_.setNativeExecutable(_))
    .maybeTransform(rateLimit)(_.setRateLimit(_))
    .maybeTransform(recordMaxBufferedTime)(_.setRecordMaxBufferedTime(_))
    .maybeTransform(recordTtl)(_.setRecordTtl(_))
    .maybeTransform(region)(_.setRegion(_))
    .maybeTransform(requestTimeout)(_.setRequestTimeout(_))
    .maybeTransform(tempDirectory)(_.setTempDirectory(_))
    .maybeTransform(verifyCertificate)(_.setVerifyCertificate(_))
    .maybeTransform(proxyHost)(_.setProxyHost(_))
    .maybeTransform(proxyPort)(_.setProxyPort(_))
    .maybeTransform(proxyUserName)(_.setProxyUserName(_))
    .maybeTransform(proxyPassword)(_.setProxyPassword(_))
    .maybeTransform(threadingModel)(_.setThreadingModel(_))
    .maybeTransform(threadPoolSize)(_.setThreadPoolSize(_))
    .maybeTransform(userRecordTimeout)(_.setUserRecordTimeoutInMillis(_))
    .maybeRunUnsafe(additionalMetricsDimensions) {
      case (conf, additionalDims) =>
        additionalDims.foreach(x =>
          conf.addAdditionalMetricsDimension(
            x.key,
            x.value,
            x.granularity.value
          )
        )

    }

  /** Reads environment variables and system properties to load
    * [[https://github.com/awslabs/amazon-kinesis-producer/blob/master/java/amazon-kinesis-producer/src/main/java/com/amazonaws/services/kinesis/producer/KinesisProducerConfiguration.java KinesisProducerConfiguration]]
    * as an [[cats.effect.Async Async]]
    *
    * @param credentialsProvider
    *   Optional
    *   [[https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/auth/AWSCredentialsProvider.html AWSCredentialsProvider (V1)]]
    *   for Kinesis interactions. Defaults to
    *   [[https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/auth/DefaultAWSCredentialsProviderChain.html DefaultAWSCredentialsProviderChain]]
    * @param metricsCredentialsProvider
    *   Optional
    *   [[https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/auth/AWSCredentialsProvider.html AWSCredentialsProvider (V1)]]
    *   for Cloudwatch interactions. Default is the value of the
    *   credentialsProvider
    * @param glueSchemaRegistryCredentialsProvider
    *   Optional
    *   [[https://sdk.amazonaws.com/java/api/2.0.0/software/amazon/awssdk/auth/credentials/AwsCredentialsProvider.html AwsCredentialsProvider (V2)]]
    *   for the
    *   [[https://docs.aws.amazon.com/glue/latest/dg/schema-registry-integrations.html Glue]]
    *   schema registry. Defaults to
    *   [[https://sdk.amazonaws.com/java/api/2.0.0/software/amazon/awssdk/auth/credentials/DefaultCredentialsProvider.html DefaultCredentialsProvider]]
    * @param prefix
    *   Optional prefix to apply to configuration loaders. Default None
    * @param F
    *   [[cats.effect.Async Async]]
    * @return
    *   [[cats.effect.Async Async]] containing
    *   [[https://github.com/awslabs/amazon-kinesis-producer/blob/master/java/amazon-kinesis-producer/src/main/java/com/amazonaws/services/kinesis/producer/KinesisProducerConfiguration.java KinesisProducerConfiguration]]
    */
  def loadKplConfig[F[_]](
      credentialsProvider: Option[AWSCredentialsProvider] = None,
      metricsCredentialsProvider: Option[AWSCredentialsProvider] = None,
      glueSchemaRegistryCredentialsProvider: Option[AwsCredentialsProvider] =
        None,
      prefix: Option[String] = None
  )(implicit F: Async[F]): F[KinesisProducerConfiguration] = readKplConfig(
    credentialsProvider,
    metricsCredentialsProvider,
    glueSchemaRegistryCredentialsProvider,
    prefix
  ).load[F]

  /** Reads environment variables and system properties to load
    * [[https://github.com/awslabs/amazon-kinesis-producer/blob/master/java/amazon-kinesis-producer/src/main/java/com/amazonaws/services/kinesis/producer/KinesisProducerConfiguration.java KinesisProducerConfiguration]]
    * as a [[cats.effect.Resource Resource]]
    *
    * @param credentialsProvider
    *   Optional
    *   [[https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/auth/AWSCredentialsProvider.html AWSCredentialsProvider (V1)]]
    *   for Kinesis interactions. Defaults to
    *   [[https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/auth/DefaultAWSCredentialsProviderChain.html DefaultAWSCredentialsProviderChain]]
    * @param metricsCredentialsProvider
    *   Optional
    *   [[https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/auth/AWSCredentialsProvider.html AWSCredentialsProvider (V1)]]
    *   for Cloudwatch interactions. Default is the value of the
    *   credentialsProvider
    * @param glueSchemaRegistryCredentialsProvider
    *   Optional
    *   [[https://sdk.amazonaws.com/java/api/2.0.0/software/amazon/awssdk/auth/credentials/AwsCredentialsProvider.html AwsCredentialsProvider (V2)]]
    *   for the
    *   [[https://docs.aws.amazon.com/glue/latest/dg/schema-registry-integrations.html Glue]]
    *   schema registry. Defaults to
    *   [[https://sdk.amazonaws.com/java/api/2.0.0/software/amazon/awssdk/auth/credentials/DefaultCredentialsProvider.html DefaultCredentialsProvider]]
    * @param prefix
    *   Optional prefix to apply to configuration loaders. Default None
    * @param F
    *   [[cats.effect.Async Async]]
    * @return
    *   [[cats.effect.Resource Resource]] containing
    *   [[https://github.com/awslabs/amazon-kinesis-producer/blob/master/java/amazon-kinesis-producer/src/main/java/com/amazonaws/services/kinesis/producer/KinesisProducerConfiguration.java KinesisProducerConfiguration]]
    */
  def kplConfigResource[F[_]](
      credentialsProvider: Option[AWSCredentialsProvider] = None,
      metricsCredentialsProvider: Option[AWSCredentialsProvider] = None,
      glueSchemaRegistryCredentialsProvider: Option[AwsCredentialsProvider] =
        None,
      prefix: Option[String] = None
  )(implicit F: Async[F]): Resource[F, KinesisProducerConfiguration] =
    readKplConfig(
      credentialsProvider,
      metricsCredentialsProvider,
      glueSchemaRegistryCredentialsProvider,
      prefix
    ).resource[F]

  /** Reads environment variables and system properties to load
    * [[kinesis4cats.kpl.KPLProducer KPLProducer]] as a
    * [[cats.effect.Resource Resource]]
    *
    * @param credentialsProvider
    *   Optional
    *   [[https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/auth/AWSCredentialsProvider.html AWSCredentialsProvider (V1)]]
    *   for Kinesis interactions. Defaults to
    *   [[https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/auth/DefaultAWSCredentialsProviderChain.html DefaultAWSCredentialsProviderChain]]
    * @param metricsCredentialsProvider
    *   Optional
    *   [[https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/auth/AWSCredentialsProvider.html AWSCredentialsProvider (V1)]]
    *   for Cloudwatch interactions. Default is the value of the
    *   credentialsProvider
    * @param glueSchemaRegistryCredentialsProvider
    *   Optional
    *   [[https://sdk.amazonaws.com/java/api/2.0.0/software/amazon/awssdk/auth/credentials/AwsCredentialsProvider.html AwsCredentialsProvider (V2)]]
    *   for the
    *   [[https://docs.aws.amazon.com/glue/latest/dg/schema-registry-integrations.html Glue]]
    *   schema registry. Defaults to
    *   [[https://sdk.amazonaws.com/java/api/2.0.0/software/amazon/awssdk/auth/credentials/DefaultCredentialsProvider.html DefaultCredentialsProvider]]
    * @param prefix
    *   Optional prefix to apply to configuration loaders. Default None
    * @param F
    *   [[cats.effect.Async Async]]
    * @param encoders
    *   [[kinesis4cats.kpl.KPLProducer.LogEncoders KPLProducer.LogEncoders]]
    * @return
    *   [[cats.effect.Resource Resource]] containing
    *   [[https://github.com/awslabs/amazon-kinesis-producer/blob/master/java/amazon-kinesis-producer/src/main/java/com/amazonaws/services/kinesis/producer/KinesisProducerConfiguration.java KinesisProducerConfiguration]]
    */
  def kpl[F[_]](
      credentialsProvider: Option[AWSCredentialsProvider] = None,
      metricsCredentialsProvider: Option[AWSCredentialsProvider] = None,
      glueSchemaRegistryCredentialsProvider: Option[AwsCredentialsProvider] =
        None,
      prefix: Option[String] = None,
      encoders: KPLProducer.LogEncoders = KPLProducer.LogEncoders.show
  )(implicit
      F: Async[F]
  ): Resource[F, KPLProducer[F]] = for {
    kplConfig <- kplConfigResource[F](
      credentialsProvider,
      metricsCredentialsProvider,
      glueSchemaRegistryCredentialsProvider,
      prefix
    )
    kpl <- KPLProducer[F](kplConfig, encoders = encoders)
  } yield kpl
}
