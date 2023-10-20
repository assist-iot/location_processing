package eu.assistiot.locationprocessing.v1.api.queries.data

import eu.assistiot.locationprocessing.v1.api.queries.data.dto.MqttSettingsDTO
import pl.waw.ibspan.scala_mqtt_wrapper.MqttSettings as MqttWrapperSettings
import pl.waw.ibspan.scala_mqtt_wrapper.MqttTopic as MqttWrapperTopic

final case class MqttSettings(
    username: Option[String],
    password: Option[String],
    host: String,
    port: Int,
    topics: Option[Seq[Topic]],
    format: Option[JsonFormat]
) {
  def toMqttWrapperSourceSettings: MqttWrapperSettings = MqttWrapperSettings(
    username = username.getOrElse(""),
    password = password.getOrElse(""),
    host = host,
    port = port,
    subscriptions = topics.map(_.map(_.toMqttWrapperTopic)).getOrElse(Seq.empty[MqttWrapperTopic])
  )

  def toMqttWrapperSinkSettings: MqttWrapperSettings = MqttWrapperSettings(
    username = username.getOrElse(""),
    password = password.getOrElse(""),
    host = host,
    port = port
  )

  def toDTO: MqttSettingsDTO = MqttSettingsDTO(
    username = username,
    isPasswordSet = password.isDefined,
    host = host,
    port = port,
    topics = topics,
    format = format
  )
}
