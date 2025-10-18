package forex.services.rates.interpreters

import cats.Monad
import cats.data.EitherT
import cats.implicits.toBifunctorOps
import forex.config.RedisConfig
import forex.domain.Rate
import io.circe.parser.decode
import forex.services.rates.RedisAlgebra
import forex.services.rates.errors.Error.SystemError
import forex.services.rates.errors._
import forex.services.rates.interpreters.RateUtils.{ toJson, toRate }
import io.circe.syntax.EncoderOps

class RedisInterpreter[F[_]: Monad](client: RedisClient[F], config: RedisConfig) extends RedisAlgebra[F] {

  private val key = "rates"

  override def getAll: F[Error Either List[Rate]] =
    (for {
      jsonOption <- EitherT.liftF(client.get(key))
      json <- EitherT.fromOption[F](jsonOption, SystemError(s"Cache miss for key $key"): Error)
      jsonRates <- EitherT.fromEither[F](
                    decode[List[RateJson]](json).leftMap(e => SystemError(s"Parse error: ${e.getMessage}"): Error)
                  )
      rates = jsonRates.flatMap(toRate)
    } yield rates).value
  override def store(rates: List[Rate]): F[Unit] = {
    val jsonRates = rates.map(toJson)
    val json      = jsonRates.asJson.noSpaces
    client.setEx(key, json, config.ttl)
  }
}
