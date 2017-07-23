package controllers

import dao.QuestionDao
import javax.naming.AuthenticationException
import models.{Question, FavouriteQuestion}
import java.sql._
import play.api.libs.json._
import play.api.mvc.{Action, Controller, Request}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Failure
import scala.util.Success

import utils.{TaggedQuestion, SortingPaginationWrapper}
import utils.SortingPaginationWrapper.sortingPaginationAnswerReads

class QuestionController(questionDao: QuestionDao, auth: SecuredAuthenticator,
  sessionInfo: LoginChecker) extends Controller {

  def index = Action.async { request =>
   // val result =
   //request.body.validate[SortingPaginationWrapper]
   //val result = Json.toJson(SortingPaginationWrapper(sort_by, page, resultsPerPage, direction))
    //  .validate[SortingPaginationWrapper]

    val result = headersPaginationWrapper(request)
//    import play.api.Logger
//    Logger.debug(s"$result")
    result.fold(
      valid = { r =>
       questionDao.findAll(r).map { questions =>
          Ok(Json.toJson(questions))
        }
      },
      invalid = { errors =>
        Future.successful(
          BadRequest(JsError.toJson(errors))
        )
      }
    )
  }

  def tagged(tag: String) = Action.async(parse.json) { request =>
    val result = request.body.validate[SortingPaginationWrapper]
    result.fold (
      valid = { params =>
        questionDao.findByTag(tag, params).map { questions =>
          Ok(Json.toJson(questions))
        }.recoverWith {
          case e: SQLIntegrityConstraintViolationException  =>
            Future { NotFound }
          case _  => Future { InternalServerError }
        }
      },
      invalid = { errors =>
        Future.successful(
          BadRequest(JsError.toJson(errors))
        )
      }
    )
  }

  def show(id: Long) = Action.async { request =>
    questionDao.findById(id).map(q => Ok(Json.toJson(q))).recoverWith {
      case _ => Future { NotFound }
    }.recoverWith {
      case e: SQLIntegrityConstraintViolationException  =>
        Future { NotFound }
      case _  => Future { InternalServerError }
    }
  }

  def showThread(id: Long) = sessionInfo.LoginInfo.async(parse.json) { request =>
    val result = request.body.validate[SortingPaginationWrapper](sortingPaginationAnswerReads)

    result.fold(
      valid = { spw =>
        questionDao.findAndRetrieveThread(id, spw, request.user).map(qt => Ok(Json.toJson(qt))).
          recoverWith {
            case e: SQLIntegrityConstraintViolationException => Future { NotFound }
            case _ => Future { InternalServerError}
          }
      },
      invalid = { errors =>
        Future.successful(
          BadRequest(JsError.toJson(errors))
        )
      }
    )

  }

  def create = auth.JWTAuthentication.async(parse.json) { request =>
    val questionResult = request.body.validate[TaggedQuestion]

    questionResult.fold(
      valid = {tq =>
        val questionWithUser = tq.question.copy(created_by = request.user.id)
        val taggedQuestionWithUser: TaggedQuestion = tq.copy(question = questionWithUser)
        val question = questionDao.addWithTags(taggedQuestionWithUser)
        question.map(q => Created(Json.toJson(q))).recoverWith {
          case e: Exception => Future { BadRequest(s"create: ${e.getClass().getName()}")}
          case _ => Future { InternalServerError }
        }
      },
      invalid = { errors =>
        Future.successful(
          BadRequest(JsError.toJson(errors))
        )
      }
    )
  }

  def update(id: Long) = auth.JWTAuthentication.async(parse.json) { request =>
    val questionResult = request.body.validate[TaggedQuestion]

    val user = request.user

    questionResult.fold(
      valid = { tq =>
        val questionWithId = tq.question.copy(id = Some(id))
        val taggedQuestion = tq.copy(question = questionWithId)
        questionDao.update(taggedQuestion, user).
          map(updatedQuestion => Accepted(Json.toJson(updatedQuestion))).
          recoverWith {
            case authEx: AuthenticationException => Future { Unauthorized }
            case e: SQLIntegrityConstraintViolationException  =>
              Future { NotFound("invalid id or id not provided or title is not unique")}
            case _ => Future { InternalServerError }
          }
      },
      invalid = { errors =>
        Future.successful(
          BadRequest(JsError.toJson(errors))
        )
      }
    )

  }

  def setCorrectAnswer(id: Long) = auth.JWTAuthentication.async(parse.json) { request =>
    (request.body \ "answer_id").validate[Long] match {
      case a: JsSuccess[Long] => {
        val answer_id = Some(a.get)
        questionDao.setCorrectAnswer(id, answer_id).
          map(updatedQuestion => Accepted(Json.toJson(updatedQuestion))).
          recoverWith {
            case e: SQLIntegrityConstraintViolationException =>
              Future { NotFound(s"${e.getMessage()}")}
            case _ => Future { InternalServerError }
          }
      }

      case e: JsError => Future { BadRequest }
    }
  }

  def unsetCorrectAnswer(id: Long) = auth.JWTAuthentication.async(parse.json) { request =>
    (request.body \ "answer_id").validate[Long] match {
      case a: JsSuccess[Long] => {
        val answer_id = Some(a.get)
        questionDao.setCorrectAnswer(id, None).
          map(updatedQuestion => Accepted(Json.toJson(updatedQuestion))).
          recoverWith {
            case e: SQLIntegrityConstraintViolationException =>
              Future { NotFound(s"${e.getMessage()}")}
            case _ => Future { InternalServerError }
          }
      }

      case e: JsError => Future { BadRequest }
    }
   }

  def destroy(id: Long) = auth.JWTAuthentication.async(parse.json) { request =>
    questionDao.remove(id).map(question => NoContent).
      recoverWith {
        case _ => Future { NotFound }
      }
  }

  def markFavourite(id: Long) = auth.JWTAuthentication.async(parse.json) { request =>
    val fqResult = request.body.validate[FavouriteQuestion]

    fqResult.fold(
      valid = { fq =>
        val fqWithId = fq.copy(question_id = id)
        val favouritedQuestion = questionDao.markFavourite(fqWithId)
        favouritedQuestion.map(f => Created(Json.toJson(f))).recoverWith {
          case _ => Future { InternalServerError }
        }
      },
      invalid = { errors =>
        Future.successful(
          BadRequest(JsError.toJson(errors))
        )
      }
    )

  }

  def removeFavourite(id: Long) = auth.JWTAuthentication.async(parse.json) { request =>
    val fqResult = request.body.validate[FavouriteQuestion]

    fqResult.fold(
      valid = { fq =>
        val fqWithId = fq.copy(question_id = id)
        questionDao.removeFavourite(fqWithId).map(favourite =>
          favourite match {
            case 1 => Ok(s"Tag with id: ${fq.question_id} removed")
            case 0 => NotFound
          }).recoverWith {
          case _ => Future { InternalServerError }
        }
      },
      invalid = { errors =>
        Future.successful(
          BadRequest(JsError.toJson(errors))
        )
      }
    )
  }

  private def getQuestion(id: Long): Future[Option[Question]] = {
    val questionQuery = questionDao.findById(id)
    questionQuery.map(question =>
      question
    ).recoverWith {
      case _ => Future {None}
    }
  }

  def headersPaginationWrapper(request: Request[Any]) = {
    val sort_by = request.headers.get("sort_by").getOrElse("date")
    val page = request.headers.get("page").getOrElse("0")
    val resultsPerPage = request.headers.get("resultsPerPage").getOrElse("25")
    val direction = request.headers.get("direction").getOrElse("desc")

    Json.obj("sort_by" -> sort_by, "page" -> page,
      "resultsPerPage" -> resultsPerPage, "direction" -> direction)
      .validate[SortingPaginationWrapper]
  }
}
