package controllers

import dao.AnswerDao
import models.{Answer, Vote}
import java.sql._
import play.api.libs.json._
import play.api.mvc.{Action, Controller}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Failure
import scala.util.Success

class AnswerController(answerDao: AnswerDao) extends Controller {

  def create(qId: Long) = Action.async(parse.json) { request =>
    val answerResult = request.body.validate[Answer]

    answerResult.fold(
      valid = { a =>
        val answer = answerDao.add(a)
        answer.map(a => Created(Json.toJson(a))).recoverWith {
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

  def update(qId: Long, id: Long) = Action.async(parse.json) { request =>
    val answerResult = request.body.validate[Answer]

    answerResult.fold(
      valid = { a =>
        answerDao.update(a).
          map(updatedAnswer => Accepted(Json.toJson(updatedAnswer))).
          recoverWith {
            case e: SQLIntegrityConstraintViolationException =>
              Future { NotFound("invalid id or id not provided") }
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

  def destroy(qId: Long, id: Long) = Action.async(parse.json) { request =>
    answerDao.remove(id).map(answer => NoContent).
      recoverWith {
        case e: SQLIntegrityConstraintViolationException =>
          Future { NotFound("invalid id or id not provided")}
        case _ => Future { InternalServerError }
      }
  }

  def vote(id: Long) = Action.async(parse.json) { request =>
    val voteResult = request.body.validate[Vote]

    voteResult.fold(
      valid = { v =>
        answerDao.vote(v).
          map(voteUpdatedAnswer => Accepted(Json.toJson(voteUpdatedAnswer))).
          recoverWith {
            case e: SQLIntegrityConstraintViolationException =>
              Future { NotFound("invalid id or id not provided")}
            case oe: Exception =>
              Future { BadRequest(s"${oe.getClass().getName()} ${oe.getMessage()}")}
            case  _ => Future { InternalServerError }
          }
      },
      invalid = { errors =>
        Future.successful(
          BadRequest(JsError.toJson(errors))
        )
      }
    )
  }
}
