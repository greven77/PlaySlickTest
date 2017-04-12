package utils

import play.api.data.validation.ValidationError
import play.api.libs.json._
import play.api.libs.functional.syntax._

case class SortingPaginationWrapper(sort_by: String,
  page: Int = 1,
  resultsPerPage: Int = 25, direction: String = "desc")

object SortingPaginationWrapper {

  val sortingParameters = List("date", "votes", "title")
  val directionParameters = List("asc", "desc")

  val sortValidate = Reads.StringReads.
    filter(ValidationError("invalid parameter"))(sortByValidator(_))

  val sortValidateAnswers = Reads.StringReads.
    filter(ValidationError("invalid parameter"))(sortByValidator)

  val directionValidate = Reads.StringReads.
    filter(ValidationError("invalid parameter"))(directionValidator(_))

  implicit val sortingPaginationReads: Reads[SortingPaginationWrapper] = (
    ((JsPath \ "sort_by").read[String](sortValidate) orElse Reads.pure("date")) and
    ((JsPath \ "page").read[Int] orElse Reads.pure(1)) and
    ((JsPath \ "resultsPerPage").read[Int] orElse Reads.pure(25)) and
    ((JsPath \ "direction").read[String](directionValidate) orElse Reads.pure("desc"))
  )(SortingPaginationWrapper.apply _)

  val sortingPaginationAnswerReads: Reads[SortingPaginationWrapper] = (
    ((JsPath \ "sort_by").read[String](sortValidateAnswers) orElse Reads.pure("created_at")) and
    ((JsPath \ "page").read[Int] orElse Reads.pure(1)) and
    ((JsPath \ "resultsPerPage").read[Int] orElse Reads.pure(25)) and
    ((JsPath \ "direction").read[String](directionValidate) orElse Reads.pure("desc"))
  )(SortingPaginationWrapper.apply _)


  def sortByValidator(criteria: String) =
    List("date", "votecount", "title", "answercount", "favouritecount")
      .contains(criteria.toLowerCase)

  def sortByValidatorAnswers(criteria: String) =
    List("created_at", "updated_at", "voteCount").contains(criteria.toLowerCase)

  def directionValidator(param: String) =
    List("asc", "desc").contains(param.toLowerCase)
}
