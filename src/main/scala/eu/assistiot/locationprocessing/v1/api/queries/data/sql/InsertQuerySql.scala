package eu.assistiot.locationprocessing.v1.api.queries.data.sql

import eu.assistiot.locationprocessing.v1.api.queries.data.Query
import eu.assistiot.locationprocessing.v1.shared.Uuid.generateUuid

import java.sql.Connection
import java.sql.Types
import scala.io.Source
import scala.util.Failure
import scala.util.Success
import scala.util.Try
import scala.util.Using

trait InsertQuerySql extends InsertMqttSettingsSql {
  private val sqlFile = "/v1/sql/insertQuery.sql"
  private val encoding = "UTF-8"
  private val sql =
    Using(Source.fromInputStream(getClass.getResourceAsStream(sqlFile))(encoding))(_.mkString).get
  private val idIdx = 1
  private val nameIdx = 2
  private val inputSettingsIdIdx = 3
  private val outputSettingsIdIdx = 4
  private val queryIdx = 5

  def insertQuerySqlHelperWithoutTxUnsafe(query: Query)(implicit conn: Connection): Int = {
    val queryId = generateUuid
    val insertQueryStatement = conn.prepareStatement(sql)
    insertQueryStatement.setObject(idIdx, queryId)
    insertQueryStatement.setString(nameIdx, query.name)
    query.inputSettings.fold(insertQueryStatement.setNull(inputSettingsIdIdx, Types.NULL)) {
      inputSettings =>
        val id = insertMqttSettingsSqlUnsafe(inputSettings, queryId)
        insertQueryStatement.setObject(inputSettingsIdIdx, id)
    }
    query.outputSettings.fold(insertQueryStatement.setNull(outputSettingsIdIdx, Types.NULL)) {
      outputSettings =>
        val id = insertMqttSettingsSqlUnsafe(outputSettings, queryId)
        insertQueryStatement.setObject(outputSettingsIdIdx, id)
    }
    insertQueryStatement.setString(queryIdx, query.sql)
    insertQueryStatement.executeUpdate()
  }

  def insertQuerySqlTx(query: Query)(implicit conn: Connection): Try[Query] = {
    val result = for {
      _ <- Try(conn.setAutoCommit(false))
      _ <- Try(insertQuerySqlHelperWithoutTxUnsafe(query))
      _ <- Try(conn.commit())
    } yield ()
    result match {
      case Success(_) =>
        Success(query)
      case Failure(exception) =>
        conn.rollback()
        Failure[Query](exception)
    }
  }
}
