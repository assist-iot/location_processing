package eu.assistiot.locationprocessing.v1.api

import akka.http.scaladsl.server.Directives.*
import akka.http.scaladsl.server.Route
import eu.assistiot.locationprocessing.v1.api.metrics.MetricsRouter
import eu.assistiot.locationprocessing.v1.api.utils.Routable
import fr.davit.akka.http.metrics.core.scaladsl.server.HttpMetricsDirectives.*

class EntrypointRouter(routers: Routable*) extends Routable {
  val routes: Route = pathPrefixLabeled("v1") {
    concat(routers.map(_.routes): _*)
  } ~ MetricsRouter().routes
}
