package controllers

import dao.QuestionDao
import models.{Question, FavouriteQuestion}
import java.sql._
import play.api.libs.json._
import play.api.mvc.{Action, Controller}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Failure
import scala.util.Success

import utils.{TaggedQuestion, SortingPaginationWrapper}

class QuestionController(questionDao: QuestionDao) extends Controller {
  // add pagination and sorting later
  import play.api.Logger

  def index = Action.async(parse.json) { request =>
    val result =
      request.body.validate[SortingPaginationWrapper]
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

  def tagged(tag: String) = Action.async { request =>
    questionDao.findByTag(tag).map { questions =>
      Ok(Json.toJson(questions))
    }.recoverWith {
      case e: SQLIntegrityConstraintViolationException  =>
        Future { NotFound }
      case _  => Future { InternalServerError }
    }
  }

  // shows only the content of the question excluding answers
  def show(id: Long) = Action.async { request =>
    // change query in order to join tags and answers
    questionDao.findById(id).map(q => Ok(Json.toJson(q))).recoverWith {
      case _ => Future { NotFound }
    }.recoverWith {
      case e: SQLIntegrityConstraintViolationException  =>
        Future { NotFound }
      case _  => Future { InternalServerError }
    }
  }

  // shows question with answers included
  def showThread(id: Long) = Action.async { request =>
    questionDao.findAndRetrieveThread(id).map(qt => Ok(Json.toJson(qt))).
      recoverWith {
        case _ => Future { NotFound }
      }
  }

  def create = Action.async(parse.json) { request =>
    val questionResult = request.body.validate[TaggedQuestion]

    questionResult.fold(
      valid = {q =>
        val question = questionDao.addWithTags(q)
        question.map(q => Created(Json.toJson(q))).recoverWith {
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
  // TODO: add business rules
  // add update tags feature
  def update(id: Long) = Action.async(parse.json) { request =>
    // add validation
    val questionResult = request.body.validate[TaggedQuestion]

    questionResult.fold(
      valid = { q =>
        questionDao.update(q).
          map(updatedQuestion => Accepted(Json.toJson(updatedQuestion))).
          recoverWith {
            case e: SQLIntegrityConstraintViolationException  =>
              Future { NotFound("invalid id or id not provided")}
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

  // TODO: add business rules
  def setCorrectAnswer(id: Long) = Action.async(parse.json) { request =>
    (request.body \ "answer_id").validate[Long] match {
      case a: JsSuccess[Long] => {
        val answer_id = Some(a.get)
        questionDao.setCorrectAnswer(id, answer_id).
          map(updatedQuestion => Accepted(Json.toJson(updatedQuestion))).
          recoverWith {
            case _ => Future { InternalServerError }
          }
      }

      case e: JsError => Future { BadRequest }
    }
    //val question_id = (request.body \ "id").as[Long]
  }

  // TODO: add business rules
  def destroy(id: Long) = Action.async(parse.json) { request =>
    questionDao.remove(id).map(question => NoContent).
      recoverWith {
        case _ => Future { NotFound }
      }
  }

  // TODO: add business rules
  def markFavourite(id: Long) = Action.async(parse.json) { request =>
    val fqResult = request.body.validate[FavouriteQuestion]

    fqResult.fold(
      valid = { fq =>
        val favouritedQuestion = questionDao.markFavourite(fq)
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

  // TODO: add business rules
  def removeFavourite(id: Long) = Action.async(parse.json) { request =>
    val fqResult = request.body.validate[FavouriteQuestion]

    fqResult.fold(
      valid = { fq =>
        questionDao.removeFavourite(fq).map(favourite =>
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
}
