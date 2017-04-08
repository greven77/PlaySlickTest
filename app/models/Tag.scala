package models

import play.api.data.validation.ValidationError
import play.api.libs.json.Json
import slick.driver.MySQLDriver.api.{Tag => SlickTag}
import slick.driver.MySQLDriver.api._

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

case class Tag(id: Option[Long], text: String)


object Tag {
  val textValidate = Reads.StringReads.
    filter(ValidationError("Must not contain spaces or non-alphanumeric characters or other character than underscores or hyphens"))(validText(_))

  implicit val tagReads: Reads[Tag] = (
    (JsPath \ "id").readNullable[Long] and
    (JsPath \ "text").read[String](textValidate keepAnd minLength[String](3) keepAnd maxLength[String](20))
  )(Tag.apply _)

  implicit val tagWrites = Json.writes[Tag]

  def validText(text: String) = text.matches("^[a-zA-Z0-9_-]*$")
}

class TagTable(tag: SlickTag) extends Table[Tag](tag, "tags") {
  def id = column[Option[Long]]("id", O.PrimaryKey, O.AutoInc)
  def text = column[String]("text")

  def * = (id, text) <> ((Tag.apply _).tupled, Tag.unapply _)
}
