package dao

import models.{Tag => TagM, TagTable}
import slick.backend.DatabaseConfig
import slick.driver.JdbcProfile
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

class TagDao(dbConfig: DatabaseConfig[JdbcProfile]) extends BaseDao[TagM] {
  import dbConfig.driver.api._

  val db = dbConfig.db
  val tags = TableQuery[TagTable]

  def add(tag: TagM): Future[TagM] = {
    db.run(tagsReturningRow += tag)
  }

  def findAll(): Future[Seq[TagM]] = db.run(tags.result)

  override def findById(id: Long): Future[Option[TagM]] =
    db.run(tags.filter(_.id === id).result.headOption)

  def findByName(name: String): Future[Option[TagM]] =
    db.run(tags.filter(_.text === name).result.headOption)

  override def remove(id: Long): Future[Int] =
    db.run(tags.filter(_.id === id).delete)

  private def tagsReturningRow =
    tags returning tags.map(_.id) into { (b, id) =>
      b.copy(id = id)
    }
}
