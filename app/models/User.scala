package models

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import slick.driver.MySQLDriver.api.{Tag => SlickTag}
import slick.driver.MySQLDriver.api._

case class User(
  id: Option[Long] = None,
  username: String,
  fullname: String,
  email: String,
  password: String
)

object User {
  //implicit val format = Json.format[User]
  implicit val userReads: Reads[User] = (
    (JsPath \ "id").readNullable[Long] and
    (JsPath \ "username").read[String] and
    (JsPath \ "fullname").read[String] and
    (JsPath \ "email").read[String](email) and
    (JsPath \ "password" ).read[String]
  )(User.apply _)

  implicit val userWrites = new Writes[User] {
    def writes(user: User) = Json.obj(
      "id" -> user.id,
      "username" -> user.username,
      "fullname" -> user.fullname,
      "email"  -> user.email
    )
  }
}

class UserTable(tag: SlickTag) extends Table[User](tag, "users") {
  def id = column[Option[Long]]("id", O.PrimaryKey, O.AutoInc)
  def username = column[String]("username")
  def fullname = column[String]("fullname")
  def email = column[String]("email")
  def password = column[String]("password")

  def * = (id, username, fullname, email, password) <> ((User.apply _).tupled, User.unapply _)
}
