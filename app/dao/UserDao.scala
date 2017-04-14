package dao

import models.{User, UserTable, UserLogin}
import org.mindrot.jbcrypt.BCrypt
import play.api.libs.json.Json
import scala.concurrent.Future
import slick.backend.DatabaseConfig
import slick.driver.JdbcProfile
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import utils.JwtUtility

class UserDao(dbConfig: DatabaseConfig[JdbcProfile]) extends BaseDao[User] {
  import dbConfig.driver.api._

  val db = dbConfig.db
  val users = TableQuery[UserTable]

  def add(user: User): Future[User] = {
    val secureUser = user.copy(password = hashPW(user.password))
    val payload = Json.toJson(secureUser).toString
    val secureUserWithToken = secureUser.copy(token = Some(JwtUtility.createToken(payload)))
    db.run(usersReturningRow += secureUserWithToken)
  }

  def update(u2: User) =  Future[Unit] {
    db.run(
      users.filter(_.id === u2.id).map(u =>
        (u.username, u.fullname, u.email, u.password))
        .update((u2.username, u2.fullname, u2.email, u2.password))
    )
  }

  def findAll(): Future[Seq[User]] = db.run(users.result)

  override def findById(id: Long): Future[Option[User]] =
    db.run(users.filter(_.id === id).result.headOption)

  def findByEmail(email: String): Future[Option[User]] =
    db.run(users.filter(_.email === email).result.headOption)

  def findByUsername(username: String): Future[Option[User]] =
    db.run(users.filter(_.username === username).result.headOption)

  def findByToken(token: String): Future[Option[User]] =
    db.run(users.filter(_.token === token).result.headOption)

  override def remove(id: Long): Future[Int] = db.run(users.filter(_.id === id).delete)

  def login(userLogin: UserLogin): Future[Option[User]] = {
    val maybeUser = findByUsername(userLogin.username)
    val userF: Future[Option[User]] = maybeUser.map(u =>
      u match {
        case user: Some[User] if (BCrypt.checkpw(userLogin.password, user.get.password)) =>  {
          updateToken(user)
        }
        case None => None
      }
    )

    for {
      user <- userF
      u <- findByUsername(user.get.username)
    } yield u
  }

  def logout(user: User): Future[Int] = {
    db.run(
      users.filter(_.id === user.id).map(_.token).
        update(None)
    )
  }

  private def updateToken(u: Option[User]) = {
    val user = u.get
    val payload = Json.toJson(user).toString
    val token = Some(JwtUtility.createToken(payload))
    db.run(
      users.filter(_.id === user.id).map(_.token).
        update(token)
    )
    u
  }

  private def hashPW(password: String) =
    BCrypt.hashpw(password, BCrypt.gensalt())

  private def usersReturningRow =
    users returning users.map(_.id) into { (b, id) =>
      b.copy(id = id)
    }
}
