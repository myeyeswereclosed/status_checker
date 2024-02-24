package io.vbelov.status_checker.items_storage

import mongo4cats.bson.Document
import mongo4cats.collection.GenericMongoCollection
import mongo4cats.database.GenericMongoDatabase
import mongo4cats.zio.ZMongoClient
import zio.{Task, ZLayer}
import zio.stream.ZStream

object Mongo {
  private type T = { type λ[β$2$] = ZStream[Any, Throwable, β$2$] }

  private type Database = GenericMongoDatabase[Task, T#λ]

  type Collection = GenericMongoCollection[Task, Document, T#λ]

  final case class Connection(
    client: ZLayer[Any, Throwable, ZMongoClient],
    database: ZLayer[ZMongoClient, Throwable, Database]
  )
}
