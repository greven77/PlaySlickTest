package utils

import models.Question

case class TaggedQuestion(question: Question, tagIds: Option[Seq[Long]] = None)

object TaggedQuestion {
  import play.api.libs.json._
  import play.api.libs.json.Reads
  import play.api.libs.functional.syntax._

  implicit val taggedQuestionReads: Reads[TaggedQuestion] = (
    (JsPath \ "question").read[Question] and
    (JsPath \ "tagIds").readNullable[Seq[Long]]
  )(TaggedQuestion.apply _)

  implicit val taggedQuestionWrites: Writes[TaggedQuestion] = Json.writes[TaggedQuestion]
}
