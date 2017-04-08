package utils

import models.Answer
import play.api.libs.json._

case class AnswerVoteWrapper(answer: Answer, voteCount: Int, current_user_vote: Int)

object AnswerVoteWrapper {
  implicit val answerVoteWrites = Json.writes[AnswerVoteWrapper]
}
