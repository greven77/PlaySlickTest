package modules

import com.softwaremill.macwire._
import controllers._
import dao._

import scala.concurrent.ExecutionContext

trait ControllerModule {

  // Dependencies
  implicit def ec: ExecutionContext
  def userDao: UserDao
  def tagDao: TagDao
  def questionDao: QuestionDao
  def answerDao: AnswerDao

  // Controllers
  lazy val authController = wire[AuthController]
  lazy val tagController  = wire[TagController]
  lazy val questionController = wire[QuestionController]
  lazy val answerController = wire[AnswerController]
  lazy val securedAuthenticator = wire[SecuredAuthenticator]
}
