package dao

import models.{User, UserTable}
import org.mindrot.jbcrypt.BCrypt
import scala.concurrent.Future
import slick.backend.DatabaseConfig
import slick.driver.JdbcProfile
import play.api.libs.concurrent.Execution.Implicits.defaultContext

class UserDao(dbConfig: DatabaseConfig[JdbcProfile]) extends BaseDao[User] {
  import dbConfig.driver.api._

  val db = dbConfig.db
  val users = TableQuery[UserTable]

  override def add(user: User): Future[User] = {
    val secureUser = user.copy(password = hashPW(user.password))
    db.run(usersReturningRow += secureUser)
  }

  def update(u2: User) =  Future[Unit] {
    db.run(
      users.filter(_.id === u2.id).map(u =>
        (u.username, u.fullname, u.email, u.password))
        .update((u2.username, u2.fullname, u2.email, u2.password))
    )
  }

  override def findAll(): Future[Seq[User]] = db.run(users.result)

  override def findById(id: Long): Future[Option[User]] =
    db.run(users.filter(_.id === id).result.headOption)

  def findByEmail(email: String): Future[Option[User]] =
    db.run(users.filter(_.email === email).result.headOption)

  override def remove(id: Long): Future[Int] = db.run(users.filter(_.id === id).delete)

  private def hashPW(password: String) =
    BCrypt.hashpw(password, BCrypt.gensalt())

  private def usersReturningRow =
    users returning users.map(_.id) into { (b, id) =>
      b.copy(id = id)
    }
}
