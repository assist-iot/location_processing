package eu.assistiot.locationprocessing.v1.api.queries.data.sql

import eu.assistiot.locationprocessing.v1.api.queries.data.Topic
import eu.assistiot.locationprocessing.v1.shared.Uuid.generateUuid

import java.sql.Connection
import java.sql.Types
import java.util.UUID
import scala.io.Source
import scala.util.Failure
import scala.util.Success
import scala.util.Using

trait InsertMqttTopicsSql extends GetPublishWhenTypesSql with InsertMqttPublishFlagJunctionSql {
  private val sqlFile = "/v1/sql/insertMqttTopic.sql"
  private val encoding = "UTF-8"
  private val sql =
    Using(Source.fromInputStream(getClass.getResourceAsStream(sqlFile))(encoding))(_.mkString).get
  private val idIdx = 1
  private val nameIdx = 2
  private val publishEmptyOutputIdx = 3
  private val publishWhenIdIdx = 4
  private val mqttSettingsIdIdx = 5

  def insertMqttTopicsSqlUnsafe(topics: Seq[Topic], mqttSettingsId: UUID)(implicit
      conn: Connection
  ): Array[Int] = {
    val publishWhenTypesNameToIdMap = getPublishWhenTypesSqlUnsafe()
    val insertTopicsStatement = conn.prepareStatement(sql)
    for (topic <- topics) {
      val id = generateUuid
      insertTopicsStatement.setObject(idIdx, id)
      insertTopicsStatement.setString(nameIdx, topic.name)
      topic.publishEmptyOutput.fold(
        insertTopicsStatement.setNull(publishEmptyOutputIdx, Types.BOOLEAN)
      )(
        insertTopicsStatement.setBoolean(publishEmptyOutputIdx, _)
      )
      topic.publishWhen.fold(insertTopicsStatement.setNull(publishWhenIdIdx, Types.NULL))(
        publishWhen =>
          Topic.mapPublishWhenToString(publishWhen) match {
            case Success(publishWhenString) =>
              publishWhenTypesNameToIdMap.get(publishWhenString) match {
                case Some(publishWhenId) =>
                  insertTopicsStatement.setObject(publishWhenIdIdx, publishWhenId)
                case None =>
                  throw new Exception(s"Could not find ID for publishWhenType: $publishWhenString")
              }
            case Failure(exception) =>
              throw exception
          }
      )
      insertTopicsStatement.setObject(mqttSettingsIdIdx, mqttSettingsId)
      insertTopicsStatement.addBatch()
      topic.publishFlags.map(publishFlags =>
        insertMqttPublishFlagsJunctionSqlUnsafe(publishFlags, id)
      )
    }
    insertTopicsStatement.executeBatch()
  }
}
