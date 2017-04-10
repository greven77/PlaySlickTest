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
import dao.{AnswerDao, QuestionDao, UserDao}

case class UserRequest[A](val user: User, val request: Request[A]) extends WrappedRequest[A](request)

class SecuredAuthenticator(userDao: UserDao, questionDao: QuestionDao,
  answerDao: AnswerDao) extends Controller {

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

           val requestObj = request.toString.split(" ")

           // val reg = "/api/questions/([0-9]+)/answer/([0-9]+)".r
//           reg.findAllIn("/api/questions/71/answer/10").matchData.foreach { m => println(m.group) }
//group   groupCount   groupNames

//           reg.findAllIn("/api/questions/71/answer/10").matchData.foreach { m => println(m.groupCount) }

           user.flatMap(usr => block(UserRequest(usr.get, request)))
         }
       } else {
         Future.successful(Unauthorized("Invalid credential 2"))
       }
    }

    def isAuthorized(route: List[String], user:User, question_id: Long = -1,
      answer_id: Long = -1): Future[Boolean] = route match {
      case List("PUT", url) if url.matches("/api/questions/[0-9]+") =>
        isQuestionOwner(user, question_id)
      case List("PUT", url) if url.matches("/api/questions/[0-9]+/correctanswer") =>
        isQuestionOwner(user, question_id)
      case List("DELETE", url) if url.matches("/api/questions/[0-9]+") =>
        isQuestionOwner(user, question_id)
      case List("PUT",url) if url.matches("/api/questions/[0-9]+/answer/[0-9]+") =>
        isAnswerOwner(user, answer_id)
      case List("DELETE",url) if url.matches("/api/questions/[0-9]+/answer/[0-9]+") =>
        for {
          answerOwner <- isAnswerOwner(user, answer_id)
          questionOwner <- isQuestionOwner(user, question_id)
        } yield answerOwner || questionOwner
      case List("POST", url) if url.matches("/api/answer/[0-9]+/vote") =>
        isAnswerOwner(user, answer_id).map(!_)
      case _ => Future { false }
    }

    def isQuestionOwner(user: User, id: Long): Future[Boolean] =
      questionDao.findById(id).map(q => q.get.created_by == user.id).recoverWith {
        case _ => Future { false }
      }
    def isAnswerOwner(user: User, id: Long): Future[Boolean] =
      answerDao.findById(id).map(a => a.get.user_id == user.id).recoverWith {
        case _ => Future { false }
      }
  }
}


