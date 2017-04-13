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

case class OptionalUserRequest[A](val user: Option[User], val request: Request[A])
    extends WrappedRequest[A](request)

class LoginChecker(userDao: UserDao) extends Controller {

  object LoginInfo extends ActionBuilder[OptionalUserRequest] {
    def invokeBlock[A](request: Request[A],
      block: (OptionalUserRequest[A]) => Future[Result]): Future[Result] = {

      val jwtToken = request.headers.get("token").getOrElse("")

       if (JwtUtility.isValidToken(jwtToken)) {
         JwtUtility.decodePayload(jwtToken).fold {
           block(OptionalUserRequest(None, request))
         } { payload =>
           val userCredentials = Json.parse(payload).validate[UserPayloadWrapper].get

           val userF: Future[Option[User]] = userDao.findByEmail(userCredentials.email).
             map(identity)
             .recoverWith {
             case _ => Future { None }
             }

           userF.flatMap(usr => block(OptionalUserRequest(usr, request)))
         }
       } else {
         block(OptionalUserRequest(None, request))
       }
    }

  }
}


