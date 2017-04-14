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

case class UserRequest[A](val user: User, val request: Request[A])
    extends WrappedRequest[A](request)

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
           //val userCredentials = Json.parse(payload).validate[UserPayloadWrapper].get

           val userF: Future[Option[User]] = userDao.findByToken(jwtToken).
             map(identity)
             .recoverWith {
             case _ => Future { None }
             }

           for {
               user <- userF
               isAccessGranted <- checkAuthorized(request.toString, user)
               block <- decide(isAccessGranted, user, request, block)
             } yield block

           //userF.flatMap(usr => block(UserRequest(usr.get, request)))
         }
       } else {
         Future.successful(Unauthorized("Invalid credential 2"))
       }
    }

    def decide[A](isAccessGranted: Boolean, user: Option[User],
      request: Request[A], block: (UserRequest[A]) => Future[Result]): Future[Result] =
      if (isAccessGranted)
        block(UserRequest(user.get, request))
      else
        Future.successful(Unauthorized)

    def checkAuthorized(requestString: String, user: Option[User]): Future[Boolean] = {
      val requestItems = requestString.split(" ")
      val Array(verb, url) = requestItems

      val reg = """(\/api\/[a-z]+)[\/]?(\d*).*?(\/([a-z]+)\/(\d+))?$""".r

      val groupsList =
        reg.findFirstMatchIn(url).get.subgroups.filter { group =>
          group != null && group != ""
        }

      user match {
        case Some(user) => groupsList match {
          case (List(_, id)) =>
            isAuthorized(requestItems, user,  id.toLong)
          case List(_, id, _, _, child_id) =>
            isAuthorized(requestItems, user, id.toLong, child_id.toLong)

          case _ => Future { true }
        }

        case _ => Future { false }
      }
    }

    def isAuthorized(route: Array[String], user: User, parent_id: Long = -1,
      child_id: Long = -1): Future[Boolean] = {

      route match {
        case Array("PUT", url) if url.matches("/api/questions/[0-9]+") =>
          isQuestionOwner(user, parent_id)
        case Array("PUT", url) if url.matches("/api/questions/[0-9]+/correctanswer") =>
          isQuestionOwner(user, parent_id)
        case Array("DELETE", url) if url.matches("/api/questions/[0-9]+") =>
          isQuestionOwner(user, parent_id)
        case Array("PUT",url) if url.matches("/api/questions/[0-9]+/answer/[0-9]+") =>
          isAnswerOwner(user, parent_id)
        case Array("DELETE",url) if url.matches("/api/questions/[0-9]+/answer/[0-9]+") =>
          for {
            answerOwner <- isAnswerOwner(user, child_id)
            questionOwner <- isQuestionOwner(user, parent_id)
          } yield answerOwner || questionOwner
        case Array("POST", url) if url.matches("/api/answer/[0-9]+/vote") =>
          isAnswerOwner(user, parent_id).map(!_)
        case _ => Future { false }
      }
    }

    def isQuestionOwner(user: User, id: Long): Future[Boolean] =
      questionDao.findById(id).map(q => {
        q.get.created_by == user.id}).recoverWith {
        case _ => Future { false }
      }
    def isAnswerOwner(user: User, id: Long): Future[Boolean] =
      answerDao.findById(id).map(a => a.get.user_id == user.id).recoverWith {
        case _ => Future { false }
      }
  }
}


