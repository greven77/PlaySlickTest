package utils

import models.Question
import play.api.libs.json._

case class QuestionFavouriteWrapper(question: Question, favouriteCount: Int,
  answerCount: Int,
  current_user_favourited: Option[Boolean] = Some(false))

object QuestionFavouriteWrapper {
  implicit val answerVoteWrites = Json.writes[QuestionFavouriteWrapper]
}
