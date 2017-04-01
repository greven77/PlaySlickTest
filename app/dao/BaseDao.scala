package dao

import scala.concurrent.Future

trait BaseDao[T] {
  def add(o: T): Future[T]
  def update(o: T): Future[Unit]
  def findAll(): Future[Seq[T]]
  def remove(id: Long): Future[Int]
  def findById(id: Long): Future[Option[T]]
}
