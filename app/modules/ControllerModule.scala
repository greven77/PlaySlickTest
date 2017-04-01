package modules

import com.softwaremill.macwire._
import controllers._
import dao._

import scala.concurrent.ExecutionContext

trait ControllerModule {

  // Dependencies
  implicit def ec: ExecutionContext
  def userDao: UserDao

  // Controllers
  lazy val authController = wire[AuthController]
}
