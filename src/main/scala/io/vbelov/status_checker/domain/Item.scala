package io.vbelov.status_checker.domain

import mongo4cats.bson.Document
import mongo4cats.bson.syntax._

final case class Item(index: Int, id: String, status: String, statusChecks: Int = 0) {

  def toDocument: Document =
    Document("_id" := index, "id" := id, "status" := status, "statusChecks" := statusChecks)

}

object Item {
  def fromDocument(document: Document): Option[Item] =
    for {
      _id    <- document.getAs[Int]("_id")
      id     <- document.getAs[String]("id")
      status <- document.getAs[String]("status")
      statusChecks = document.getAs[Int]("statusChecks").getOrElse(0)
    } yield Item(_id, id, status, statusChecks)
}
