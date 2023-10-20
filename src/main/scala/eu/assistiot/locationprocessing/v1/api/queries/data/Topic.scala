package eu.assistiot.locationprocessing.v1.api.queries.data

import akka.stream.alpakka.mqtt.streaming.ControlPacketFlags
import pl.waw.ibspan.scala_mqtt_wrapper.{MqttTopic => MqttWrapperTopic}

import scala.util.Failure
import scala.util.Success
import scala.util.Try

sealed trait PublishWhen
object FlowSuccess extends PublishWhen
object FlowFailure extends PublishWhen
object FlowAlways extends PublishWhen

final case class Topic(
    name: String,
    publishEmptyOutput: Option[Boolean],
    publishWhen: Option[PublishWhen],
    publishFlags: Option[Seq[ControlPacketFlags]]
) {
  def toMqttWrapperTopic: MqttWrapperTopic = MqttWrapperTopic(name)
}

object Topic {
  val publishWhenToString: Map[PublishWhen, String] = Map(
    FlowSuccess -> "success",
    FlowFailure -> "failure",
    FlowAlways -> "always"
  )

  val stringToPublishWhen: Map[String, PublishWhen] =
    publishWhenToString.map(_.swap)

  def mapStringToPublishWhen(input: String): Try[PublishWhen] = {
    if (stringToPublishWhen.isDefinedAt(input)) {
      Success(stringToPublishWhen(input))
    } else {
      Failure[PublishWhen](new Exception(s"Unknown PublishWhen: $input"))
    }
  }

  def mapPublishWhenToString(input: PublishWhen): Try[String] = {
    if (publishWhenToString.isDefinedAt(input)) {
      Success(publishWhenToString(input))
    } else {
      Failure[String](new Exception(s"Unknown PublishWhen: $input"))
    }
  }

  val controlPacketToString: Map[ControlPacketFlags, String] = Map(
    ControlPacketFlags.QoSAtLeastOnceDelivery -> "QoSAtLeastOnceDelivery",
    ControlPacketFlags.QoSAtMostOnceDelivery -> "QoSAtMostOnceDelivery",
    ControlPacketFlags.QoSExactlyOnceDelivery -> "QoSExactlyOnceDelivery",
    ControlPacketFlags.RETAIN -> "Retain"
  )

  val stringToControlPacket: Map[String, ControlPacketFlags] =
    controlPacketToString.map(_.swap)

  def mapStringToControlPacket(input: String): Try[ControlPacketFlags] = {
    if (stringToControlPacket.isDefinedAt(input)) {
      Success(stringToControlPacket(input))
    } else {
      Failure[ControlPacketFlags](new Exception(s"Unknown ControlPacketFlag: $input"))
    }
  }

  def mapControlPacketToString(input: ControlPacketFlags): Try[String] = {
    if (controlPacketToString.isDefinedAt(input)) {
      Success(controlPacketToString(input))
    } else {
      Failure[String](new Exception(s"Unknown ControlPacketFlag: $input"))
    }
  }

  def verifyControlPackets(packets: Seq[ControlPacketFlags]): Try[String] = {
    val numUniqueQoS = packets.count {
      case ControlPacketFlags.QoSAtLeastOnceDelivery => true
      case ControlPacketFlags.QoSAtMostOnceDelivery  => true
      case ControlPacketFlags.QoSExactlyOnceDelivery => true
      case _                                         => false
    }
    if (packets.isEmpty || numUniqueQoS.eq(1)) {
      Success("OK")
    } else {
      Failure[String](new Exception("Only one QoS level is allowed."))
    }
  }
}
