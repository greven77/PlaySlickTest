package dao

import java.sql.SQLIntegrityConstraintViolationException
import javax.naming.AuthenticationException
import models._
import slick.backend.DatabaseConfig
import slick.driver.JdbcProfile
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import com.github.tototoshi.slick.MySQLJodaSupport._
import org.joda.time.DateTime
import slick.lifted.ColumnOrdered

import utils.{QuestionFavouriteWrapper, SortingPaginationWrapper, TaggedQuestion}
import utils.SortingPaginationWrapper.getPage

class QuestionDao(val dbConfig: DatabaseConfig[JdbcProfile], answerDao: AnswerDao) extends BaseDao[Question] {
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

  def add(question: Question): Future[Option[Question]] =
    db.run(addQuestionQuery(question))

  def addWithTags(tq: TaggedQuestion): Future[TaggedQuestion] = {
    for {
      question <- db.run(addQuestionQuery(tq.question))
      tagsQuestions <- Future { getTagsQuestions(question.get.id, tq.tagIds) }
      tags <- db.run(insertTagsQuery(tagsQuestions))
      taggedQuestionIds <- db.run(getTagsIdsQuery(question.get.id))
    } yield TaggedQuestion(question.get, Some(taggedQuestionIds))
  }

  def update(tq: TaggedQuestion, user: User): Future[Option[TaggedQuestion]] = {
    val id = tq.question.id
    val title = tq.question.title
    val content = tq.question.content
    val updateQuestion = (questions.filter(_.id === id).map(q =>
      (q.title, q.content)
    ).update((title, content))
      andThen
      findByIdQuery(id.get)
    )

    val updatedTagsQuery = insertOrUpdateTagsQuery(getTagsQuestions(tq.question.id, tq.tagIds))

    for {
      question <- db.run(updateQuestion)
      tags <- db.run(updatedTagsQuery) if !tq.tagIds.isEmpty
      taggedQuestionIds <- db.run(getTagsIdsQuery(tq.question.id))
    } yield Option(TaggedQuestion(question.get, Some(taggedQuestionIds)))
  }

