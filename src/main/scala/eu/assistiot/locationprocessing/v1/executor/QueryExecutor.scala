package eu.assistiot.locationprocessing.v1.executor

import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.stream.Attributes
import akka.stream.FlowShape
import akka.stream.scaladsl.Broadcast
import akka.stream.scaladsl.Flow
import akka.stream.scaladsl.GraphDSL
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Source
import akka.stream.scaladsl.Zip
import com.typesafe.scalalogging.LazyLogging
import eu.assistiot.locationprocessing.v1.api.queries.data.Query
import eu.assistiot.locationprocessing.v1.executor.JsonMapper.AnyJson
import eu.assistiot.locationprocessing.v1.executor.QueryExecutor
import pl.waw.ibspan.scala_mqtt_wrapper.MqttClient
import pl.waw.ibspan.scala_mqtt_wrapper.MqttLoggingSettings
import pl.waw.ibspan.scala_mqtt_wrapper.MqttPublishMessage
import pl.waw.ibspan.scala_mqtt_wrapper.MqttReceivedMessage
import pl.waw.ibspan.scala_mqtt_wrapper.MqttSink
import pl.waw.ibspan.scala_mqtt_wrapper.MqttSource

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Either
import scala.util.Failure
import scala.util.Success
import scala.util.Try

object QueryExecutor extends LazyLogging {
  final case class Parameters(
      mqttSource: Option[Source[(String, String), NotUsed]],
      sqlFlow: SqlFlow,
      mqttSinkFilter: Option[MqttSinkFilter],
      mqttSink: Option[Sink[MqttPublishMessage, NotUsed]]
  )

  def parseParameters(query: Query)(implicit
      system: ActorSystem[_]
  ): Try[Parameters] = {
    val mqttWrapperSourceSettings = query.inputSettings.map(_.toMqttWrapperSourceSettings)
    val mqttWrapperSourceClient = mqttWrapperSourceSettings.map(MqttClient(_))
    val mqttWrapperSource: Option[Source[(String, String), NotUsed]] = mqttWrapperSourceClient.map(
      MqttSource
        .source(_)
        .map { case MqttReceivedMessage(payload, topic, _, _) =>
          (topic, payload.utf8String)
        }
        .wireTap { case incomingMessage => logger.debug("Incoming message: {}", incomingMessage) }
    )

    val sqlFlow = SqlFlow(query.sql, query.outputSettings.flatMap(_.format), query.name)

    val mqttSinkFilter = query.outputSettings.map(MqttSinkFilter(_))

    val mqttWrapperSinkSettings = query.outputSettings.map(_.toMqttWrapperSinkSettings)
    val mqttWrapperSinkClient = mqttWrapperSinkSettings.map(MqttClient(_))
    val mqttWrapperSink = mqttWrapperSinkClient.map(MqttSink.sink(_))

    (sqlFlow, mqttSinkFilter) match {
      case (Success(flow), Some(Success(sinkFilter))) =>
        Success(Parameters(mqttWrapperSource, flow, Some(sinkFilter), mqttWrapperSink))
      case (Success(flow), None) =>
        Success(Parameters(mqttWrapperSource, flow, None, None))
      case (Failure(sqlFlowException), _) =>
        Failure[Parameters](sqlFlowException)
      case (_, Some(Failure(mqttSinkFilterException))) =>
        Failure[Parameters](mqttSinkFilterException)
      case _ =>
        Failure[Parameters](new Exception("Could not parse query executor parameters"))
    }
  }

  def apply(query: Query, system: ActorSystem[_]): Try[QueryExecutor] = {
    parseParameters(query)(system) match {
      case Success(parameters) =>
        Success(new QueryExecutor(parameters)(system))
      case Failure(exception) =>
        Failure[QueryExecutor](exception)
    }
  }

  def flowInputToSinkInput(
      sql: SqlFlow
  ): Flow[(String, String), (String, Try[AnyJson], String, Try[AnyJson], Either[String, String], Boolean), NotUsed] =
    Flow.fromGraph(GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits.*

      val inputMapper = builder.add(JsonMapper.inputFlow)
      val inputJsonBroadcast = builder.add(Broadcast[(String, Try[AnyJson], String)](2))
      val inputOutputZip =
        builder.add(Zip[(String, Try[AnyJson], String), (Try[AnyJson], Either[String, String], Boolean)]())
      val zipFlattener =
        builder.add(
          Flow[((String, Try[AnyJson], String), (Try[AnyJson], Either[String, String], Boolean))].map {
            case ((inTopic, inJson, inString), (outJson, outString, isOutputEmpty)) =>
              (inTopic, inJson, inString, outJson, outString, isOutputEmpty)
          }
        )

      inputMapper ~> inputJsonBroadcast
      inputJsonBroadcast.out(0) ~> inputOutputZip.in0
      inputJsonBroadcast.out(1) ~> sql.flow ~> JsonMapper.outputFlow ~> inputOutputZip.in1
      inputOutputZip.out ~> zipFlattener

      FlowShape(inputMapper.in, zipFlattener.out)
    })

  def flowInputToStringOutput(
      sql: SqlFlow
  ): Flow[(String, String), (Either[String, String], Boolean), NotUsed] =
    Flow[(String, String)].via(JsonMapper.inputFlow).via(sql.flow)
}

class QueryExecutor(params: QueryExecutor.Parameters)(implicit system: ActorSystem[_]) {
  import QueryExecutor._

  (params.mqttSource, params.mqttSinkFilter, params.mqttSink) match {
    case (Some(mqttSource), Some(mqttSinkFilter), Some(mqttSink)) =>
      mqttSource
        .via(flowInputToSinkInput(params.sqlFlow))
        .via(mqttSinkFilter.flow)
        .runWith(mqttSink)
    case (Some(mqttSource), None, None) =>
      mqttSource
        .via(flowInputToStringOutput(params.sqlFlow))
        .runWith(Sink.ignore)
    case _ =>
  }

  def runForInput(input: (String, String), sendResultToSink: Boolean)(implicit
      ec: ExecutionContext
  ): Future[String] = {
    (params.mqttSinkFilter, params.mqttSink, sendResultToSink) match {
      case (Some(mqttSinkFilter), Some(mqttSink), true) =>
        Source(List(input))
          .via(flowInputToSinkInput(params.sqlFlow))
          .runWith(Sink.head)
          .map { sinkInput =>
            Source(List(sinkInput)).via(mqttSinkFilter.flow).runWith(mqttSink)
            sinkInput._5.fold(identity, identity)
          }
      case _ =>
        Source(List(input))
          .via(flowInputToStringOutput(params.sqlFlow))
          .runWith(Sink.head)
          .map(_._1.fold(identity, identity))
    }
  }
}
