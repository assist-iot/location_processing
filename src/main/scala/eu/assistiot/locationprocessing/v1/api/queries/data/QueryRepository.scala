package eu.assistiot.locationprocessing.v1.api.queries.data

import eu.assistiot.locationprocessing.v1.api.queries.data.sql.DeleteQuerySql
import eu.assistiot.locationprocessing.v1.api.queries.data.sql.GetAllQueriesSql
import eu.assistiot.locationprocessing.v1.api.queries.data.sql.GetQuerySql
import eu.assistiot.locationprocessing.v1.api.queries.data.sql.InsertQuerySql
import eu.assistiot.locationprocessing.v1.api.queries.data.sql.UpdateQuerySql
import eu.assistiot.locationprocessing.v1.shared.DatabaseHelper
import scalikejdbc.ConnectionPool
import scalikejdbc.using

import scala.util.Try

object QueryRepository
    extends GetQuerySql
    with GetAllQueriesSql
    with DeleteQuerySql
    with InsertQuerySql
    with UpdateQuerySql {
  def getQuery(name: String): Try[Option[Query]] = {
    using(ConnectionPool.borrow(DatabaseHelper.queriesName)) { implicit connection =>
      Try(getQuerySqlUnsafe(name))
    }
  }

  def getAllQueries: Try[Seq[Query]] = {
    using(ConnectionPool.borrow(DatabaseHelper.queriesName)) { implicit connection =>
      Try(getAllQueriesSqlUnsafe())
    }
  }

  def createQuery(query: Query): Try[Query] = {
    using(ConnectionPool.borrow(DatabaseHelper.queriesName)) { implicit connection =>
      insertQuerySqlTx(query)
    }
  }

  def updateQuery(name: String, query: Query): Try[Query] = {
    using(ConnectionPool.borrow(DatabaseHelper.queriesName)) { implicit connection =>
      updateQuerySqlTx(name, query)
    }
  }

  def deleteQuery(name: String): Try[Int] = {
    using(ConnectionPool.borrow(DatabaseHelper.queriesName)) { implicit connection =>
      Try(deleteQuerySqlUnsafe(name))
    }
  }
}
