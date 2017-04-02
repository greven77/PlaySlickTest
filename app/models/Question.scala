package models

import org.joda.time.DateTime
import play.api.libs.json.Json
import slick.driver.MySQLDriver.api.{Tag => SlickTag}
import slick.driver.MySQLDriver.api._

case class Question(id: Option[Long], title: String, content: String,
  created_by: Long, correct_answer: Option[Long],
  created_at: Option[DateTime] = None, updated_at: Option[DateTime] = None)

object Question {
  implicit val format = Json.format[Question]
}

class QuestionTable(tag: SlickTag) extends Table[Question](tag, "questions") {
  import utils.CustomColumnTypes._

  def id = column[Option[Long]]("id", O.PrimaryKey, O.AutoInc)
  def title = column[String]("title")
  def content = column[String]("content")
  def created_by = column[Long]("created_by")
  def correct_answer = column[Option[Long]]("correct_answer")
  def created_at = column[Option[DateTime]]("created_at")
  def updated_at = column[Option[DateTime]]("updated_at")

  def * = (id, title, content, created_by, correct_answer,
    created_at, updated_at) <> ((Question.apply _).tupled, Question.unapply)

  def creator = foreignKey("creator_fk", created_by, TableQuery[UserTable])(_.id.get)
  def answer = foreignKey("answer_fk", correct_answer, TableQuery[AnswerTable])(_.id)
}
