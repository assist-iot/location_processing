package eu.assistiot.locationprocessing.v1.executor.parameters

import com.jayway.jsonpath.JsonPath
import eu.assistiot.locationprocessing.v1.executor.JsonMapper.AnyJson
import eu.assistiot.locationprocessing.v1.executor.JsonMapper.AnyJsonValue
import eu.assistiot.locationprocessing.v1.executor.JsonMapper.readJsonValue
import net.minidev.json.JSONArray

import java.sql.PreparedStatement
import scala.annotation.tailrec
import scala.util.Failure
import scala.util.Success
import scala.util.Try

object Parsed {
  @tailrec
  final private def createSql(
      inputParts: List[String],
      paramsLeft: Int,
      acc: String
  ): Try[String] = {
    val diff = inputParts.length - paramsLeft
    if (diff >= 0 && diff <= 1) {
      inputParts.headOption match
        case Some(part) if paramsLeft > 0 =>
          createSql(inputParts.drop(1), paramsLeft - 1, acc + part + "?")
        case Some(part) =>
          Success(acc + part)
        case None =>
          Success(acc)
    } else {
      Failure[String](
        new Exception("Incorrect difference of length between input parts and parameters")
      )
    }
  }

  @tailrec
  final private def fillSql(
      idx: Int,
      params: List[Parameter],
      statement: PreparedStatement,
      inputTopic: String,
      inputJson: AnyJson,
      stringInputJson: String
  ): Try[Unit] = {
    params.headOption match {
      case Some(param) =>
        param match {
          case InputTopic =>
            Try(statement.setString(idx, inputTopic)) match {
              case Success(_) =>
                fillSql(idx + 1, params.drop(1), statement, inputTopic, inputJson, stringInputJson)
              case Failure(e) =>
                statement.clearParameters()
                Failure[Unit](e)
            }
          case InputJson(jsonPath) =>
            val result = for {
              value <- readJsonValue(inputJson, jsonPath)
              _ <- setSqlParameterFromJson(idx, value, statement)
            } yield ()
            result match {
              case Success(_) =>
                fillSql(idx + 1, params.drop(1), statement, inputTopic, inputJson, stringInputJson)
              case Failure(e) =>
                statement.clearParameters()
                Failure[Unit](e)
            }
          case StringInputJson =>
            Try(statement.setString(idx, stringInputJson)) match {
              case Success(_) =>
                fillSql(idx + 1, params.drop(1), statement, inputTopic, inputJson, stringInputJson)
              case Failure(e) =>
                statement.clearParameters()
                Failure[Unit](e)
            }
          case _ =>
            statement.clearParameters()
            Failure[Unit](new Exception(s"Incorrect parameter used for SQL query $param"))
        }
      case None =>
        Success(())
    }
  }

  private def setSqlParameterFromJson(
      idx: Int,
      param: Option[AnyJsonValue],
      statement: PreparedStatement
  ): Try[Unit] = {
    param match {
      case Some(p: String) =>
        Try(statement.setString(idx, p))
      case Some(p: Int) =>
        Try(statement.setInt(idx, p))
      case Some(p: Double) =>
        Try(statement.setDouble(idx, p))
      case Some(_: Unit) =>
        Try(statement.setNull(idx, java.sql.Types.NULL))
      case _ =>
        Failure[Unit](
          new Exception(s"Unsupported type (${param.getClass}) of parameter $param")
        )
    }
  }

  @tailrec
  final private def createTopic(
      inputTopic: String,
      inputParts: List[String],
      params: List[Parameter],
      inputJson: Try[AnyJson],
      stringInputJson: String,
      outputJson: Try[AnyJson],
      stringOutputJson: String,
      acc: String
  ): Try[String] = {
    inline def next(inputParts: List[String], params: List[Parameter], acc: String) =
      createTopic(inputTopic, inputParts, params, inputJson, stringInputJson, outputJson, stringOutputJson, acc)

    inline def handleJson(part: String, json: Try[AnyJson], jsonPath: JsonPath) =
      json match {
        case Success(j) =>
          readJsonValue(j, jsonPath) match {
            case Success(value) =>
              value match {
                case Some(_: Unit) =>
                  next(inputParts.drop(1), params.drop(1), acc + part + "null")
                case Some(v: String) =>
                  next(inputParts.drop(1), params.drop(1), acc + part + v)
                case Some(v: Int) =>
                  next(inputParts.drop(1), params.drop(1), acc + part + v.toString)
                case Some(v: Double) =>
                  next(inputParts.drop(1), params.drop(1), acc + part + v.toString)
                case _ =>
                  Failure[String](
                    new Exception(s"Unsupported json type of topic parameter $value")
                  )
              }
            case Failure(e) =>
              Failure[String](e)
          }
        case Failure(e) => Failure[String](e)
      }

    (inputParts.headOption, params.headOption) match
      case (Some(part), Some(param)) =>
        param match {
          case InputJson(jsonPath) =>
            handleJson(part, inputJson, jsonPath)
          case OutputJson(jsonPath) =>
            handleJson(part, outputJson, jsonPath)
          case InputTopic =>
            next(inputParts.drop(1), params.drop(1), acc + part + inputTopic)
          case StringInputJson =>
            next(inputParts.drop(1), params.drop(1), acc + part + stringInputJson)
          case StringOutputJson =>
            next(inputParts.drop(1), params.drop(1), acc + part + stringOutputJson)
        }
      case (Some(part), None) =>
        Success(acc + part)
      case (None, None) =>
        Success(acc)
      case (None, Some(_)) =>
        Failure[String](
          new Exception("Incorrect difference of length between input parts and parameters")
        )
  }
}

class Parsed(val inputParts: List[String], val parameters: List[Parameter]) {
  import Parsed._

  def toSql: Try[String] = createSql(inputParts, parameters.length, "")

  def fillSqlPreparedStatementParameters(
      statement: PreparedStatement,
      inputTopic: String,
      inputJson: AnyJson,
      stringInputJson: String
  ): Try[Unit] = {
    fillSql(1, parameters, statement, inputTopic, inputJson, stringInputJson)
  }

  def toTopic(
      inputTopic: String,
      inputJson: Try[AnyJson],
      stringInputJson: String,
      outputJson: Try[AnyJson],
      stringOutputJson: String
  ): Try[String] =
    createTopic(
      inputTopic,
      inputParts,
      parameters,
      inputJson,
      stringInputJson,
      outputJson,
      stringOutputJson,
      ""
    )
}
