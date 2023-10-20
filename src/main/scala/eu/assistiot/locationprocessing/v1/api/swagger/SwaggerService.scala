package eu.assistiot.locationprocessing.v1.api.swagger

import akka.http.scaladsl.server.Route
import com.github.swagger.akka.SwaggerHttpService
import com.github.swagger.akka.model.Info
import eu.assistiot.locationprocessing.v1.api.EntrypointRouter
import eu.assistiot.locationprocessing.v1.api.metrics.MetricsRouter
import eu.assistiot.locationprocessing.v1.api.queries.QueryRouter
import eu.assistiot.locationprocessing.v1.api.utils.Routable

import scala.annotation.tailrec

object SwaggerService extends SwaggerHttpService with Routable {
  override def apiClasses: Set[Class[_]] = Set(classOf[QueryRouter], classOf[MetricsRouter])
  override def host = "localhost:8080"
  override def apiDocsPath = "api-docs"
  override def info: Info = Info(
    description = "Location Processing API",
    version = "1.0",
    title = "Location Processing API"
  )
  override def unwantedDefinitions: Seq[String] = Seq("Function1RequestContextFutureRouteResult")
  override def basePath: String = "/v1"
  override val routes: Route = super.routes
}
