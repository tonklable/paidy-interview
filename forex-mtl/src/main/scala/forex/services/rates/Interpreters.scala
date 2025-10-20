package forex.services.rates

import cats.effect._
import cats.effect.concurrent.Semaphore
import forex.config.{ OneFrameConfig, RedisConfig }
import interpreters._
import org.http4s.client.Client

object Interpreters {
  def redis[F[_]: Sync](redisClient: RedisClient[F], config: RedisConfig): RedisInterpreter[F] =
    new RedisInterpreter[F](redisClient, config)
  def oneFrame[F[_]: Sync](httpClient: Client[F], config: OneFrameConfig): ApiAlgebra[F] =
    new OneFrameInterpreter[F](httpClient, config)
  def cached[F[_]: Sync](redis: RedisAlgebra[F], api: ApiAlgebra[F], guard: Semaphore[F]): Algebra[F] =
    new RateServiceImpl[F](redis, api, guard)
}
