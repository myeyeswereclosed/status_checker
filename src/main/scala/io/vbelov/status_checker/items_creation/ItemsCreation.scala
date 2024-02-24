package io.vbelov.status_checker.items_creation

import io.vbelov.status_checker.domain.Item
import io.vbelov.status_checker.items_storage.Mongo.Collection
import mongo4cats.operations.Sort
import zio.ZIO
import zio._
import zio.stream.ZStream

import java.util.UUID

// emulates real creation of items
object ItemsCreation {
  private def creationInterval: zio.Duration = scala.util.Random.between(200, 5000).millis

  def run(items: Collection): ZIO[Any, Throwable, Unit] = {
    for {
      maybeLastItem <- items.find.sort(Sort.desc("_id")).first
      lastIndex = maybeLastItem.flatMap(Item.fromDocument).map(_.index).getOrElse(0)
      _ <- create(lastIndex, items)
    } yield ()
  }

  private def create(
      lastIndex: Int,
      collection: Collection
    ): ZIO[Any, Throwable, Unit] =
    ZStream
      .iterate(lastIndex + 1)(_ + 1)
      .map(index => Item(index, UUID.randomUUID().toString, "PENDING"))
      .throttleShape(1, creationInterval)(_ => 1)
      .tap(item => collection.insertOne(item.toDocument))
      .tap(item => ZIO.logInfo(s"$item created"))
      .runDrain
}
