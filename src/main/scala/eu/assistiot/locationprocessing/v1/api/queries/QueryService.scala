package eu.assistiot.locationprocessing.v1.api.queries

import eu.assistiot.locationprocessing.v1.api.queries.data.Query
import eu.assistiot.locationprocessing.v1.api.queries.data.QueryRepository
import eu.assistiot.locationprocessing.v1.api.queries.data.Topic
import eu.assistiot.locationprocessing.v1.api.queries.data.dto.QueryDTO
import eu.assistiot.locationprocessing.v1.executor.MqttSinkFilter
import eu.assistiot.locationprocessing.v1.executor.SqlFlow

import scala.annotation.tailrec
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success
import scala.util.Try

object QueryService {
  def verifyQuery(query: Query): Try[String] = {
    @tailrec
    def checkOutputTopicsPublishFlags(topics: List[Topic]): Try[String] = {
      topics.headOption match {
        case Some(topic) =>
          topic.publishFlags match {
            case Some(publishFlags) =>
              Topic.verifyControlPackets(publishFlags) match {
                case Success(_)  => checkOutputTopicsPublishFlags(topics.drop(1))
                case Failure(ex) => Failure[String](ex)
              }
            case None => checkOutputTopicsPublishFlags(topics.drop(1))
          }
        case None => Success("OK")
      }
    }

    val outputTopics = query.outputSettings.flatMap(_.topics).getOrElse(Seq.empty[Topic])
    for {
      _ <- checkOutputTopicsPublishFlags(outputTopics.toList)
      _ <- SqlFlow.parseSql(query.sql)
      _ <- MqttSinkFilter.parseOutputTopics(outputTopics.map(_.name))
    } yield "OK"
  }

  def getQuery(name: String, activeQueriesState: ActiveQueriesState): Try[Option[QueryDTO]] = {
    // TODO: add authorization
    // TODO: get query state from active queries
    QueryRepository.getQuery(name).map(_.map(_.toDTO))
  }

  def getAllQueries(activeQueriesState: ActiveQueriesState): Try[Seq[QueryDTO]] = {
    // TODO: add authorization
    // TODO: get query states from active queries
    QueryRepository.getAllQueries.map(_.map(_.toDTO))
  }

  def createQuery(query: Query): Try[QueryDTO] = {
    // TODO: add authorization
    verifyQuery(query) match {
      case Success(_) =>
        QueryRepository.createQuery(query).map(_.toDTO)
      case Failure(e) => Failure[QueryDTO](e)
    }
  }

  def updateQuery(name: String, query: Query): Try[QueryDTO] = {
    // TODO: add authorization
    verifyQuery(query) match {
      case Success(_) =>
        QueryRepository.updateQuery(name, query).map(_.toDTO)
      case Failure(e) => Failure[QueryDTO](e)
    }
  }

  def deleteQuery(name: String): Try[Int] = {
    // TODO: add authorization
    QueryRepository.deleteQuery(name)
  }

  def runQueryForInput(
      queryName: String,
      inputTopic: String,
      input: String,
      passOutputToSink: Boolean,
      activeQueriesState: ActiveQueriesState
  )(implicit ec: ExecutionContext): Try[Future[String]] = {
    // TODO: add authorization
    activeQueriesState.queryExecutors.get(queryName) match {
      case Some(found) =>
        found match {
          case Success(queryExecutor) =>
            Success(queryExecutor.runForInput((inputTopic, input), passOutputToSink))
          case Failure(exception) =>
            Failure[Future[String]](exception)
        }
      case None =>
        Failure[Future[String]](throw new Exception(s"Query $queryName not found"))
    }
  }
}
