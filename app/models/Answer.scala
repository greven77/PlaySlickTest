package models

import org.joda.time.DateTime
import play.api.libs.json.Json
import slick.driver.MySQLDriver.api.{Tag => SlickTag}
import slick.driver.MySQLDriver.api._

case class Answer(id: Option[Long], content: String,
  user_id: Long , question_id: Long,
  created_at: Option[DateTime], updated_at: Option[DateTime]
)

object Answer {
  implicit val format = Json.format[Answer]
}
