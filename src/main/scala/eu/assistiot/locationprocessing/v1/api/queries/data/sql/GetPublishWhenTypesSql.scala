package eu.assistiot.locationprocessing.v1.api.queries.data.sql

import java.sql.Connection
import scala.io.Source
import scala.util.Using

trait GetPublishWhenTypesSql {
  private val sqlFile = "/v1/sql/getPublishWhenTypes.sql"
  private val encoding = "UTF-8"
  private val sql =
    Using(Source.fromInputStream(getClass.getResourceAsStream(sqlFile))(encoding))(_.mkString).get
  private val nameColumnLabel = "name"
  private val idColumnLabel = "id"

  def getPublishWhenTypesSqlUnsafe()(implicit conn: Connection): Map[String, Object] = {
    val statement = conn.prepareStatement(sql)
    val resultSet = statement.executeQuery()
    Iterator
      .continually(resultSet.next)
      .takeWhile(identity)
      .map { _ =>
        resultSet.getString(nameColumnLabel) -> resultSet.getObject(idColumnLabel)
      }
      .toMap
  }
}
