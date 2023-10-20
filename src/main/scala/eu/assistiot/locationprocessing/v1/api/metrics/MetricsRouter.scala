package eu.assistiot.locationprocessing.v1.api.metrics

import akka.http.scaladsl.server.*
import akka.http.scaladsl.server.Directives.*
import eu.assistiot.locationprocessing.v1.api.queries.QueryRouter.{QueryNotFoundResponse, QueryNotRetrievedResponse, QueryResponse}
import eu.assistiot.locationprocessing.v1.api.utils.Routable
import eu.assistiot.locationprocessing.v1.shared.Metrics
import fr.davit.akka.http.metrics.core.scaladsl.server.HttpMetricsDirectives.*
import fr.davit.akka.http.metrics.prometheus.marshalling.PrometheusMarshallers.*
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import io.swagger.v3.oas.annotations.media.{Content, Schema}
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType

@Path("/metrics")
class MetricsRouter extends Routable:
  override val routes: Route = pathPrefixLabeled("metrics") {
    pathEndOrSingleSlash {
      get {
        metrics(Metrics.metricsRegistry)
      }
    }
  }

  @GET
  @Path("/")
  @Produces(Array(MediaType.TEXT_PLAIN))
  @Operation(
    summary = "Retrieves the metrics in the Prometheus format",
    operationId = "getMetrics",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "The metrics",
      ),
      new ApiResponse(
        responseCode = "500",
        description = "Internal server error",
      )
    )
  )
  def getMetrics = metrics(Metrics.metricsRegistry)
