package eu.assistiot.locationprocessing.v1.api.queries.data

import eu.assistiot.locationprocessing.v1.api.queries.data.dto.QueryDTO

final case class Query(
    name: String,
    inputSettings: Option[MqttSettings],
    outputSettings: Option[MqttSettings],
    sql: String
) {
  def toDTO: QueryDTO = QueryDTO(
    name = name,
    inputSettings = inputSettings.map(_.toDTO),
    outputSettings = outputSettings.map(_.toDTO),
    sql = sql
  )
}
