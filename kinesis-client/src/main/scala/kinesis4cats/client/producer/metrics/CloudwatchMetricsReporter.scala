package kinesis4cats.client.producer.metrics

import cats.effect.Async
import fs2.Chunk
import fs2.concurrent.Channel
import org.typelevel.log4cats.StructuredLogger
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient
import software.amazon.awssdk.services.cloudwatch.model.Dimension
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataResponse

import kinesis4cats.producer.metrics

class CloudwatchMetricsReporter[F[_]](
    client: CloudWatchAsyncClient,
    namespace: String,
    encoders: metrics.MetricsReporter.LogEncoders,
    override val logger: StructuredLogger[F],
    override val config: metrics.MetricsReporter.Config[F],
    override protected val channel: Channel[F, metrics.Metric]
)(
    override protected val callback: PutMetricDataResponse => F[Unit]
)(implicit F: Async[F])
    extends metrics.MetricsReporter[F, PutMetricDataResponse](encoders) {

  override def _put(x: Chunk[metrics.Metric]): F[PutMetricDataResponse] = F
    .fromCompletableFuture(
      F.delay(
        client.putMetricData(
          PutMetricDataRequest
            .builder()
            .namespace(namespace)
            .metricData(x.toList.map(CloudwatchMetricsReporter.asAws): _*)
            .build()
        )
      )
    )

}

object CloudwatchMetricsReporter {
  private def asAwsDimension(dimension: metrics.Dimension): Dimension =
    Dimension
      .builder()
      .name(dimension.name)
      .value(dimension.value)
      .build()

  def asAws(metric: metrics.Metric): MetricDatum = MetricDatum
    .builder()
    .metricName(metric.name)
    .timestamp(metric.timestamp)
    .dimensions(metric.dimensions.map(asAwsDimension): _*)
    .unit(metric.unit.value)
    .value(metric.value)
    .build()
}
