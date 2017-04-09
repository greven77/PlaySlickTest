package controllers

import dao.UserDao
import play.api.libs.json.JsError
import play.api.data.Form
import models.User
import play.api.libs.json.Json
import play.api.mvc.Action
import play.api.mvc.Controller
import scala.concurrent.Future

import scala.concurrent.ExecutionContext.Implicits.global

class AuthController(userDao: UserDao) extends Controller {
  def register = Action.async(parse.json) { request =>
    val userResult = request.body.validate[User]
    userResult.fold(
      valid = { u =>
        val user = userDao.add(u)
        user.map(u => Created(Json.toJson(u))).recoverWith {
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

  def login = Action.async(parse.json) { request =>
    val userResult = request.body.validate[User]
    userResult.fold(
      valid = { u =>
        val loggedUser = userDao.login(u)
        loggedUser.map(lu =>
          lu match {
            case u: Some[User] => Ok(Json.toJson(u))
            case None => Unauthorized
          }
        ).recoverWith {
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

  def show(id: Long) = Action.async(parse.json) { request =>
    userDao.findById(id).map(u => Ok(Json.toJson(u))).recoverWith {
      case _ => Future { NotFound }
    }
  }
}
