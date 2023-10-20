package eu.assistiot.locationprocessing.v1.api.queries.data.sql

import eu.assistiot.locationprocessing.v1.api.queries.data.JsonFormat

import java.sql.Connection
import java.util.UUID
import scala.io.Source
import scala.util.Failure
import scala.util.Success
import scala.util.Using

trait GetMqttSettingsJsonFormatSql {
  private val sqlFile = "/v1/sql/getMqttSettingsJsonFormat.sql"
  private val encoding = "UTF-8"
  private val sql =
    Using(Source.fromInputStream(getClass.getResourceAsStream(sqlFile))(encoding))(_.mkString).get
  private val mqttSettingsIdIdx = 1
  private val recordFormatColumnLabel = "record_format"
  private val showHeaderColumnLabel = "show_header"
  private val wrapSingleColumnColumnLabel = "wrap_single_column"

  def getMqttSettingsJsonFormatSqlUnsafe(
      mqttSettingsId: UUID
  )(implicit conn: Connection): Option[JsonFormat] = {
    val statement = conn.prepareStatement(sql)
    statement.setObject(mqttSettingsIdIdx, mqttSettingsId)
    val resultSet = statement.executeQuery()
    if (resultSet.next()) {
      val recordFormat =
        JsonFormat.mapStringToRecordFormat(resultSet.getString(recordFormatColumnLabel))
      recordFormat match {
        case Success(format) =>
          Option(
            JsonFormat(
              recordFormat = format,
              showHeader = resultSet.getBoolean(showHeaderColumnLabel),
              wrapSingleColumn = resultSet.getBoolean(wrapSingleColumnColumnLabel)
            )
          )
        case Failure(exception) =>
          throw exception
      }
    } else {
      None
    }
  }
}
