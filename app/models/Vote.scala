package models

import play.api.data.validation.ValidationError
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

import slick.driver.MySQLDriver.api.{Tag => SlickTag}
import slick.driver.MySQLDriver.api._

case class Vote(answer_id: Long, user_id: Long, value: Int)

object Vote {
  implicit val voteReads: Reads[Vote] = (
    (JsPath \ "answer_id").read[Long] and
    (JsPath \ "user_id").read[Long] and
    (JsPath \ "value").read[Int](userValidate)
  )(Vote.apply _)

  implicit val voteWrites = Json.writes[Vote]

  val userValidate = Reads.IntReads.
    filter(ValidationError("Value must be -1 or 1"))(validValue(_))

  def validValue(value: Int) = value == -1 || value == 1
}

class VoteTable(tag: SlickTag) extends Table[Vote](tag, "votes") {
  def answer_id = column[Long]("answer_id")
  def user_id = column[Long]("user_id")
  def value = column[Int]("vote_value")

  def pk = primaryKey("vote_pk", (answer_id, user_id))

  def * = (answer_id, user_id, value) <> ((Vote.apply _).tupled, Vote.unapply)
}
