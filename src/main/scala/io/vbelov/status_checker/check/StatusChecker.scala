package io.vbelov.status_checker.check

import io.vbelov.status_checker.Main.pendingItemsFilter
import io.vbelov.status_checker.domain.Item
import io.vbelov.status_checker.status_server.{RateLimit, StatusServerMock}
import io.vbelov.status_checker.items_storage.Mongo.Collection
import mongo4cats.operations.Filter
import mongo4cats.operations.Update
import sttp.capabilities
import sttp.capabilities.zio.ZioStreams
import sttp.client3._
import sttp.client3.httpclient.zio.HttpClientZioBackend
import sttp.model.Uri
import zio.ZIO
import zio._

import java.util.concurrent.TimeUnit

object StatusChecker {
  private val updateDirectly = true

  private type Client = SttpBackend[Task, ZioStreams with capabilities.WebSockets]

  def run(statusServer: StatusServerMock, items: Collection): ZIO[Any, Throwable, Unit] =
    for {
      client <- HttpClientZioBackend()
      _ <- checkStatuses(items, statusServer, client)
    } yield ()

  private def checkStatuses(
    itemsCollection: Collection,
    statusServer: StatusServerMock,
    client: Client
  ): ZIO[Any, Throwable, Unit] =
    for {
      _ <- itemsCollection
        .find(pendingItemsFilter)
        .stream
        .map(Item.fromDocument)
        .groupedWithin(statusServer.rateLimit.requests, statusServer.rateLimit.period)
        .mapZIO { items =>
          for {
            startTime <- Clock.currentTime(TimeUnit.SECONDS)
            itemsToUpdate <- ZIO.foreachPar(items.flatten.toList)(checkStatus(_, client, statusServer.uri))
            _       <- update(itemsToUpdate, itemsCollection)
            endTime <- Clock.currentTime(TimeUnit.SECONDS)
            expectedNextStartAt = startTime + statusServer.rateLimit.waitPeriod.toSeconds
            waitFor             = expectedNextStartAt - endTime
            _ <- (ZIO.log(s"Waiting $waitFor seconds to start next check") *>
              ZIO.sleep(waitFor.seconds)).when(waitFor > 0)
          } yield ()
        }
        .runDrain
        .forever
    } yield ()

  private def checkStatus(item: Item, client: Client, url: Uri): ZIO[Any, Throwable, Item] =
    for {
      _              <- ZIO.logInfo(s"Checking status for item ${item.index}")
      statusResponse <- client.send(basicRequest.get(url))
      status = statusResponse.body.getOrElse(item.status)
      _ <- ZIO.logInfo(s"Result for item ${item.index}: $status")
    } yield item.copy(status = status, statusChecks = item.statusChecks + 1)

  private def update(
    items: List[Item],
    collection: Collection
  ): ZIO[Any, Throwable, Unit] =
    for {
      _ <- Option
        .when(updateDirectly) {
          //      ZIO.foreachPar(items)(item =>
          ZIO.foreach(items)(item =>
            collection.updateOne(
              Filter.eq("_id", item.index),
              Update
                .set("status", item.status)
                .set("statusChecks", item.statusChecks)
            ) *> ZIO.logInfo(s"Item ${item.index} updated")
          )
        }
        .getOrElse(updateViaKafka(items))
    } yield ()

  // emulation
  private def updateViaKafka(items: List[Item]): UIO[Unit] = ZIO.log(
    s"Producing update messages to Kafka: ${items.size}"
  )
}
