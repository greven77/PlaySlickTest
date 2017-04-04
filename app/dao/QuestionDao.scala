package dao

import models._
import slick.backend.DatabaseConfig
import slick.driver.JdbcProfile
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

class QuestionDao(val dbConfig: DatabaseConfig[JdbcProfile]) extends BaseDao[Question] {
  import dbConfig.driver.api._

  val db = dbConfig.db
  val questions = TableQuery[QuestionTable]
  val favouriteQuestions = TableQuery[FavouriteQuestionTable]
  val answers = TableQuery[AnswerTable]
  val users = TableQuery[UserTable]
  val votes = TableQuery[VoteTable]
  val tags = TableQuery[TagTable]
  val questionTags = TableQuery[TagsQuestionsTable]
  type AnswersQuery = Query[AnswerTable, Answer, Seq]

  override def add(question: Question): Future[Question] =
    db.run(questionsReturningRow += question)

  override def update(id: Long, title:String, content: String): Future[Option[Question]] = {
    db.run(questions.filter(_.id === id).map(q =>
      (q.title, q.content)
    ).update((title, content))
      andThen
      findByIdQuery(id)
    )
  }

  def setCorrectAnswer(qId: Long, correct_answer_id: Option[Long]): Future[Option[Question]]  = {
    val query = questions.filter(_.id === qId).map(_.correct_answer)
    db.run(query.update(correct_answer_id)
      andThen findByIdQuery(qId))
  }

  def markFavourite(favouriteQuestion: FavouriteQuestion): Future[FavouriteQuestion] = {
    val markFavourite =
      favouriteQuestions += favouriteQuestion
    val question_id = favouriteQuestion.question_id
    val user_id = favouriteQuestion.user_id
    val retrieveFavourite = findFQ(question_id, user_id).result.head

    db.run(markFavourite andThen retrieveFavourite)
  }

  def removeFavourite(id: Long, user_id: Long): Future[Int] =
    db.run(findFQ(id, user_id).delete)

  override def findAll(): Future[Seq[Question]] = db.run(questions.result)

  override def findById(id: Long): Future[Option[Question]] =
    db.run(findByIdQuery(id))

  def findByTag(tag: String): Future[Seq[Question]] = {
    val findQuestionsQuery = tags.filter(_.text === tag).
      join(questionTags).on(_.id === _.tag_id).
      join(questions).on {
        case ((tag, questionTag), question) => questionTag.question_id === question.id
      }.result

    val action = for {
      questionsResult <- findQuestionsQuery
    } yield {
      questionsResult.map {
          case ((tag, questionTag), question) => question
      }
    }
    db.run(action)
  }

  def findAndRetrieveThread(id: Long): Future[QuestionThread] = {
    val filteredAnswers = answers.filter(_.question_id === id)

    for {
      votesMap <- db.run(answerVotesQuery(filteredAnswers))
      question <- db.run(findByIdQuery(id))
      answers       <- db.run(answerUsersQuery(votesMap, filteredAnswers))
    } yield QuestionThread(question, (answers))
  }

  override def remove(id: Long): Future[Int] =
    db.run(questions.filter(_.id === id).delete)

  private def answerVotesQuery(answers: AnswersQuery): DBIO[Map[Long, Int]] = {
    answers.
      join(votes).on(_.id === _.answer_id).result.
      map { rows =>
        rows.groupBy { case (answer, vote) => answer.id.get }.
        mapValues(values => values.map { case (answer, vote) => vote.value}.sum)
      }
  }

  private def answerUsersQuery(votesMap: Map[Long, Int], answers: AnswersQuery) = {
    answers.
      join(questions).on(_.question_id  === _.id).
      join(users).on { case ((answer, question), user) => question.created_by === user.id }.
      result.
      map { rows =>
        rows.map { case ((answer, question), user) =>
          (answer, user, votesMap.getOrElse(answer.id.get, 0))
        }
      }
  }

  private def findByIdQuery(id: Long) =
    questions.filter(_.id === id).result.headOption
  private def findFQ(id: Long, user_id: Long) =
    favouriteQuestions.filter(fq => fq.question_id === id && fq.user_id === user_id)
  private def questionsReturningRow =
    questions returning questions.map(_.id) into { (q, id) =>
      q.copy(id = id)
    }
}
