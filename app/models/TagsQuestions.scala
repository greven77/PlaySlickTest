package models

import play.api.libs.json.Json
import slick.driver.MySQLDriver.api.{Tag => SlickTag}
import slick.driver.MySQLDriver.api._

case class TagsQuestions(tag_id: Long, question_id: Long)

object TagsQuestions {
  implicit val format = Json.format[TagsQuestions]
}

class TagsQuestionsTable(tag: SlickTag) extends Table[TagsQuestions](tag, "TagsQuestions") {
  def tag_id = column[Long]("tag_id")
  def question_id = column[Long]("question_id")

  def pk = primaryKey("tag_question_pk", (tag_id, question_id))

  def * = (tag_id, question_id) <> ((TagsQuestions.apply _).tupled, TagsQuestions.unapply)
}
