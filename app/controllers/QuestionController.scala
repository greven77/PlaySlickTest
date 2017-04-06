package controllers

import dao.QuestionDao
import models.{Question, FavouriteQuestion}
import play.api.libs.json._
import play.api.mvc.{Action, Controller}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Failure
import scala.util.Success

import utils.TaggedQuestion

class QuestionController(questionDao: QuestionDao) extends Controller {
  // add pagination and sorting later
  def index = Action.async { request =>
    questionDao.findAll().map { questions =>
      Ok(Json.toJson(questions))
    }
  }

  def tagged(tag: String) = Action.async { request =>
    questionDao.findByTag(tag).map { questions =>
      Ok(Json.toJson(questions))
    }
  }

  // shows only the content of the question excluding answers
  def show(id: Long) = Action.async { request =>
    questionDao.findById(id).map(q => Ok(Json.toJson(q))).recoverWith {
      case _ => Future { NotFound }
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
  def update(id: Long) = Action.async(parse.json) { request =>
    // add validation
    val updatedTitle = (request.body \ "title").as[String]
    val updatedContent = (request.body \ "content").as[String]
    questionDao.update(id, updatedTitle, updatedContent).
      map(updatedQuestion => Accepted(Json.toJson(updatedQuestion))).
      recoverWith {
        case _ => Future { InternalServerError }
      }
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
    questionDao.remove(id).map(question => Ok(s"Tag with id: ${id} removed")).recoverWith {
      case _ => Future { NotFound }
    }
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
