package io.vbelov.status_checker

import io.vbelov.status_checker.items_creation.ItemsCreation
import io.vbelov.status_checker.check.StatusChecker
import io.vbelov.status_checker.config.Config.MongoConfig
import io.vbelov.status_checker.metrics.PendingItemsMetrics
import io.vbelov.status_checker.status_server.{RateLimit, StatusServerMock}
import mongo4cats.operations.Filter
import mongo4cats.zio.{ZMongoClient, ZMongoDatabase}
import zio._
import zio.http._
import zio.metrics.Metric
import zio.metrics.Metric.Gauge
import zio.metrics.connectors.prometheus.PrometheusPublisher
import zio.metrics.connectors.MetricsConfig
import zio.metrics.connectors.prometheus._
import pureconfig._
import pureconfig.generic.auto._
import io.vbelov.status_checker.config.{Config => AppConfig}
import io.vbelov.status_checker.items_storage.Mongo.Connection
import sttp.model.Uri

object Main extends ZIOAppDefault {
  private val metricsConfig = ZLayer.succeed(MetricsConfig(1.seconds))

  private val app =
    Routes(
      Method.GET / "metrics" ->
        handler {
          ZIO.serviceWithZIO[PrometheusPublisher](_.get.map(Response.text))
        }
    ).toHttpApp

  private val pendingItemsGauge: Gauge[Double] = Metric.gauge("pending_items")

  val pendingItemsFilter: Filter = Filter.eq("status", "PENDING")

  override def run = {
    val config = ConfigSource.default.loadOrThrow[AppConfig]
    val connection  = mongoConnection(config.mongo)

    val statusServerConfig = config.statusServer
    val statusServerMock =
      Uri.parse(config.statusServer.url).map(
        url =>
          StatusServerMock(
            url,
            RateLimit(
              statusServerConfig.rateLimit.requests,
              Duration.fromScala(statusServerConfig.rateLimit.interval),
              Duration.fromScala(statusServerConfig.requestTimeout)
            )
        )
      )

    statusServerMock.map(
      statusServer =>
        ZIO
          .serviceWithZIO[ZMongoDatabase](_.getCollection("item"))
          .flatMap(
            itemsCollection =>
              ZIO.collectAllPar(
                List(
                  StatusServerMock.run(config.statusServer),
                  Server
                    .serve(app)
                    .provide(Server.default, metricsConfig, publisherLayer, prometheusLayer),
                  ItemsCreation.run(itemsCollection),
                  StatusChecker.run(statusServer, itemsCollection),
                  PendingItemsMetrics.run(itemsCollection, pendingItemsGauge)
                )
              )
          ).provide(connection.client, connection.database)
    ).getOrElse(ZIO.logInfo("Invalid status server config"))
  }

  private def mongoConnection(mongo: MongoConfig): Connection =
    Connection(
      client = ZLayer.scoped(ZMongoClient.fromConnectionString(mongo.connectionString)),
      database = ZLayer.fromZIO(ZIO.serviceWithZIO[ZMongoClient](_.getDatabase(mongo.database)))
    )
}
