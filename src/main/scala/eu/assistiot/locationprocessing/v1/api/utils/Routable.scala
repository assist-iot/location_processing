package eu.assistiot.locationprocessing.v1.api.utils

import akka.http.scaladsl.server.Route

trait Routable {
  val routes: Route
}
