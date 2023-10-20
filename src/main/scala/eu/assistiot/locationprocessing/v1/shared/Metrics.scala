package eu.assistiot.locationprocessing.v1.shared

import fr.davit.akka.http.metrics.prometheus.{Buckets, PrometheusRegistry, PrometheusSettings}
import io.prometheus.client.*

/**
 * Controller for metrics reported via Prometheus.
 *
 * Used by the main application object to register the HTTP server with the collector.
 * Also used in [[eu.assistiot.locationprocessing.v1.api.metrics.MetricsRouter]]
 */
object Metrics:
  private val bytesBuckets = Buckets(
    0, 16, 64, 256, 1024,
    4 * 1024, 16 * 1024, 64 * 1024, 256 * 1024, 1024 * 1024,
  )

  val prometheusRegistry = CollectorRegistry()

  private val metricsSettings = PrometheusSettings.default
    .withDurationConfig(Buckets(
      0.001, 0.0025, 0.005, 0.0075, 0.01,
      0.05, 0.1, 0.5, 1.0, 5.0, 10.0, 60.0,
    ))
    .withSentBytesConfig(bytesBuckets)
    .withReceivedBytesConfig(bytesBuckets)
    .withIncludePathDimension(true)
    .withIncludeMethodDimension(true)

  val queryRequests = Counter.build()
    .name("locproc_query_requests_total")
    .help("Total number of query requests")
    .labelNames("query")
    .register(prometheusRegistry)

  val queryResponses = Counter.build()
    .name("locproc_query_responses_total")
    .help("Total number of query responses")
    .labelNames("query")
    .register(prometheusRegistry)

  val metricsRegistry = PrometheusRegistry(prometheusRegistry, metricsSettings)

