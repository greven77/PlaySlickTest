package dao

import models.{Answer, AnswerTable, Vote, VoteTable, UserTable}
import utils.{AnswerVoteWrapper, SortingPaginationWrapper}
import utils.SortingPaginationWrapper.getPage
import scala.concurrent.Future
import slick.backend.DatabaseConfig
import slick.driver.JdbcProfile
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import com.github.tototoshi.slick.MySQLJodaSupport._

class AnswerDao(val dbConfig: DatabaseConfig[JdbcProfile]) {
  import dbConfig.driver.api._

  val db = dbConfig.db
  val answers = TableQuery[AnswerTable]
  val votes = TableQuery[VoteTable]
  val users = TableQuery[UserTable]

  def add(answer: Answer): Future[Answer] =
    db.run(answersReturningRow += answer)

  def update(a2: Answer): Future[Answer] = {
    db.run(answers.filter(_.id === a2.id).map(_.content).update(a2.content) andThen
      answers.filter(_.id === a2.id).result.head
    )
  }

  def findById(id: Long): Future[Option[Answer]] =
    db.run(answers.filter(_.id === id).result.headOption)

  def remove(id: Long): Future[Int] =
    db.run(answers.filter(_.id === id).delete)

  // upvote and downvote methods here
  def vote(user_vote: Vote): Future[AnswerVoteWrapper]  = {
    val answerWithVotes = answers.filter(_.id === user_vote.answer_id).
      join(votes).on {
        case (answer, vote) => answer.id === vote.answer_id
      }.result.
      map { rows =>
        val answer = rows.filter { case (answer, vote) => answer.id == Some(vote.answer_id) }.
          map { case (answer, vote)  => answer}.head

        rows.groupBy { case (answer, vote) => answer.id.get }.
          mapValues { values =>
            val voteSum = values.map { case (answer, vote) => vote.value}.sum
            val userVoteValue = values.
              filter { case (answer, vote) => vote.user_id == user_vote.user_id }
              .map { case (answer, vote) => vote.value}.headOption
            AnswerVoteWrapper(answer, voteSum, userVoteValue.getOrElse(0))
          }.head._2
      }

    db.run(votes.insertOrUpdate(user_vote) andThen answerWithVotes)
  }

  def answerUsersQuery(question_id: Long, logged_user_id: Option[Long],
    params: SortingPaginationWrapper) = {

    val qr = answers.filter(_.question_id === question_id).
      joinLeft(votes).on(_.id === _.answer_id).
      join(users).on { case ((answer, vote), user) => answer.user_id === user.id }.
      groupBy { case ((answer, vote), user) => (answer, user) }.
      map { case ((answer, user), votes) => (answer, user,
        votes.map {
          case ((answer, vote), user) =>
            vote.map(_.value)
        }.sum.getOrElse(0),
        votes.map {
          case ((answer, vote), user) =>
            vote.filter(_.user_id === logged_user_id).map(_.value)
        }.sum.getOrElse(0)
      )
      }

    val sortPaginateQr = qr.sortBy(answerSorter(_, params)).
      drop(getPage(params.page, params.resultsPerPage)).
      take(params.resultsPerPage)
    sortPaginateQr.result
  }

  private def answerSorter (qfcount: (AnswerTable, UserTable, Rep[Int], Rep[Int]),
    settings: SortingPaginationWrapper) =
  {
    val (answer, user, voteCount, userVoteValue) = qfcount
    settings match {
      case SortingPaginationWrapper(sort_by,_,_,direction)
          if sort_by == "created_at" && direction == "desc" => answer.created_at.desc
      case SortingPaginationWrapper(sort_by,_,_,direction)
          if sort_by == "created_at" && direction == "asc" => answer.created_at.asc
      case SortingPaginationWrapper(sort_by,_,_,direction)
          if sort_by == "updated_at" && direction == "desc" => answer.updated_at.desc
      case SortingPaginationWrapper(sort_by,_,_,direction)
          if sort_by == "updated_at" && direction == "asc" => answer.updated_at.asc
      case SortingPaginationWrapper(sort_by,_,_,direction)
          if sort_by == "votecount" && direction == "asc" => voteCount.asc
      case SortingPaginationWrapper(sort_by,_,_,direction)
          if sort_by == "votecount" && direction == "desc" => voteCount.desc
      case SortingPaginationWrapper(_,_,_,_) => answer.created_at.desc
    }
  }

  private def answersReturningRow =
    answers returning answers.map(_.id) into { (a, id) =>
      a.copy(id = Some(id))
    }
}
