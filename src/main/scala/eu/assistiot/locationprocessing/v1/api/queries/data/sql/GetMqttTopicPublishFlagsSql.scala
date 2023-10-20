package eu.assistiot.locationprocessing.v1.api.queries.data.sql

import akka.stream.alpakka.mqtt.streaming.ControlPacketFlags
import eu.assistiot.locationprocessing.v1.api.queries.data.Topic

import java.sql.Connection
import java.util.UUID
import scala.io.Source
import scala.util.Failure
import scala.util.Success
import scala.util.Using

trait GetMqttTopicPublishFlagsSql {
  private val sqlFile = "/v1/sql/getMqttTopicPublishFlags.sql"
  private val encoding = "UTF-8"
  private val sql =
    Using(Source.fromInputStream(getClass.getResourceAsStream(sqlFile))(encoding))(_.mkString).get
  private val mqttTopicIdIdx = 1
  private val publishFlagsColumnLabel = "publish_flags"

  def getMqttTopicPublishFlagsSqlUnsafe(
      mqttTopicId: UUID
  )(implicit conn: Connection): Seq[ControlPacketFlags] = {
    val statement = conn.prepareStatement(sql)
    statement.setObject(mqttTopicIdIdx, mqttTopicId)
    val resultSet = statement.executeQuery()
    Iterator
      .continually(resultSet.next)
      .takeWhile(identity)
      .map { _ => resultSet.getString(publishFlagsColumnLabel) }
      .map { flag =>
        Topic.mapStringToControlPacket(flag)
      }
      .map {
        case Success(flag)      => flag
        case Failure(exception) => throw exception
      }
      .toSeq
  }
}
