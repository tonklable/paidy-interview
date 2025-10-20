package forex

import cats.effect.concurrent.Semaphore
import cats.effect.{ Concurrent, Timer }
import forex.config.ApplicationConfig
import forex.http.auth.TokenAuth
import forex.http.rates.RatesHttpRoutes
import forex.services._
import forex.programs._
import org.http4s._
import org.http4s.client.Client
import org.http4s.implicits._
import org.http4s.server.middleware.{ AutoSlash, Timeout }
import forex.services.rates.interpreters.RedisClient
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

class Module[F[_]: Concurrent: Timer](
    config: ApplicationConfig,
    httpClient: Client[F],
    redisClient: RedisClient[F],
    guard: Semaphore[F]
) {

  private val logger: Logger[F] = Slf4jLogger.getLogger[F]

  private val redisService: RedisService[F] = RedisService.redis[F](redisClient, config.redis)

  private val oneFrameService: OneFrameService[F] = OneFrameService.oneFrame[F](httpClient, config.oneFrame)

  private val ratesService: RatesService[F] = RatesServices.cached[F](redisService, oneFrameService, guard)

  private val ratesProgram: RatesProgram[F] = RatesProgram[F](ratesService)

  private val ratesHttpRoutes: HttpRoutes[F] = new RatesHttpRoutes[F](ratesProgram, logger).routes

  type PartialMiddleware = HttpRoutes[F] => HttpRoutes[F]
  type TotalMiddleware   = HttpApp[F] => HttpApp[F]

  private val authenticatedRoutes: HttpRoutes[F] =
    TokenAuth[F](config.auth.authToken)(ratesHttpRoutes, logger)

  private val routesMiddleware: PartialMiddleware = {
    { http: HttpRoutes[F] =>
      AutoSlash(http)
    }
  }

  private val appMiddleware: TotalMiddleware = { http: HttpApp[F] =>
    Timeout(config.http.timeout)(http)
  }

  private val http: HttpRoutes[F] = authenticatedRoutes

  val httpApp: HttpApp[F] = appMiddleware(routesMiddleware(http).orNotFound)

}
