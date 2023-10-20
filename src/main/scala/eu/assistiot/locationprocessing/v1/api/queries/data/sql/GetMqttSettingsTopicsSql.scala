package eu.assistiot.locationprocessing.v1.api.queries.data.sql

import eu.assistiot.locationprocessing.v1.api.queries.data.PublishWhen
import eu.assistiot.locationprocessing.v1.api.queries.data.Topic

import java.sql.Connection
import java.util.UUID
import scala.io.Source
import scala.util.Failure
import scala.util.Success
import scala.util.Using

case class TopicSql(
    id: UUID,
    name: String,
    publishEmptyOutput: Option[Boolean],
    publishWhen: Option[PublishWhen]
)

trait GetMqttSettingsTopicsSql extends GetMqttTopicPublishFlagsSql {
  private val sqlFile = "/v1/sql/getMqttSettingsTopics.sql"
  private val encoding = "UTF-8"
  private val sql =
    Using(Source.fromInputStream(getClass.getResourceAsStream(sqlFile))(encoding))(_.mkString).get
  private val mqttSettingsIdIdx = 1
  private val publishWhenColumnLabel = "publish_when"
  private val idColumnLabel = "id"
  private val nameColumnLabel = "name"
  private val publishEmptyOutputColumnLabel = "publish_empty_output"

  def getMqttSettingsTopicsSqlHelperUnsafe(
      mqttSettingsId: UUID
  )(implicit conn: Connection): Seq[TopicSql] = {
    val statement = conn.prepareStatement(sql)
    statement.setObject(mqttSettingsIdIdx, mqttSettingsId)
    val resultSet = statement.executeQuery()
    Iterator
      .continually(resultSet.next)
      .takeWhile(identity)
      .map { _ =>
        Option(resultSet.getString(publishWhenColumnLabel)).map(publishWhen =>
          Topic.mapStringToPublishWhen(publishWhen)
        )
      }
      .map {
        case Some(Success(publishWhen)) => Some(publishWhen)
        case Some(Failure(exception))   => throw exception
        case None                       => None
      }
      .map { maybePublishWhen =>
        TopicSql(
          id = resultSet.getObject(idColumnLabel, classOf[UUID]),
          name = resultSet.getString(nameColumnLabel),
          publishEmptyOutput = Option(resultSet.getBoolean(publishEmptyOutputColumnLabel)),
          publishWhen = maybePublishWhen
        )
      }
      .toSeq
  }

  def getMqttSettingsTopicsSqlUnsafe(
      id: UUID
  )(implicit conn: Connection): Seq[Topic] = {
    getMqttSettingsTopicsSqlHelperUnsafe(id).map { topicSql =>
      val flags = getMqttTopicPublishFlagsSqlUnsafe(topicSql.id)
      val maybeFlags = if (flags.isEmpty) None else Some(flags)
      Topic(
        name = topicSql.name,
        publishEmptyOutput = topicSql.publishEmptyOutput,
        publishWhen = topicSql.publishWhen,
        publishFlags = maybeFlags
      )
    }
  }
}
