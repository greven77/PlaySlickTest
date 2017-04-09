package utils

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

case class UserPayloadWrapper(
  id: Option[Long] = None,
  username: String,
  fullname: String,
  email: String,
  token: Option[String] = None
)

object UserPayloadWrapper {
  implicit val userReads: Reads[UserPayloadWrapper] = (
    (JsPath \ "id").readNullable[Long] and
    (JsPath \ "username").read[String] and
    (JsPath \ "fullname").read[String] and
    (JsPath \ "email").read[String](email) and
    (JsPath \ "token").readNullable[String]
  )(UserPayloadWrapper.apply _)
}
