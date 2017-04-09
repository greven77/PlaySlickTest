package controllers

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._
import play.api.Logger
import play.api.mvc._
import play.api.libs.json._
import scala.util.{Success, Failure}
import utils.JwtUtility

import models.User
import utils.UserPayloadWrapper
import dao.UserDao

case class UserRequest[A](val user: User, val request: Request[A]) extends WrappedRequest[A](request)

class SecuredAuthenticator(userDao: UserDao) extends Controller {

  object JWTAuthentication extends ActionBuilder[UserRequest] {
    def invokeBlock[A](request: Request[A],
      block: (UserRequest[A]) => Future[Result]): Future[Result] = {

      val jwtToken = request.headers.get("token").getOrElse("")

       if (JwtUtility.isValidToken(jwtToken)) {
         JwtUtility.decodePayload(jwtToken).fold {
           Future.successful(Unauthorized("Invalid credential 1"))
         } { payload =>
           val userCredentials = Json.parse(payload).validate[UserPayloadWrapper].get

           val user: Future[Option[User]] = userDao.findByEmail(userCredentials.email).
             map(identity)
             .recoverWith {
             case _ => Future { None }
           }

           // val fakeUser = User(None, "adsdads",
           //   "adsad asddsad", "hello@world.com", "adsdadsasdassd")
           user.flatMap(usr => block(UserRequest(usr.get, request)))
         }
       } else {
         Future.successful(Unauthorized("Invalid credential 2"))
       }
    }
  }
}


