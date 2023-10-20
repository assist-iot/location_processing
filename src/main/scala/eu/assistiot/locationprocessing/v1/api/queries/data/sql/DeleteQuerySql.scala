package eu.assistiot.locationprocessing.v1.api.queries.data.sql

import java.sql.Connection
import scala.io.Source
import scala.util.Using

trait DeleteQuerySql {
  private val sqlFile = "/v1/sql/deleteQuery.sql"
  private val encoding = "UTF-8"
  private val sql =
    Using(Source.fromInputStream(getClass.getResourceAsStream(sqlFile))(encoding))(_.mkString).get
  private val queryNameIdx = 1

  def deleteQuerySqlUnsafe(queryName: String)(implicit conn: Connection): Int = {
    val statement = conn.prepareStatement(sql)
    statement.setString(queryNameIdx, queryName)
    statement.executeUpdate()
  }
}
