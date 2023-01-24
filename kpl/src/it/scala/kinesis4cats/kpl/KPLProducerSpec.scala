package kinesis4cats.kpl

import scala.concurrent.duration._

import java.nio.ByteBuffer

import cats.effect.{IO, Resource, SyncIO}
import com.amazonaws.services.kinesis.producer._
import io.circe.syntax._

import kinesis4cats.localstack.LocalstackConfig
import kinesis4cats.localstack.aws.v1.{AwsClients, AwsCreds}

abstract class KPLProducerSpec(implicit LE: KPLProducerLogEncoders)
    extends munit.CatsEffectSuite
    with munit.CatsEffectFunFixtures {
  def fixture(
      streamName: String,
      shardCount: Int
  ): SyncIO[FunFixture[KPLProducer[IO]]] = ResourceFixture(
    KPLProducerSpec.resource(streamName, shardCount)
  )

  fixture("test1", 1).test("It should produce successfully") { producer =>
    val testData = TestData("foo", 1.0f, 2.0, true, 3, 4L)
    val testDataBB = ByteBuffer.wrap(testData.asJson.noSpaces.getBytes())

    producer.put(new UserRecord("test1", "partitionKey", testDataBB)).map {
      result =>
        assert(result.isSuccessful())
    }
  }
}

object KPLProducerSpec {

  def resource(
      streamName: String,
      shardCount: Int
  )(implicit LE: KPLProducerLogEncoders): Resource[IO, KPLProducer[IO]] = for {
    config <- LocalstackConfig.resource[IO]()
    _ <- AwsClients
      .kinesisStreamResource[IO](config, streamName, shardCount, 5, 500.millis)
    producer <- KPLProducer[IO](
      new KinesisProducerConfiguration()
        .setVerifyCertificate(false)
        .setKinesisEndpoint(config.host)
        .setCloudwatchEndpoint(config.host)
        .setCredentialsProvider(AwsCreds.LocalCredsProvider)
        .setKinesisPort(config.servicePort.toLong)
        .setCloudwatchPort(config.servicePort.toLong)
        .setMetricsLevel("none")
        .setLogLevel("warning")
        .setRegion(config.region.name)
    )
  } yield producer
}
