package forex.services.rates.interpreters

import cats.Applicative
import cats.implicits.catsSyntaxApplicativeId
import forex.config.RedisConfig
import forex.domain.Rate
import forex.services.rates.errors.Error.SystemError
import forex.services.rates.RedisAlgebra
import forex.services.rates.errors._
import cats.implicits.catsSyntaxEitherId

class RedisInterpreter[F[_]: Applicative](client: RedisClient[F], config: RedisConfig) extends RedisAlgebra[F] {
  override def get(pair: Rate.Pair): F[Either[Error, Rate]] = {
    println(client)
    println(config)
    (SystemError("Cannon connect to Redis"): Error).asLeft[Rate].pure[F]
  }
  override def store(rates: List[Rate]): F[Unit] = ().pure[F]
}
