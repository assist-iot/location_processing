package eu.assistiot.locationprocessing.v1.api.queries.data.dto

import eu.assistiot.locationprocessing.v1.api.queries.data.JsonFormat
import eu.assistiot.locationprocessing.v1.api.queries.data.Topic

final case class MqttSettingsDTO(
    username: Option[String],
    isPasswordSet: Boolean,
    host: String,
    port: Int,
    topics: Option[Seq[Topic]],
    format: Option[JsonFormat]
)
