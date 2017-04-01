package modules

import play.api.db.evolutions.EvolutionsComponents
import play.api.db.evolutions.{DynamicEvolutions, EvolutionsComponents}
import play.api.db.slick.evolutions.SlickEvolutionsComponents
import slick.driver.JdbcProfile
import play.api.db.slick.{DbName, SlickComponents}

trait DatabaseModule extends SlickComponents
    with EvolutionsComponents
    with SlickEvolutionsComponents
{
  lazy val dbConfig = api.dbConfig[JdbcProfile](DbName("default"))

  override lazy val dynamicEvolutions = new DynamicEvolutions

  def onStart() = {
    applicationEvolutions.start()
  }

  onStart()
}
