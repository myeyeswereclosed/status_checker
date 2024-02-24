package io.vbelov.status_checker.status_server

import io.vbelov.status_checker.config.Config.StatusServerConfig
import sttp.model.Uri
import zio.Random
import zio.ZIO
import zio.durationInt
import zio.http._

case class StatusServerMock(uri: Uri, rateLimit: RateLimit)

object StatusServerMock {
  private val app: HttpApp[Any] =
    Routes(
      Method.GET / "status" ->
        handler {
          for {
            responseTime <- Random.nextIntBetween(500, 1000).map(_.millis)
            _            <- ZIO.sleep(responseTime)
          } yield Response.text(scala.util.Random.shuffle(List("PENDING", "SUCCESS")).head)
        }
    ).toHttpApp

  def run(config: StatusServerConfig): ZIO[Any, Throwable, Unit] =
    for {
      _ <- ZIO.log("Starting status server")
      _ <- Server.serve(app).provide(Server.defaultWithPort(config.port))
    } yield ()
}
