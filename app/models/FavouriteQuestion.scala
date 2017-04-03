package models

import play.api.libs.json.Json
import slick.driver.MySQLDriver.api.{Tag => SlickTag}
import slick.driver.MySQLDriver.api._

case class FavouriteQuestion(question_id: Long, user_id: Long)

object FavouriteQuestion {
  implicit val format = Json.format[FavouriteQuestion]
}

class FavouriteQuestionTable(tag: SlickTag)
    extends Table[FavouriteQuestion](tag, "FavouriteQuestions") {

  def question_id = column[Long]("question_id")
  def user_id = column[Long]("user_id")

  def pk = primaryKey("favouritequestion_pk", (question_id, user_id))

  def * = (question_id, user_id) <> ((FavouriteQuestion.apply _).tupled, FavouriteQuestion.unapply)
}
