package models

import play.api.libs.json._

case class QuestionThread(question: Option[Question], tags: Seq[Tag],
  answers: Seq[(Answer, User, (Int, Int))])

object QuestionThread {
  implicit val answerUserWrites = new Writes[(Answer, User, (Int, Int))] {
    def writes(answerUser: (Answer, User, (Int, Int))) = {
      val answer = answerUser._1
      val user = answerUser._2
      val votes = answerUser._3
      val voteSum = votes._1
      val loggedUserVote = votes._2
      Json.obj(
        "id" -> answer.id,
        "content" -> answer.content,
        "created_at" -> answer.created_at,
        "updated_at" -> answer.updated_at,
        "created_by" -> user.username,
        "votes" -> voteSum,
        "loggedUserVote" -> loggedUserVote
      )
    }
  }

  implicit val writes = Json.writes[QuestionThread]
}
