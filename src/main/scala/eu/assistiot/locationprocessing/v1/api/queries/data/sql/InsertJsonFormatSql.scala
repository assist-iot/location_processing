package eu.assistiot.locationprocessing.v1.api.queries.data.sql

import eu.assistiot.locationprocessing.v1.api.queries.data.JsonFormat
import eu.assistiot.locationprocessing.v1.shared.Uuid.generateUuid

import java.sql.Connection
import java.util.UUID
import scala.io.Source
import scala.util.Failure
import scala.util.Success
import scala.util.Using

trait InsertJsonFormatSql extends GetRecordFormatsSql {
  private val sqlFile = "/v1/sql/insertJsonFormat.sql"
  private val encoding = "UTF-8"
  private val sql =
    Using(Source.fromInputStream(getClass.getResourceAsStream(sqlFile))(encoding))(_.mkString).get
  private val idIdx = 1
  private val recordFormatIdIdx = 2
  private val showHeaderIdx = 3
  private val wrapSingleColumnIdx = 4

  def insertJsonFormatSqlUnsafe(
      jsonFormat: JsonFormat
  )(implicit connection: Connection): UUID = {
    val maybeRecordFormat = JsonFormat.mapRecordFormatToString(jsonFormat.recordFormat)
    maybeRecordFormat match {
      case Success(recordFormat) =>
        val sqlRecordFormatNameToIdMap = getRecordFormatsSqlUnsafe()
        sqlRecordFormatNameToIdMap.get(recordFormat) match {
          case Some(recordFormatId) =>
            val id = generateUuid
            val insertJsonFormatStatement = connection.prepareStatement(sql)
            insertJsonFormatStatement.setObject(idIdx, id)
            insertJsonFormatStatement.setObject(recordFormatIdIdx, recordFormatId)
            insertJsonFormatStatement.setBoolean(showHeaderIdx, jsonFormat.showHeader)
            insertJsonFormatStatement.setBoolean(wrapSingleColumnIdx, jsonFormat.wrapSingleColumn)
            insertJsonFormatStatement.executeUpdate()
            id
          case None =>
            throw new Exception(s"Record format $recordFormat not found in database.")
        }
      case Failure(exception) =>
        throw exception
    }
  }
}
