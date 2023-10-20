package eu.assistiot.locationprocessing.v1.api.queries.utils

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.stream.alpakka.mqtt.streaming.ControlPacketFlags
import eu.assistiot.locationprocessing.v1.api.queries.QueryRouter.QueriesNotRetrievedResponse
import eu.assistiot.locationprocessing.v1.api.queries.QueryRouter.QueriesResponse
import eu.assistiot.locationprocessing.v1.api.queries.QueryRouter.QueryCreatedResponse
import eu.assistiot.locationprocessing.v1.api.queries.QueryRouter.QueryDeletedResponse
import eu.assistiot.locationprocessing.v1.api.queries.QueryRouter.QueryNotCreatedResponse
import eu.assistiot.locationprocessing.v1.api.queries.QueryRouter.QueryNotDeletedResponse
import eu.assistiot.locationprocessing.v1.api.queries.QueryRouter.QueryNotFoundResponse
import eu.assistiot.locationprocessing.v1.api.queries.QueryRouter.QueryNotRetrievedResponse
import eu.assistiot.locationprocessing.v1.api.queries.QueryRouter.QueryNotUpdatedResponse
import eu.assistiot.locationprocessing.v1.api.queries.QueryRouter.QueryResponse
import eu.assistiot.locationprocessing.v1.api.queries.QueryRouter.QueryUpdatedResponse
import eu.assistiot.locationprocessing.v1.api.queries.QueryRouter.RunQueryForInputFailureResponse
import eu.assistiot.locationprocessing.v1.api.queries.data.JsonFormat
import eu.assistiot.locationprocessing.v1.api.queries.data.MqttSettings
import eu.assistiot.locationprocessing.v1.api.queries.data.PublishWhen
import eu.assistiot.locationprocessing.v1.api.queries.data.Query
import eu.assistiot.locationprocessing.v1.api.queries.data.Topic
import eu.assistiot.locationprocessing.v1.api.queries.data.dto.MqttSettingsDTO
import eu.assistiot.locationprocessing.v1.api.queries.data.dto.QueryDTO
import org.jooq.JSONFormat
import spray.json.DefaultJsonProtocol
import spray.json.JsString
import spray.json.JsValue
import spray.json.NullOptions
import spray.json.RootJsonFormat
import spray.json.deserializationError
import spray.json.serializationError
import spray.json.{JsonFormat => SprayJsonFormat}

import scala.util.Failure
import scala.util.Success

trait CustomSprayJsonSupport extends SprayJsonSupport with DefaultJsonProtocol with NullOptions {
  implicit val controlPacketFlagsFormat: RootJsonFormat[ControlPacketFlags] =
    new RootJsonFormat[ControlPacketFlags] {
      def write(obj: ControlPacketFlags): JsValue =
        Topic.mapControlPacketToString(obj) match {
          case Success(value) => JsString(value)
          case Failure(ex)    => serializationError(ex.getMessage)
        }

      def read(json: JsValue): ControlPacketFlags = json match {
        case JsString(s) =>
          Topic.mapStringToControlPacket(s) match {
            case Success(x)  => x
            case Failure(ex) => deserializationError(ex.getMessage)
          }
        case _ => deserializationError("Expected a string")
      }
    }

  implicit val publishWhenFormat: RootJsonFormat[PublishWhen] = new RootJsonFormat[PublishWhen] {
    def write(obj: PublishWhen): JsValue =
      Topic.mapPublishWhenToString(obj) match {
        case Success(value) => JsString(value)
        case Failure(ex)    => serializationError(ex.getMessage)
      }

    def read(json: JsValue): PublishWhen = json match {
      case JsString(s) =>
        Topic.mapStringToPublishWhen(s) match {
          case Success(x)  => x
          case Failure(ex) => deserializationError(ex.getMessage)
        }
      case _ => deserializationError("Expected a string")
    }
  }

  implicit val recordFormatFormat: RootJsonFormat[JSONFormat.RecordFormat] =
    new RootJsonFormat[JSONFormat.RecordFormat] {
      def write(obj: JSONFormat.RecordFormat): JsValue =
        JsonFormat.mapRecordFormatToString(obj) match {
          case Success(value) => JsString(value)
          case Failure(ex)    => serializationError(ex.getMessage)
        }

      def read(json: JsValue): JSONFormat.RecordFormat = json match {
        case JsString(s) =>
          JsonFormat.mapStringToRecordFormat(s) match {
            case Success(x)  => x
            case Failure(ex) => deserializationError(ex.getMessage)
          }
        case _ => deserializationError("Expected a string")
      }
    }

  implicit val topicFormat: RootJsonFormat[Topic] = jsonFormat4(Topic.apply)

  implicit val jsonFormatFormat: RootJsonFormat[JsonFormat] = jsonFormat3(JsonFormat.apply)

  implicit val mqttSettingsFormat: RootJsonFormat[MqttSettings] = jsonFormat6(MqttSettings.apply)

  implicit val queryFormat: RootJsonFormat[Query] = jsonFormat4(Query.apply)

  implicit val mqttSettingsDTOFormat: RootJsonFormat[MqttSettingsDTO] = jsonFormat6(
    MqttSettingsDTO.apply
  )

  implicit val queryDTOFormat: RootJsonFormat[QueryDTO] = jsonFormat4(QueryDTO.apply)

  implicit val queryResponseFormat: RootJsonFormat[QueryResponse] = jsonFormat1(QueryResponse.apply)

  implicit val queryNotFoundResponseFormat: RootJsonFormat[QueryNotFoundResponse] = jsonFormat1(
    QueryNotFoundResponse.apply
  )

  implicit val queryNotRetrievedResponseFormat: RootJsonFormat[QueryNotRetrievedResponse] =
    jsonFormat1(
      QueryNotRetrievedResponse.apply
    )

  implicit val queriesResponseFormat: RootJsonFormat[QueriesResponse] = jsonFormat1(
    QueriesResponse.apply
  )

  implicit val queriesNotRetrievedResponseFormat: RootJsonFormat[QueriesNotRetrievedResponse] =
    jsonFormat1(
      QueriesNotRetrievedResponse.apply
    )

  implicit val queryCreatedResponseFormat: RootJsonFormat[QueryCreatedResponse] = jsonFormat2(
    QueryCreatedResponse.apply
  )

  implicit val queryNotCreatedResponseFormat: RootJsonFormat[QueryNotCreatedResponse] = jsonFormat1(
    QueryNotCreatedResponse.apply
  )

  implicit val queryUpdatedResponseFormat: RootJsonFormat[QueryUpdatedResponse] = jsonFormat2(
    QueryUpdatedResponse.apply
  )

  implicit val queryNotUpdatedResponseFormat: RootJsonFormat[QueryNotUpdatedResponse] = jsonFormat1(
    QueryNotUpdatedResponse.apply
  )

  implicit val queryDeletedResponseFormat: RootJsonFormat[QueryDeletedResponse] = jsonFormat2(
    QueryDeletedResponse.apply
  )

  implicit val queryNotDeletedResponseFormat: RootJsonFormat[QueryNotDeletedResponse] = jsonFormat1(
    QueryNotDeletedResponse.apply
  )

  implicit val runQueryForInputFailureResponseFormat
      : RootJsonFormat[RunQueryForInputFailureResponse] = jsonFormat1(
    RunQueryForInputFailureResponse.apply
  )
}
