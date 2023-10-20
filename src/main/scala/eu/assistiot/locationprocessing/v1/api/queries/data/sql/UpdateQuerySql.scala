package eu.assistiot.locationprocessing.v1.api.queries.data.sql

import eu.assistiot.locationprocessing.v1.api.queries.data.Query

import java.sql.Connection
import scala.util.Failure
import scala.util.Success
import scala.util.Try

trait UpdateQuerySql extends DeleteQuerySql with InsertQuerySql {
  def updateQuerySqlTx(oldQueryName: String, newQuery: Query)(implicit
      conn: Connection
  ): Try[Query] = {
    val result = for {
      _ <- Try(conn.setAutoCommit(false))
      _ <- Try(deleteQuerySqlUnsafe(oldQueryName))
      _ <- Try(insertQuerySqlHelperWithoutTxUnsafe(newQuery))
      _ <- Try(conn.commit())
    } yield ()
    result match {
      case Success(_) =>
        Success(newQuery)
      case Failure(exception) =>
        conn.rollback()
        Failure[Query](exception)
    }
  }
}
