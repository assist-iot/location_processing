package eu.assistiot.locationprocessing.v1.api

import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import eu.assistiot.locationprocessing.v1.api.metrics.MetricsRouter
import eu.assistiot.locationprocessing.v1.api.queries.ActiveQueriesState
import eu.assistiot.locationprocessing.v1.api.queries.QueryRouter
import eu.assistiot.locationprocessing.v1.api.swagger.SwaggerService
import eu.assistiot.locationprocessing.v1.shared.Metrics
import fr.davit.akka.http.metrics.core.HttpMetrics.*

import scala.concurrent.ExecutionContextExecutor
import scala.util.Failure
import scala.util.Success

object HttpServer {
  def apply(activeQueriesState: ActiveQueriesState): Behavior[Nothing] = Behaviors.setup[Nothing] {
    context =>
      context.log.info("Creating routers")
      val queryRouter = QueryRouter(activeQueriesState)(context.system)
      val router = EntrypointRouter(queryRouter, SwaggerService)
      context.log.info("Starting HTTP server")
      start(router.routes)(context.system)
      Behaviors.empty[Nothing]
  }

  private def start(routes: Route)(implicit system: ActorSystem[_]): Unit = {
    implicit val executionContext: ExecutionContextExecutor = system.executionContext
    val interface = system.settings.config.getString("v1.http.interface")
    val port = system.settings.config.getInt("v1.http.port")
    Http()
      .newMeteredServerAt(interface, port, Metrics.metricsRegistry)
      .bind(routes)
      .onComplete {
        case Success(binding) =>
          val address = binding.localAddress
          system.log.info(
            "HTTP server online at http://{}:{}/",
            address.getHostString,
            address.getPort
          )
        case Failure(error) =>
          system.log.error("Failed to bind HTTP endpoint, terminating system", error)
          system.terminate()
      }
  }
}
