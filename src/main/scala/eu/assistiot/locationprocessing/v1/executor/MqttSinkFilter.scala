package eu.assistiot.locationprocessing.v1.executor

import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.stream.alpakka.mqtt.streaming.ControlPacketFlags
import akka.stream.scaladsl.Flow
import akka.util.ByteString
import com.typesafe.scalalogging.LazyLogging
import eu.assistiot.locationprocessing.v1.api.queries.data.FlowFailure
import eu.assistiot.locationprocessing.v1.api.queries.data.FlowSuccess
import eu.assistiot.locationprocessing.v1.api.queries.data.MqttSettings
import eu.assistiot.locationprocessing.v1.api.queries.data.Topic
import eu.assistiot.locationprocessing.v1.executor.parameters.ParameterParser
import eu.assistiot.locationprocessing.v1.executor.parameters.Parsed
import pl.waw.ibspan.scala_mqtt_wrapper.MqttPublishMessage

import scala.annotation.tailrec
import scala.util.Failure
import scala.util.Success
import scala.util.Try

import JsonMapper.AnyJson

object MqttSinkFilter {
  final def parseOutputTopics(topics: Seq[String]): Try[List[Parsed]] = {
    parseTopics(topics, List.empty)
  }

  @tailrec
  final private def parseTopics(topics: Seq[String], acc: List[Parsed]): Try[List[Parsed]] = {
    topics.headOption match {
      case Some(topic) =>
        ParameterParser(isInputAvailable = true, isOutputAvailable = true).run(topic) match {
          case Success(parsed) => parseTopics(topics.drop(1), parsed :: acc)
          case Failure(ex)     => Failure[List[Parsed]](ex)
        }
      case None => Success(acc)
    }
  }

  @tailrec
  final private def applyTopicParams(
      parsedTopicNames: List[Parsed],
      inputTopic: String,
      inputJson: Try[AnyJson],
      stringInputJson: String,
      outputJson: Try[AnyJson],
      stringOutputJson: String,
      acc: List[String]
  ): Try[List[String]] = {
    parsedTopicNames.headOption match {
      case Some(parsedTopicName) =>
        parsedTopicName.toTopic(inputTopic, inputJson, stringInputJson, outputJson, stringOutputJson) match {
          case Success(parametrizedTopicName) =>
            applyTopicParams(
              parsedTopicNames.drop(1),
              inputTopic,
              inputJson,
              stringInputJson,
              outputJson,
              stringOutputJson,
              parametrizedTopicName :: acc
            )
          case Failure(ex) => Failure[List[String]](ex)
        }
      case None => Success(acc)
    }
  }

  def apply(mqttSettings: MqttSettings)(implicit
      system: ActorSystem[_]
  ): Try[MqttSinkFilter] = {
    val topics = mqttSettings.topics.getOrElse(Seq.empty[Topic])
    val topicNames = topics.map(topic => topic.name)
    for {
      parsedTopicNames <- parseTopics(topicNames, List.empty)
    } yield new MqttSinkFilter(mqttSettings, topics, parsedTopicNames)
  }
}

class MqttSinkFilter(
    mqttSettings: MqttSettings,
    topics: Seq[Topic],
    parsedTopicNames: List[Parsed]
)(implicit
    system: ActorSystem[_]
) extends LazyLogging {

  import MqttSinkFilter._

  val zippedTopics: Seq[(Topic, Parsed)] = topics.zip(parsedTopicNames)
  val flow: Flow[
    (String, Try[AnyJson], String, Try[AnyJson], Either[String, String], Boolean),
    MqttPublishMessage,
    NotUsed
  ] =
    Flow[(String, Try[AnyJson], String, Try[AnyJson], Either[String, String], Boolean)]
      .wireTap { case incomingMessage => logger.debug("Receiving message {}", incomingMessage) }
      .map {
        case (
              inputTopic,
              maybeInputJson,
              stringInputJson,
              maybeOutputJson,
              eitherStringOutputJson,
              isOutputEmpty
            ) =>
          val (filteredTopics, filteredParsedTopicNames) = zippedTopics
            .filter(pair => {
              val (topic, _) = pair
              (topic.publishEmptyOutput, isOutputEmpty) match {
                case (Some(false), true) => false
                case _                   => true
              }
            })
            .filter(pair => {
              val (topic, _) = pair
              (topic.publishWhen, eitherStringOutputJson) match {
                case (Some(FlowFailure), Right(_)) => false
                case (Some(FlowSuccess), Left(_))  => false
                case _                             => true
              }
            })
            .unzip
          val stringOutputJson = eitherStringOutputJson.fold(identity, identity)
          for {
            filteredParametrizedTopicNames <- applyTopicParams(
              filteredParsedTopicNames.toList,
              inputTopic,
              maybeInputJson,
              stringInputJson,
              maybeOutputJson,
              stringOutputJson,
              List.empty
            )
          } yield (filteredTopics, filteredParametrizedTopicNames, ByteString(stringOutputJson))
      }
      .wireTap {
        case Failure(exception) =>
          logger.error(exception.getMessage)
        case other => logger.debug("Wiretapping message {}", other)
      }
      .collect { case Success((topics, parametrizedTopicNames, msg)) =>
        topics
          .zip(parametrizedTopicNames)
          .map { case (topic, parametrizedTopicName) =>
            val publishFlags = topic.publishFlags
              .getOrElse(Seq.empty[ControlPacketFlags])
              .reduceOption(_ | _)
              .getOrElse(ControlPacketFlags.None)
            val result = MqttPublishMessage(msg, parametrizedTopicName, publishFlags)
            logger.debug("Producing following result {}", result)
            logger.debug("Result ByteString {}", msg.utf8String)
            result
          }
      }
      .mapConcat(identity)
}
