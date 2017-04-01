import com.softwaremill.macwire._
import controllers._
import play.api.ApplicationLoader.Context
import play.api._
import play.api.i18n._
import play.api.routing.Router
import router.Routes
import modules._

import scala.concurrent.ExecutionContext

/**
 * Application loader that wires up the application dependencies using Macwire
 */
class AppApplicationLoader extends ApplicationLoader {
  def load(context: Context): Application = {
    (new BuiltInComponentsFromContext(context) with AppComponents).application
  }
}

trait AppComponents extends BuiltInComponents
    with I18nComponents
    with DatabaseModule
    with ControllerModule
    with DaoModule
{

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  lazy val assets: Assets = wire[Assets]
  lazy val router: Router = {
    lazy val prefix = "/"
    wire[Routes]
  }

  lazy val mainController = wire[MainController]

//  def userDao: UserDao
}

