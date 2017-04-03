package dao

import models.{Question, QuestionTable, FavouriteQuestion, FavouriteQuestionTable}
import slick.backend.DatabaseConfig
import slick.driver.JdbcProfile
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

class QuestionDao(dbConfig: DatabaseConfig[JdbcProfile]) extends BaseDao[Question] {
  import dbConfig.driver.api._

  val db = dbConfig.db
  val questions = TableQuery[QuestionTable]
  val favouriteQuestions = TableQuery[FavouriteQuestionTable]

  override def add(question: Question): Future[Question] =
    db.run(questionsReturningRow += question)

  override def update(q2: Question) = Future[Unit] {
    db.run(questions.filter(_.id === q2.id).map(q =>
      (q.title, q.content)
    ).update(q2.title, q2.content))
  }

  def setCorrectAnswer(qId: Long, correct_answer_id: Option[Long]) = Future[Unit] {
    val query = questions.filter(_.id === qId).map(_.correct_answer)
    db.run(query.update(correct_answer_id))
  }

  def markFavourite(id: Long, user_id: Long): Future[FavouriteQuestion] = {
    val markFavourite =
      favouriteQuestions += FavouriteQuestion(id, user_id)
    val retrieveFavourite = findFQ(id, user_id).result.head

    db.run(markFavourite andThen retrieveFavourite)
  }

  def removeFavourite(id: Long, user_id: Long): Future[Int] =
    db.run(findFQ(id, user_id).delete)

  override def findAll(): Future[Seq[Question]] = db.run(questions.result)

  override def findById(id: Long): Future[Option[Question]] =
    db.run(questions.filter(_.id === id).result.headOption)

  override def remove(id: Long): Future[Int] =
    db.run(questions.filter(_.id === id).delete)

  private def findFQ(id: Long, user_id: Long) =
    favouriteQuestions.filter(fq => fq.question_id === id && fq.user_id === user_id)
  private def questionsReturningRow =
    questions returning questions.map(_.id) into { (q, id) =>
      q.copy(id = id)
    }
}
