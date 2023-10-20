package eu.assistiot.locationprocessing.v1.api.queries.data.sql

import eu.assistiot.locationprocessing.v1.api.queries.data.MqttSettings
import eu.assistiot.locationprocessing.v1.shared.Uuid.generateUuid

import java.sql.Connection
import java.sql.Types
import java.util.UUID
import scala.io.Source
import scala.util.Using

trait InsertMqttSettingsSql extends InsertMqttTopicsSql with InsertJsonFormatSql {
  private val sqlFile = "/v1/sql/insertMqttSettings.sql"
  private val encoding = "UTF-8"
  private val sql =
    Using(Source.fromInputStream(getClass.getResourceAsStream(sqlFile))(encoding))(_.mkString).get
  private val idIdx = 1
  private val usernameIdx = 2
  private val passwordIdx = 3
  private val hostIdx = 4
  private val portIdx = 5
  private val jsonFormatIdIdx = 6
  private val queryIdIdx = 7

  def insertMqttSettingsSqlUnsafe(
      mqttSettings: MqttSettings,
      queryId: UUID
  )(implicit connection: Connection): UUID = {
    val id = generateUuid
    val insertSettingsStatement = connection.prepareStatement(sql)
    insertSettingsStatement.setObject(idIdx, id)
    mqttSettings.username.fold(insertSettingsStatement.setNull(usernameIdx, Types.VARCHAR)) {
      username =>
        insertSettingsStatement.setString(usernameIdx, username)
    }
    mqttSettings.password.fold(insertSettingsStatement.setNull(passwordIdx, Types.VARCHAR)) {
      password =>
        insertSettingsStatement.setString(passwordIdx, password)
    }
    insertSettingsStatement.setString(hostIdx, mqttSettings.host)
    insertSettingsStatement.setInt(portIdx, mqttSettings.port)
    mqttSettings.format.fold(insertSettingsStatement.setNull(jsonFormatIdIdx, Types.NULL)) {
      jsonFormat =>
        val jsonFormatId = insertJsonFormatSqlUnsafe(jsonFormat)
        insertSettingsStatement.setObject(jsonFormatIdIdx, jsonFormatId)
    }
    insertSettingsStatement.setObject(queryIdIdx, queryId)
    insertSettingsStatement.executeUpdate()
    mqttSettings.topics.map(insertMqttTopicsSqlUnsafe(_, id))
    id
  }
}
