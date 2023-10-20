package eu.assistiot.locationprocessing.v1

import akka.actor.CoordinatedShutdown
import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import eu.assistiot.locationprocessing.v1.api.HttpServer
import eu.assistiot.locationprocessing.v1.api.queries.ActiveQueriesState
import eu.assistiot.locationprocessing.v1.shared.Config.config
import eu.assistiot.locationprocessing.v1.shared.DatabaseHelper

import scala.util.Failure
import scala.util.Success

object V1 {
  def apply(): Behavior[Nothing] = Behaviors.setup[Nothing] { context =>
    ActiveQueriesState(context.system) match {
      case Success(activeQueriesState) =>
        context.watch[Nothing](context.spawn[Nothing](HttpServer(activeQueriesState), "httpServer"))
        Behaviors.empty[Nothing]
      case Failure(exception) =>
        context.log.error(exception.getMessage)
        Behaviors.stopped[Nothing]
    }
  }

  def start(): Unit = {
    DatabaseHelper.initialize(config)
    val system = ActorSystem[Nothing](V1(), "v1")
    CoordinatedShutdown(system).addJvmShutdownHook(() => {
      DatabaseHelper.closeConnectionPools()
    })
  }
}
