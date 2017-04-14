package controllers

import dao.UserDao
import java.sql.SQLIntegrityConstraintViolationException
import play.api.libs.json.JsError
import play.api.data.Form
import models.{User, UserLogin}
import models.User._
import play.api.libs.json.Json
import play.api.mvc.Action
import play.api.mvc.Controller
import scala.concurrent.Future

import scala.concurrent.ExecutionContext.Implicits.global

class AuthController(userDao: UserDao, auth: SecuredAuthenticator) extends Controller {
  def register = Action.async(parse.json) { request =>
    val userResult = request.body.validate[User]
    userResult.fold(
      valid = { u =>
        import play.api.Logger
        Logger.debug(s"user: ${u}")
        val user = userDao.add(u)
        user.map(u => Created(Json.toJson(u))).recoverWith {
          case e: SQLIntegrityConstraintViolationException =>
            Future { BadRequest(s"${e.getMessage}") }
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
    val userResult = request.body.validate[UserLogin]
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

  def logout = auth.JWTAuthentication.async { request =>
    userDao.logout(request.user).map(u => u match {
      case 0 => NotFound
      case _ => Ok("User logged out successfully")
    }
    ).recoverWith {
      case _ => Future { NotFound }
    }
  }

  def show(id: Long) = auth.JWTAuthentication.async(parse.json) { request =>
    userDao.findById(id).map(u => Ok(Json.toJson(u)(profileWrites))).recoverWith {
      case _ => Future { NotFound }
    }
  }
}
