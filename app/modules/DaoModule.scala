package modules

import com.softwaremill.macwire._
import dao._
import slick.backend.DatabaseConfig
import slick.driver.JdbcProfile

trait DaoModule {
  def dbConfig: DatabaseConfig[JdbcProfile]

  lazy val userDao = wire[UserDao]
}
