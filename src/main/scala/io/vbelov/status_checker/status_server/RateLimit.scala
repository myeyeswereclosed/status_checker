package io.vbelov.status_checker.status_server

import zio._

case class RateLimit(requests: Int, period: Duration, requestTimeout: Duration = 5.seconds) {
  def waitPeriod: zio.Duration = period + requestTimeout
}
