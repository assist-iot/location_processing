package eu.assistiot.locationprocessing.v1.api.queries.data.sql

import akka.stream.alpakka.mqtt.streaming.ControlPacketFlags
import eu.assistiot.locationprocessing.v1.api.queries.data.Topic

import java.sql.Connection
import java.util.UUID
import scala.io.Source
import scala.util.Failure
import scala.util.Success
import scala.util.Using

trait InsertMqttPublishFlagJunctionSql extends GetMqttPublishFlagsSql {
  private val sqlFile = "/v1/sql/insertPublishFlagJunction.sql"
  private val encoding = "UTF-8"
  private val sql =
    Using(Source.fromInputStream(getClass.getResourceAsStream(sqlFile))(encoding))(_.mkString).get
  private val mqttTopicIdIdx = 1
  private val flagIdIdx = 2

  def insertMqttPublishFlagsJunctionSqlUnsafe(
      publishFlags: Seq[ControlPacketFlags],
      mqttTopicId: UUID
  )(implicit
      connection: Connection
  ): Array[Int] = {
    val publishFlagsNameToId = getMqttPublishFlagsSqlUnsafe()
    val insertPublishFlagJunctionStatement = connection.prepareStatement(sql)
    for (publishFlag <- publishFlags) {
      insertPublishFlagJunctionStatement.setObject(mqttTopicIdIdx, mqttTopicId)
      Topic.mapControlPacketToString(publishFlag) match {
        case Success(publishFlagString) =>
          publishFlagsNameToId.get(publishFlagString) match {
            case Some(flagId) =>
              insertPublishFlagJunctionStatement.setObject(flagIdIdx, flagId)
            case None =>
              throw new Exception(s"Could not map $publishFlagString to database ID")
          }
        case Failure(exception) =>
          throw exception
      }
      insertPublishFlagJunctionStatement.addBatch()
    }
    insertPublishFlagJunctionStatement.executeBatch()
  }
}
