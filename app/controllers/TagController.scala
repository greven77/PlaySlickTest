package controllers

import dao.TagDao
import models.Tag
import play.api.libs.json.{Json, JsError}
import play.api.mvc.{Action, Controller}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class TagController(tagDao: TagDao, auth: SecuredAuthenticator) extends Controller {

  def index = Action.async { request =>
    tagDao.findAll().map { tags =>
      Ok(Json.toJson(tags))
    }
  }

  def create = auth.JWTAuthentication.async(parse.json) { request =>
    val tagResult = request.body.validate[Tag]
    tagResult.fold(
      valid = { t =>
        val tag = tagDao.add(t)
        tag.map(t => Created(Json.toJson(t))).recoverWith {
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

  def destroy(id: Long) = auth.JWTAuthentication.async { request =>
    tagDao.remove(id).map(tag => Ok(s"Tag with id: ${id} removed")).recoverWith {
      case _ => Future { NotFound }
    }
  }
}
