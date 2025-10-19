package forex.config

import scala.concurrent.duration.FiniteDuration

case class ApplicationConfig(
    http: HttpConfig,
    oneFrame: OneFrameConfig,
    redis: RedisConfig,
    auth: AuthConfig
)

case class OneFrameConfig(
    url: String,
    token: String,
    timeout: FiniteDuration
)

case class RedisConfig(
    url: String,
    ttl: FiniteDuration
)

case class HttpConfig(
    host: String,
    port: Int,
    timeout: FiniteDuration
)

case class AuthConfig(
    authToken: String
)