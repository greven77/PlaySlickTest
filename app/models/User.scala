package models

import play.api.data.validation.ValidationError
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
  password: String,
  token: Option[String] = None
)

object User {
  implicit val userReads: Reads[User] = (
    (JsPath \ "id").readNullable[Long] and
    (JsPath \ "username").read[String] and
    (JsPath \ "fullname").read[String] and
    (JsPath \ "email").read[String](email) and
    (JsPath \ "password" ).read[String] and
    (JsPath \ "token").readNullable[String]
  )(User.apply _)

  implicit val userWrites = new Writes[User] {
    def writes(user: User) = Json.obj(
      "id" -> user.id,
      "username" -> user.username,
      "fullname" -> user.fullname,
      "email"  -> user.email,
      "token"  -> user.token
    )
  }

  val profileWrites = new Writes[Option[User]] {
    def writes(user: Option[User] ) = Json.obj(
      "id" -> user.map(_.id),
      "username" -> user.map(_.username),
      "fullname" -> user.map(_.fullname),
      "email" -> user.map(_.email)
    )
  }
}

class UserTable(tag: SlickTag) extends Table[User](tag, "users") {
  def id = column[Option[Long]]("id", O.PrimaryKey, O.AutoInc)
  def username = column[String]("username")
  def fullname = column[String]("fullname")
  def email = column[String]("email")
  def password = column[String]("password")
  def token = column[Option[String]]("token")

  def * = (id, username, fullname, email, password, token) <> ((User.apply _).tupled, User.unapply _)
}

case class UserLogin(username: String, password: String)

object UserLogin {
  implicit val userLoginReads = Json.reads[UserLogin]
}
