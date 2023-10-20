package eu.assistiot.locationprocessing.v1.api.queries.data.dto

final case class QueryDTO(
    name: String,
    inputSettings: Option[MqttSettingsDTO],
    outputSettings: Option[MqttSettingsDTO],
    sql: String
)
