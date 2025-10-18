package forex.services.rates

import cats.effect.Sync
import cats.Monad
import forex.config.{ OneFrameConfig, RedisConfig }
import interpreters._
import org.http4s.client.Client

object Interpreters {
  def redis[F[_]: Sync](redisClient: RedisClient[F], config: RedisConfig): RedisInterpreter[F] =
    new RedisInterpreter[F](redisClient, config)
  def oneFrame[F[_]: Sync](httpClient: Client[F], config: OneFrameConfig): ApiAlgebra[F] =
    new OneFrameInterpreter[F](httpClient, config)
  def cached[F[_]: Monad](redis: RedisAlgebra[F], api: ApiAlgebra[F]): Algebra[F] = new RateServiceImpl[F](redis, api)
}
