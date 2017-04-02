package models

import org.joda.time.DateTime
import play.api.libs.json.Json
import slick.driver.MySQLDriver.api.{Tag => SlickTag}
import slick.driver.MySQLDriver.api._

case class Question(id: Option[Long], title: String, content: String,
  created_by: Long, corrected_answered_by: Option[Long],
  created_at: Option[DateTime], updated_at: Option[DateTime])

object Question {
  implicit val format = Json.format[Question]
}
