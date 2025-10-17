package forex

import cats.effect.{Concurrent, Timer}
import forex.config.ApplicationConfig
import forex.http.rates.RatesHttpRoutes
import forex.services._
import forex.programs._
import org.http4s._
import org.http4s.client.Client
import org.http4s.implicits._
import org.http4s.server.middleware.{AutoSlash, Timeout}
import forex.services.rates.interpreters.RedisClient
import forex.services.rates.interpreters.RedisInterpreter

class Module[F[_]: Concurrent: Timer](
                                       config: ApplicationConfig,
                                       httpClient: Client[F],
                                       redisClient: RedisClient[F]
                                     ) {

  private val redisService: RedisInterpreter[F] = RedisService.redis[F](redisClient, config.redis)

  private val oneFrameService: OneFrameService[F] =  OneFrameService.oneFrame[F](httpClient, config.oneFrame)

  private val ratesService: RatesService[F] = RatesServices.cached[F](redisService, oneFrameService)

  private val ratesProgram: RatesProgram[F] = RatesProgram[F](ratesService)

  private val ratesHttpRoutes: HttpRoutes[F] = new RatesHttpRoutes[F](ratesProgram).routes

  type PartialMiddleware = HttpRoutes[F] => HttpRoutes[F]
  type TotalMiddleware   = HttpApp[F] => HttpApp[F]

  private val routesMiddleware: PartialMiddleware = {
    { http: HttpRoutes[F] =>
      AutoSlash(http)
    }
  }

  private val appMiddleware: TotalMiddleware = { http: HttpApp[F] =>
    Timeout(config.http.timeout)(http)
  }

  private val http: HttpRoutes[F] = ratesHttpRoutes

  val httpApp: HttpApp[F] = appMiddleware(routesMiddleware(http).orNotFound)

}
