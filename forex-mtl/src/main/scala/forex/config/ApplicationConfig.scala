package forex.config

import scala.concurrent.duration.FiniteDuration

case class ApplicationConfig(
    http: HttpConfig,
    oneFrame: OneFrameConfig,
    redis: RedisConfig
)

case class OneFrameConfig(
    url: String,
    token: String,
    timeout: FiniteDuration
 )

case class RedisConfig(
    host: String,
    port: Int,
    ttl: FiniteDuration
 )


case class HttpConfig(
    host: String,
    port: Int,
    timeout: FiniteDuration
)
