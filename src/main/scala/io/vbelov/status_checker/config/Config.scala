package io.vbelov.status_checker.config

import io.vbelov.status_checker.config.Config.StatusServerConfig.RateLimitConfig
import io.vbelov.status_checker.config.Config.{MongoConfig, StatusServerConfig}

import scala.concurrent.duration.Duration

case class Config(mongo: MongoConfig, statusServer: StatusServerConfig)

object Config {
 case class MongoConfig(
    connectionString: String,
    database: String
  )

  case class StatusServerConfig(
   host: String,
   port: Int,
   url: String,
   rateLimit: RateLimitConfig,
   requestTimeout: Duration
 )

  object StatusServerConfig {
    case class RateLimitConfig(requests: Int, interval: Duration)
  }
}
