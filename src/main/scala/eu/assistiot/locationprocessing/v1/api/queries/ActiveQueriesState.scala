package eu.assistiot.locationprocessing.v1.api.queries

import akka.actor.typed.ActorSystem
import com.typesafe.scalalogging.LazyLogging
import eu.assistiot.locationprocessing.v1.api.queries.data.QueryRepository
import eu.assistiot.locationprocessing.v1.executor.QueryExecutor

import scala.util.Failure
import scala.util.Success
import scala.util.Try

object ActiveQueriesState {
  def apply(system: ActorSystem[_]): Try[ActiveQueriesState] = {
    QueryRepository.getAllQueries match {
      case Success(queries) =>
        val queryExecutors = queries.map(q => q.name -> QueryExecutor(q, system)).toMap
        Success(new ActiveQueriesState(queryExecutors))
      case Failure(ex) =>
        Failure[ActiveQueriesState](ex)
    }

  }
}

class ActiveQueriesState(val queryExecutors: Map[String, Try[QueryExecutor]]) extends LazyLogging {
  queryExecutors.foreach {
    case (name, Success(_))  => logger.info(s"Query $name started")
    case (name, Failure(ex)) => logger.error(s"Query $name failed to start: $ex")
  }
}
