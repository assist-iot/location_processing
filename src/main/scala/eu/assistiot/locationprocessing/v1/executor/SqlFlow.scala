package eu.assistiot.locationprocessing.v1.executor

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.typesafe.scalalogging.LazyLogging
import eu.assistiot.locationprocessing.v1.api.queries.data.JsonFormat
import eu.assistiot.locationprocessing.v1.executor.parameters.ParameterParser
import eu.assistiot.locationprocessing.v1.executor.parameters.Parsed
import eu.assistiot.locationprocessing.v1.shared.{DatabaseHelper, Metrics}
import org.jooq.JSONFormat
import org.jooq.impl.DSL
import scalikejdbc.ConnectionPool
import scalikejdbc.using

import scala.util.Either
import scala.util.Failure
import scala.util.Left
import scala.util.Right
import scala.util.Success
import scala.util.Try
import JsonMapper.AnyJson

object SqlFlow {
  def parseSql(sql: String): Try[(String, Parsed)] = {
    for {
      parsed <- ParameterParser(isInputAvailable = true, isOutputAvailable = false).run(sql)
      parametrizedSql <- parsed.toSql
    } yield (parametrizedSql, parsed)
  }

  def apply(sql: String, outputFormat: Option[JsonFormat], name: String): Try[SqlFlow] = {
    val jsonFormat = outputFormat match {
      case Some(format) =>
        JSONFormat()
          .recordFormat(format.recordFormat)
          .header(format.showHeader)
          .wrapSingleColumnRecords(format.wrapSingleColumn)
      case None =>
        JSONFormat()
    }
    for {
      (parametrizedSql, parsed) <- parseSql(sql)
    } yield new SqlFlow(parametrizedSql, parsed, jsonFormat, name)
  }
}

class SqlFlow(sql: String, parsed: Parsed, outputFormat: JSONFormat, name: String) extends LazyLogging {
  val flow: Flow[(String, Try[AnyJson], String), (Either[String, String], Boolean), NotUsed] =
    Flow[(String, Try[AnyJson], String)]
      .wireTap { incomingMessage =>
        Metrics.queryRequests.labels(name).inc()
        logger.debug("Receiving message {}", incomingMessage)
      }
      .map {
        case (inputTopic, Success(inputJson), stringInputJson) =>
          using(ConnectionPool.borrow(DatabaseHelper.geolocationName)) { connection =>
            for {
              statement <- Try(connection.prepareStatement(sql))
              _ <- parsed.fillSqlPreparedStatementParameters(statement, inputTopic, inputJson, stringInputJson)
              resultSet <- Try(statement.executeQuery())
              isEmpty <- Try(!resultSet.isBeforeFirst)
              stringOutputJson <- Try(
                DSL
                  .using(connection)
                  .fetch(resultSet)
                  .formatJSON(outputFormat)
              )
            } yield (stringOutputJson, isEmpty)
          }
        case (_, Failure(exception), _) =>
          val msg = s"JSON input to SQL is not valid. Reason: ${exception.getMessage}"
          Failure[(String, Boolean)](new Exception(msg))
      }
      .map {
        case Success(stringOutputJson, isEmpty) =>
          val output =
            """
              |{"status": "success", "data": %s}
              |""".stripMargin.format(stringOutputJson)
          (Right[String, String](output), isEmpty)
        case Failure(exception) =>
          val msg = exception.getMessage
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
            .replace("\b", "\\b")
            .replace("\f", "\\f")
            .replace("\'", "\\\'")
          val output =
            """
              |{"status": "failure", "data": "%s"}
              |""".stripMargin.format(msg)
          (Left[String, String](output), false)
      }
      .wireTap { case result =>
        Metrics.queryResponses.labels(name).inc()  
        logger.debug("Returning message {}", result) 
      }
}
