package eu.assistiot.locationprocessing.v1.api.queries.data.sql

import eu.assistiot.locationprocessing.v1.api.queries.data.Query

import java.sql.Connection
import scala.io.Source
import scala.util.Using

trait GetAllQueriesSql extends GetQuerySql {
  private val sqlFile = "/v1/sql/getAllQueryNames.sql"
  private val encoding = "UTF-8"
  private val sql =
    Using(Source.fromInputStream(getClass.getResourceAsStream(sqlFile))(encoding))(_.mkString).get
  private val nameColumnLabel = "name"

  def getAllQueryNamesSqlHelperUnsafe()(implicit conn: Connection): Seq[String] = {
    val statement = conn.createStatement()
    val resultSet = statement.executeQuery(sql)
    Iterator
      .continually(resultSet.next)
      .takeWhile(identity)
      .map(_ => resultSet.getString(nameColumnLabel))
      .toSeq
  }

  def getAllQueriesSqlUnsafe()(implicit conn: Connection): Seq[Query] = {
    getAllQueryNamesSqlHelperUnsafe()
      .map { name =>
        getQuerySqlUnsafe(name)
      }
      .collect({ case Some(query) => query })
  }
}
