package models

import play.api.libs.json.Json
import slick.driver.MySQLDriver.api.{Tag => SlickTag}
import slick.driver.MySQLDriver.api._

case class Tag(id: Option[Long], text: String)


object Tag {
  implicit val format = Json.format[Tag]
}

class TagTable(tag: SlickTag) extends Table[Tag](tag, "tags") {
  def id = column[Option[Long]]("id", O.PrimaryKey, O.AutoInc)
  def text = column[String]("text")

  def * = (id, text) <> ((Tag.apply _).tupled, Tag.unapply _)
}
