package models

import play.api.data.validation.ValidationError
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

import slick.driver.MySQLDriver.api.{Tag => SlickTag}
import slick.driver.MySQLDriver.api._

case class Vote(answer_id: Option[Long], user_id: Option[Long], value: Int)

object Vote {
  val userValidate = Reads.IntReads.
    filter(ValidationError("Value must be -1 or 1"))(validValue(_))

  implicit val voteReads: Reads[Vote] = (
    (JsPath \ "answer_id").readNullable[Long] and
    (JsPath \ "user_id").readNullable[Long] and
    (JsPath \ "value").read[Int](userValidate)
  )(Vote.apply _)

  implicit val voteWrites = Json.writes[Vote]

  def validValue(value: Int) = value == -1 || value == 1
}

class VoteTable(tag: SlickTag) extends Table[Vote](tag, "votes") {
  def answer_id = column[Option[Long]]("answer_id")
  def user_id = column[Option[Long]]("user_id")
  def value = column[Int]("vote_value")

  def pk = primaryKey("vote_pk", (answer_id, user_id))

  def * = (answer_id, user_id, value) <> ((Vote.apply _).tupled, Vote.unapply)
}
