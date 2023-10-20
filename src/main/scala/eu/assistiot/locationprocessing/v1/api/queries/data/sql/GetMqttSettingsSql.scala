package eu.assistiot.locationprocessing.v1.api.queries.data.sql

import eu.assistiot.locationprocessing.v1.api.queries.data.MqttSettings

import java.sql.Connection
import java.util.UUID
import scala.io.Source
import scala.util.Using

final case class MqttSettingsSql(
    id: UUID,
    username: Option[String],
    password: Option[String],
    host: String,
    port: Int,
    outputJsonFormatId: UUID
)

trait GetMqttSettingsSql extends GetMqttSettingsTopicsSql with GetMqttSettingsJsonFormatSql {
  private val sqlFile = "/v1/sql/getMqttSettings.sql"
  private val encoding = "UTF-8"
  private val sql =
    Using(Source.fromInputStream(getClass.getResourceAsStream(sqlFile))(encoding))(_.mkString).get
  private val mqttSettingsIdIdx = 1
  private val idColumnLabel = "id"
  private val usernameColumnLabel = "username"
  private val passwordColumnLabel = "password"
  private val hostColumnLabel = "host"
  private val portColumnLabel = "port"
  private val outputJsonFormatIdColumnLabel = "output_json_format_id"

  def getMqttSettingsSqlHelperUnsafe(
      mqttSettingsId: UUID
  )(implicit conn: Connection): Option[MqttSettingsSql] = {
    val statement = conn.prepareStatement(sql)
    statement.setObject(mqttSettingsIdIdx, mqttSettingsId)
    val resultSet = statement.executeQuery()
    if (resultSet.next()) {
      Some(
        MqttSettingsSql(
          id = resultSet.getObject(idColumnLabel, classOf[UUID]),
          username = Option(resultSet.getString(usernameColumnLabel)),
          password = Option(resultSet.getString(passwordColumnLabel)),
          host = resultSet.getString(hostColumnLabel),
          port = resultSet.getInt(portColumnLabel),
          outputJsonFormatId = resultSet.getObject(outputJsonFormatIdColumnLabel, classOf[UUID])
        )
      )
    } else {
      None
    }
  }

  def getMqttSettingsSqlUnsafe(
      mqttSettingsId: UUID
  )(implicit conn: Connection): Option[MqttSettings] = {
    getMqttSettingsSqlHelperUnsafe(mqttSettingsId).map { mqttSettingsSql =>
      val maybeRecordFormat = getMqttSettingsJsonFormatSqlUnsafe(mqttSettingsSql.outputJsonFormatId)
      val topics = getMqttSettingsTopicsSqlUnsafe(mqttSettingsSql.id)
      val maybeTopics = if (topics.isEmpty) None else Some(topics)
      MqttSettings(
        username = mqttSettingsSql.username,
        password = mqttSettingsSql.password,
        host = mqttSettingsSql.host,
        port = mqttSettingsSql.port,
        topics = maybeTopics,
        format = maybeRecordFormat
      )
    }
  }
}
