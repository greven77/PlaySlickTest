package dao

import models.{Question, QuestionTable, FavouriteQuestion,
  FavouriteQuestionTable, Answer, AnswerTable, User,
  UserTable, QuestionThread, Vote, VoteTable}
import slick.backend.DatabaseConfig
import slick.driver.JdbcProfile
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

class QuestionDao(dbConfig: DatabaseConfig[JdbcProfile]) extends BaseDao[Question] {
  import dbConfig.driver.api._

  val db = dbConfig.db
  val questions = TableQuery[QuestionTable]
  val favouriteQuestions = TableQuery[FavouriteQuestionTable]
  val answers = TableQuery[AnswerTable]
  val users = TableQuery[UserTable]
  val votes = TableQuery[VoteTable]

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
    db.run(findByIdQuery(id))

  override def findAndRetrieveThread(id: Long): Future[QuestionThread] = {
    // perform a join between question and answer tables
    val filteredAnswers = answers.filter(_.question_id === id)

    val countVotes = filteredAnswers.
      join(votes).on(_.id === _.answer_id).
      groupBy { case (answer, vote) => answer.id }.
      map { case (answer_id, group) =>
        (answer_id, group.map { case (answer, vote) => vote.value}.sum)
      } // maybe it would be better to return in a class instead
 
    val answersUsers = filteredAnswers.
        join(questions).on(_.question_id  === _.id).
        join(users).on { case ((answer, question), user) => question.created_by === user.id }.
        map { case ((answer, question), user ) =>
            (answer, user)
        }

    val setup = for {
      question <- findByIdQuery(id)
      au       <- answersUsers.result
      voteCount <- countVotes
    } yield QuestionThread(question, (au, voteCount))

    db.run(setup)
  }

  override def remove(id: Long): Future[Int] =
    db.run(questions.filter(_.id === id).delete)

  private def findByIdQuery(id: Long) =
    questions.filter(_.id === id).result.headOption
  private def findFQ(id: Long, user_id: Long) =
    favouriteQuestions.filter(fq => fq.question_id === id && fq.user_id === user_id)
  private def questionsReturningRow =
    questions returning questions.map(_.id) into { (q, id) =>
      q.copy(id = id)
    }
}