  def setCorrectAnswer(qId: Long, correct_answer_id: Option[Long]): Future[Option[Question]]  = {

    val filteringQuery = questions.filter(_.id === qId).
      join(answers).on {
        case (question, answer) => answer.question_id === question.id &&
          answer.id === correct_answer_id
      }.result

    val validationQuery = filteringQuery.flatMap { q =>
      q match {
        case q +: Nil => DBIO.successful(q)
        case _ => DBIO.failed(
          new SQLIntegrityConstraintViolationException("Answer does not belong to question")
        )
      }
    }

    val query = questions.filter(_.id === qId).map(_.correct_answer)
    db.run(validationQuery
      andThen query.update(correct_answer_id)
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

  def removeFavourite(fq: FavouriteQuestion): Future[Int] =
    db.run(findFQ(fq.question_id, fq.user_id).delete)

  def findAll(params: SortingPaginationWrapper): Future[Seq[QuestionFavouriteWrapper]] = {
    sortedAndPaginatedQuestions(params, questions)
  }

  override def findById(id: Long): Future[Option[Question]] =
    db.run(findByIdQuery(id))

  def findByTag(tag: String, params: SortingPaginationWrapper):
      Future[Seq[QuestionFavouriteWrapper]] = {
    val query  = questions.
      join(questionTags).on(_.id === _.question_id).
      join(tags.filter(_.text === tag)).on {
        case ((question, questionTag), tag) =>
          questionTag.tag_id === tag.id
      }.map {
        case ((question, _), _) =>
          question
      }

    sortedAndPaginatedQuestions(params,query)
  }

  def findAndRetrieveThread(id: Long, params: SortingPaginationWrapper,
    logged_user: Option[User] = None): Future[QuestionThread] = {
    val logged_user_id: Option[Long] = logged_user match {
      case Some(user) => user.id
      case _ => None
    }

    for {
      question <- db.run(findByIdQuery(id))
      tags     <- db.run(getTagsQuery(id))
      answers  <- db.run(answerDao.answerUsersQuery(id,logged_user_id, params))
    } yield QuestionThread(question, tags, answers)
  }

  override def remove(id: Long): Future[Int] =
    db.run(questions.filter(_.id === id).delete)

  private def sortedAndPaginatedQuestions(params: SortingPaginationWrapper,
    questions: Query[models.QuestionTable,models.Question,Seq]) = {
        val qr = for {
      (question, sub) <- questions
      .joinLeft(answers).on { case (question, answer) =>
          question.id === answer.question_id
      }.joinLeft(favouriteQuestions).on { case ((question, _), favouriteQuestion) =>
          question.id === favouriteQuestion.question_id
      }.groupBy { case ((question, _), _) =>
          question
      }
    } yield (question, sub.map(_._1._2).map(_.map(_.id)).countDistinct, sub.map(_._2)
      .map(_.map(_.user_id)).countDistinct)

    val sortPaginateQr = qr.sortBy(sorter(_, params)).
      drop(getPage(params.page, params.resultsPerPage)).
      take(params.resultsPerPage)

    val action = sortPaginateQr.result.
      map { case q =>
        q.map { case ((question, answer, fq)) =>
          QuestionFavouriteWrapper(question, fq, answer)
        }
      }

    db.run(action)
  }

  private def sorter (qfcount: (QuestionTable, Rep[Int], Rep[Int]), settings: SortingPaginationWrapper) =
  {
    val (question, answerCount, favouriteCount) = qfcount
    settings match {
      case SortingPaginationWrapper(sort_by,_,_,direction)
          if sort_by == "title" && direction == "desc" => question.title.desc
      case SortingPaginationWrapper(sort_by,_,_,direction)
          if sort_by == "title" && direction == "asc" => question.title.asc
      case SortingPaginationWrapper(sort_by,_,_,direction)
          if sort_by == "date" && direction == "desc" => question.created_at.desc
      case SortingPaginationWrapper(sort_by,_,_,direction)
          if sort_by == "date" && direction == "asc" => question.created_at.asc
      case SortingPaginationWrapper(sort_by,_,_,direction)
          if sort_by == "favouritecount" && direction == "asc" => favouriteCount.asc
      case SortingPaginationWrapper(sort_by,_,_,direction)
          if sort_by == "favouritecount" && direction == "desc" => favouriteCount.desc
      case SortingPaginationWrapper(sort_by,_,_,direction)
          if sort_by == "answercount" && direction == "asc" => answerCount.asc
      case SortingPaginationWrapper(sort_by,_,_,direction)
          if sort_by == "answercount" && direction == "desc" => answerCount.desc
      case SortingPaginationWrapper(_,_,_,_) => question.created_at.desc
    }
  }

  private def insertTagsQuery(updatedTags: Seq[TagsQuestions]) =
    questionTags ++= updatedTags

  private def insertOrUpdateTagsQuery(updatedTags: Seq[TagsQuestions]) =
    DBIO.sequence(updatedTags.map(questionTags.insertOrUpdate(_)))

  private def getTagsQuestions(question_id: Option[Long], tagIds: Option[Seq[Long]]): Seq[TagsQuestions] = {
    val qId = question_id.get
    tagIds.getOrElse(Seq[Long]()).map(TagsQuestions(_, qId))
  }
  private def getTagsIdsQuery(id: Option[Long]) = {
    questionTags.filter(_.question_id === id).map(_.tag_id).result
  }

  private def getTagsQuery(id: Long) =
    questionTags.filter(_.question_id === id).
      join(tags).on(_.tag_id === _.id).
      map { case (questionTag, tag) => tag }.
      result

  private def findByIdQuery(id: Long) =
    questions.filter(_.id === id).result.headOption
  private def findByTitleQuery(title: String) =
    questions.filter(_.title === title).result.headOption
  private def findFQ(id: Long, user_id: Long) =
    favouriteQuestions.filter(fq => fq.question_id === id && fq.user_id === user_id)

  private def addQuestionQuery(question: Question) =
    (questionsReturningRow += question) andThen (findByTitleQuery(question.title))
  private def questionsReturningRow =
    questions returning questions.map(_.id) into { (q, id) =>
      q.copy(id = Some(id))
    }
}
