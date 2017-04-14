package models

import play.api.libs.json._

case class QuestionThread(question: Option[Question], tags: Seq[Tag],
  answers: Seq[(Answer, User, Int, Int)])

object QuestionThread {
  implicit val answerUserWrites = new Writes[(Answer, User, Int, Int)] {
    def writes(answerUser: (Answer, User, Int, Int)) = {
      val answer = answerUser._1
      val answerAuthor = answerUser._2
      val voteSum = answerUser._3
      val loggedUserVote = answerUser._4
      Json.obj(
        "id" -> answer.id,
        "content" -> answer.content,
        "created_at" -> answer.created_at,
        "updated_at" -> answer.updated_at,
        "created_by" -> answerAuthor.username,
        "votes" -> voteSum,
        "loggedUserVote" -> loggedUserVote
      )
    }
  }

  implicit val writes = Json.writes[QuestionThread]
}
