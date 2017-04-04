package controllers

import dao.QuestionDao
import models.Question
import play.api.libs.json.{Json, JsError}
import play.api.mvc.{Action, Controller}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class QuestionController(questionDao: QuestionDao) extends Controller {

  def index = Action.async { request =>

  }

  def tagged(tag: String) = Action.async { request =>

  }

  // shows only the content of the question excluding answers
  def show(id: Long) = Action.async { request =>

  }

  // shows question with answers included
  def showThread(id: Long) = Action.async { request =>

  }

  def create = Action.async(parse.json) { request =>

  }

  def update(id: Long) = Action.async(parse.json) { request =>

  }

  def setCorrectAnswer = Action.async(parse.json) { request =>

  }

  def destroy(id: Long) = Action.async(parse.json) { request =>

  }

  def markFavourite(id: Long) = Action.async(parse.json) { request =>

  }

  def removeFavourite(id: Long) = Action.async(parse.json) { request =>

  }
}
