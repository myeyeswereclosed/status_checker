package io.vbelov.status_checker.metrics

import io.vbelov.status_checker.Main.pendingItemsFilter
import io.vbelov.status_checker.items_storage.Mongo.Collection
import zio.ZIO
import zio.metrics.Metric.Gauge

object PendingItemsMetrics {
  def run(items: Collection, pendingItemsGauge: Gauge[Double]): ZIO[Any, Throwable, Unit] =
    for {
      pendingItemsNumber <- items.count(pendingItemsFilter)
      _                      <- ZIO.logInfo(s"Initial pending items number $pendingItemsNumber")
      _                      <- pendingItemsGauge.update(pendingItemsNumber)
      _ <- items.watch.stream.tap {
        change =>
          val maybeUpdatedToFinalStatus =
            Option(change.getUpdateDescription).map(_.getUpdatedFields.containsKey("status"))
          val maybeNewPendingItem = Option(change.getFullDocument)

          (maybeUpdatedToFinalStatus, maybeNewPendingItem) match {
            case (Some(true), _) => pendingItemsGauge.decrement
            case (_, Some(_)) => pendingItemsGauge.increment
            case _ => ZIO.unit
          }
      }.runDrain.forever
    } yield ()
}
