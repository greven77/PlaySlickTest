package models

import org.joda.time.DateTime
import play.api.libs.json.Json
import slick.driver.MySQLDriver.api.{Tag => SlickTag}
import slick.driver.MySQLDriver.api._

case class Answer(id: Option[Long], content: String,
  user_id: Long , question_id: Long,
  created_at: Option[DateTime] = None, updated_at: Option[DateTime] = None
)

object Answer {
  implicit val format = Json.format[Answer]
}

class AnswerTable(tag: SlickTag) extends Table[Answer](tag, "answers") {
  import utils.CustomColumnTypes._

  def id = column[Option[Long]]("id", O.PrimaryKey, O.AutoInc)
  def content = column[String]("content")
  def user_id = column[Long]("user_id")
  def question_id = column[Long]("question_id")
  def created_at = column[Option[DateTime]]("created_at")
  def updated_at = column[Option[DateTime]]("updated_at")

  def * = (id, content, user_id, question_id,
    created_at, updated_at) <> ((Answer.apply _).tupled, Answer.unapply)

  def creator = foreignKey("creator_fk", user_id, TableQuery[UserTable])(_.id.get)
  def question = foreignKey("parent_question_fk", question_id, TableQuery[QuestionTable])(_.id.get)
}
