package dao

import models._
import slick.backend.DatabaseConfig
import slick.driver.JdbcProfile
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import utils.{SortingPaginationWrapper, TaggedQuestion}

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
    db.run(addQuestionQuery(question))

  def addWithTags(tq: TaggedQuestion): Future[TaggedQuestion] = {
    val tags = getTagsQuestions(tq)
    for {
      question <- db.run(addQuestionQuery(tq.question))
      tags <- db.run(insertTagsQuery(tags)) if !tq.tagIds.isEmpty
      taggedQuestionIds <- db.run(getTagsIdsQuery(question.id)) if !tq.tagIds.isEmpty
    } yield TaggedQuestion(question, Some(taggedQuestionIds))
  }

  def update(tq: TaggedQuestion): Future[Option[TaggedQuestion]] = {
    val id = tq.question.id
    val title = tq.question.title
    val content = tq.question.content
    val updateQuestion = (questions.filter(_.id === id).map(q =>
      (q.title, q.content)
    ).update((title, content))
      andThen
      findByIdQuery(id.get)
    )

    val updatedTagsQuery = insertOrUpdateTagsQuery(getTagsQuestions(tq))

    for {
      question <- db.run(updateQuestion)
      tags <- db.run(updatedTagsQuery) if !tq.tagIds.isEmpty
      taggedQuestionIds <- db.run(getTagsIdsQuery(tq.question.id))
    } yield Option(TaggedQuestion(question.get, Some(taggedQuestionIds)))
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

  def removeFavourite(fq: FavouriteQuestion): Future[Int] =
    db.run(findFQ(fq.question_id, fq.user_id).delete)

  import org.joda.time.DateTime

  override def findAll(settings: SortingPaginationWrapper): Future[Seq[Question]] =
    db.run(questions.
      sortBy(_.title.desc).
      result)

  // change query in order to join tags and answers
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

  def findAndRetrieveThread(id: Long, logged_user_id: Option[Long] = None): Future[QuestionThread] = {
    val filteredAnswers = answers.filter(_.question_id === id)

    for {
      votesMap <- db.run(answerVotesQuery(filteredAnswers, logged_user_id))
      question <- db.run(findByIdQuery(id))
      tags     <- db.run(getTagsQuery(id))
      answers  <- db.run(answerUsersQuery(votesMap, filteredAnswers))
    } yield QuestionThread(question, tags, answers)
  }

  override def remove(id: Long): Future[Int] =
    db.run(questions.filter(_.id === id).delete)

  private def insertTagsQuery(updatedTags: Seq[TagsQuestions]) =
    questionTags ++= updatedTags

  private def insertOrUpdateTagsQuery(updatedTags: Seq[TagsQuestions]) =
    DBIO.sequence(updatedTags.map(questionTags.insertOrUpdate(_)))

  private def getTagsQuestions(tq: TaggedQuestion): Seq[TagsQuestions] = {
    val question = tq.question
    val tagIds = tq.tagIds
    val question_id = question.id.get
    tagIds.get.map(TagsQuestions(_, question_id))
  }
  private def getTagsIdsQuery(id: Option[Long]) = {
    questionTags.filter(_.question_id === id).map(_.tag_id).result
  }

  private def getTagsQuery(id: Long) =
    questionTags.filter(_.question_id === id).
      join(tags).on(_.tag_id === _.id).
      map { case (questionTag, tag) => tag }.
      result

  private def answerVotesQuery(answers: AnswersQuery, logged_user_id: Option[Long]): DBIO[Map[Long, (Int, Int)]] = {
    answers.
      join(votes).on(_.id === _.answer_id).result.
      map { rows =>
        rows.groupBy { case (answer, vote) => answer.id.get }.
          mapValues { values =>
            val voteSum = values.map { case (answer, vote) => vote.value}.sum
            val userVoteValue = values.
              filter { case (answer, vote) => vote.user_id == logged_user_id}
              .map { case (answer, vote) => vote.value}.headOption
            (voteSum, userVoteValue.getOrElse(0))
          }
      }
  }

  private def answerUsersQuery(votesMap: Map[Long, (Int, Int)], answers: AnswersQuery) = {
    answers.
      join(questions).on(_.question_id  === _.id).
      join(users).on { case ((answer, question), user) => question.created_by === user.id }.
      result.
      map { rows =>
        rows.map { case ((answer, question), user) =>
          (answer, user, votesMap.getOrElse(answer.id.get, (0,0)))
        }
      }
  }

  private def findByIdQuery(id: Long) =
    questions.filter(_.id === id).result.headOption
  private def findFQ(id: Long, user_id: Long) =
    favouriteQuestions.filter(fq => fq.question_id === id && fq.user_id === user_id)

  private def addQuestionQuery(question: Question) =
    questionsReturningRow += question
  private def questionsReturningRow =
    questions returning questions.map(_.id) into { (q, id) =>
      q.copy(id = id)
    }
}
