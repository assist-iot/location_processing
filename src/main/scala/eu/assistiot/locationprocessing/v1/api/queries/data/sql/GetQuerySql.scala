package eu.assistiot.locationprocessing.v1.api.queries.data.sql

import eu.assistiot.locationprocessing.v1.api.queries.data.MqttSettings
import eu.assistiot.locationprocessing.v1.api.queries.data.Query

import java.sql.Connection
import java.util.UUID
import scala.io.Source
import scala.util.Using

final case class QuerySql(
    id: UUID,
    name: String,
    mqttInputSettingsId: Option[UUID],
    mqttOutputSettingsId: Option[UUID],
    sql: String
)

trait GetQuerySql extends GetMqttSettingsSql {
  private val sqlFile = "/v1/sql/getQuery.sql"
  private val encoding = "UTF-8"
  private val sql: String =
    Using(Source.fromInputStream(getClass.getResourceAsStream(sqlFile))(encoding))(_.mkString).get
  private val queryNameIdx = 1
  private val queryIdxColumnLabel = "id"
  private val queryNameColumnLabel = "name"
  private val mqttInputSettingsIdColumnLabel = "mqtt_input_settings_id"
  private val mqttOutputSettingsIdColumnLabel = "mqtt_output_settings_id"
  private val sqlColumnLabel = "sql"

  def getQuerySqlHelperUnsafe(queryName: String)(implicit conn: Connection): Option[QuerySql] = {
    val statement = conn.prepareStatement(sql)
    statement.setString(queryNameIdx, queryName)
    val resultSet = statement.executeQuery()
    if (resultSet.next()) {
      Option(
        QuerySql(
          id = UUID.fromString(resultSet.getString(queryIdxColumnLabel)),
          name = resultSet.getString(queryNameColumnLabel),
          mqttInputSettingsId =
            Option(resultSet.getObject(mqttInputSettingsIdColumnLabel, classOf[UUID])),
          mqttOutputSettingsId =
            Option(resultSet.getObject(mqttOutputSettingsIdColumnLabel, classOf[UUID])),
          sql = resultSet.getString(sqlColumnLabel)
        )
      )
    } else {
      None
    }
  }

  def getQuerySqlUnsafe(name: String)(implicit conn: Connection): Option[Query] = {
    getQuerySqlHelperUnsafe(name) match {
      case Some(querySql) =>
        val inputSettings: Option[MqttSettings] = querySql.mqttInputSettingsId match {
          case Some(mqttInputSettingsId) =>
            val mqttSettings = getMqttSettingsSqlUnsafe(mqttInputSettingsId)
            mqttSettings match {
              case Some(mqttSettings) => Option(mqttSettings)
              case None               => None
            }
          case None => None
        }
        val outputSettings: Option[MqttSettings] = querySql.mqttOutputSettingsId match {
          case Some(mqttOutputSettingsId) =>
            val mqttSettings = getMqttSettingsSqlUnsafe(mqttOutputSettingsId)
            mqttSettings match {
              case Some(mqttSettings) => Option(mqttSettings)
              case None               => None
            }
          case None => None
        }
        Option(
          Query(
            name = querySql.name,
            inputSettings = inputSettings,
            outputSettings = outputSettings,
            sql = querySql.sql
          )
        )
      case None => None
    }
  }
}
