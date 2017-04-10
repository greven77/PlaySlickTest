package dao

import models.{Answer, AnswerTable, Vote, VoteTable}
import utils.AnswerVoteWrapper
import scala.concurrent.Future
import slick.backend.DatabaseConfig
import slick.driver.JdbcProfile
import play.api.libs.concurrent.Execution.Implicits.defaultContext

class AnswerDao(dbConfig: DatabaseConfig[JdbcProfile]) {
  import dbConfig.driver.api._

  val db = dbConfig.db
  val answers = TableQuery[AnswerTable]
  val votes = TableQuery[VoteTable]

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
  private def answersReturningRow =
    answers returning answers.map(_.id) into { (a, id) =>
      a.copy(id = id)
    }
}
