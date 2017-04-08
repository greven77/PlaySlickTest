package utils

import play.api.data.validation.ValidationError
import play.api.libs.json._
import play.api.libs.functional.syntax._

case class SortingPaginationWrapper(sort_by: Option[String] = Some("date"),
  page: Option[Int] = Some(1) ,
  resultsPerPage: Option[Int] = Some(25), direction: Option[String] = Some("desc"))

object SortingPaginationWrapper {
  val sortingParameters = List("date", "votes")
  val directionParameters = List("asc", "desc")

  val sortValidate = Reads.StringReads.
    filter(ValidationError("invalid parameter"))(sortByValidator(_))

  val directionValidate = Reads.StringReads.
    filter(ValidationError("invalid parameter"))(directionValidator(_))

  implicit val sortingPaginationReads: Reads[SortingPaginationWrapper] = (
    (JsPath \ "sort_by").readNullable[String](sortValidate) and
    (JsPath \ "page").readNullable[Int] and
    (JsPath \ "resultsPerPage").readNullable[Int] and
    (JsPath \ "direction").readNullable[String](directionValidate)
  )(SortingPaginationWrapper.apply _)

  def sortByValidator(param: String) =
    List("date", "votes").contains(param.toLowerCase)

  def directionValidator(param: String) =
    List("asc", "desc").contains(param.toLowerCase)
}
