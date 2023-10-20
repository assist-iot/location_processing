package eu.assistiot.locationprocessing.v1.executor

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.jayway.jsonpath.{Configuration, JsonPath, PathNotFoundException}
import com.jayway.jsonpath.spi.json.JsonProvider
import net.minidev.json.JSONArray

import scala.util.Either
import scala.util.Failure
import scala.util.Left
import scala.util.Right
import scala.util.Success
import scala.util.Try

object JsonMapper {
  type AnyJson = Any
  type AnyJsonValue = Any

  def readJsonValue(json: AnyJson, jsonPath: JsonPath): Try[Option[AnyJsonValue]] = {
    Try[AnyJsonValue](JsonPath.read[AnyJsonValue](json, jsonPath.getPath)).flatMap(value =>
      Option[AnyJsonValue](value) match {
        case Some(value) =>
          if value == null then
            Success(Some(()))
          else value.getClass[AnyJsonValue].getName match {
            case "java.lang.String" =>
              Success(Option(value.asInstanceOf[String]))
            case "net.minidev.json.JSONArray" =>
              val arr = value.asInstanceOf[JSONArray]
              // TODO: do this properly... later.
              if arr.size == 0 then
                Success(Some(arr))
              else
                Success(Some(arr.get(0)))
            case "java.lang.Integer" =>
              Success(Option(value.asInstanceOf[Int]))
            case "java.lang.Double" =>
              Success(Option(value.asInstanceOf[Double]))
            case _ =>
              Failure[Option[AnyJsonValue]](
                new Exception(s"JsonMapper: Unsupported type (${value.getClass}) of parameter $value")
              )
          }
        case None => Success(None)
      }
    ).recover {
      // Don't throw if the value was not found – return null instead
      case e: PathNotFoundException => Some(())
    }
  }

  val jsonProvider: JsonProvider = Configuration.defaultConfiguration().jsonProvider()

  val inputFlow: Flow[(String, String), (String, Try[AnyJson], String), NotUsed] =
    Flow[(String, String)]
      .map { (topic, stringJson) =>
        val json = Try(jsonProvider.parse(stringJson))
        (topic, json, stringJson)
      }

  val outputFlow: Flow[
    (Either[String, String], Boolean),
    (Try[AnyJson], Either[String, String], Boolean),
    NotUsed
  ] =
    Flow[(Either[String, String], Boolean)]
      .map { case (eitherStringJson, isEmpty) =>
        val json = eitherStringJson.fold(
          stringJson => Try(jsonProvider.parse(stringJson)),
          stringJson => Try(jsonProvider.parse(stringJson))
        )
        (json, eitherStringJson, isEmpty)
      }
}
