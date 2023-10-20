package eu.assistiot.locationprocessing.v1.shared

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

object Config {
  val config: Config = ConfigFactory.load("application.conf")
}
