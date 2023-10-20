package eu.assistiot.locationprocessing.v1.api.queries.data

import org.jooq.JSONFormat

import scala.util.Failure
import scala.util.Success
import scala.util.Try

final case class JsonFormat(
    recordFormat: JSONFormat.RecordFormat,
    showHeader: Boolean,
    wrapSingleColumn: Boolean
)

object JsonFormat {
  val recordFormatToString: Map[JSONFormat.RecordFormat, String] = Map(
    JSONFormat.RecordFormat.ARRAY -> "array",
    JSONFormat.RecordFormat.OBJECT -> "object"
  )

  val stringToRecordFormat: Map[String, JSONFormat.RecordFormat] =
    recordFormatToString.map(_.swap)

  def mapStringToRecordFormat(input: String): Try[JSONFormat.RecordFormat] = {
    if (stringToRecordFormat.isDefinedAt(input)) {
      Success(stringToRecordFormat(input))
    } else {
      Failure[JSONFormat.RecordFormat](new Exception(s"Unknown recordFormat: $input"))
    }
  }

  def mapRecordFormatToString(input: JSONFormat.RecordFormat): Try[String] = {
    if (recordFormatToString.isDefinedAt(input)) {
      Success(recordFormatToString(input))
    } else {
      Failure[String](new Exception(s"Unknown recordFormat: $input"))
    }
  }
}
