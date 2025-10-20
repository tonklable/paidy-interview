package forex.services.rates.interpreters

import cats.Monad
import cats.implicits.{ catsSyntaxApplicativeId, catsSyntaxEitherId, toFlatMapOps }
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
    client.get(key).flatMap {
      case Some(json) =>
        decode[List[RateJson]](json) match {
          case Left(err)       => (SystemError(s"Parse error: ${err.getMessage}"): Error).asLeft[List[Rate]].pure[F]
          case Right(rateJson) => rateJson.flatMap(toRate).asRight[Error].pure[F]
        }
      case None =>
        (SystemError(s"Cache miss"): Error).asLeft[List[Rate]].pure[F]
    }

  override def store(rates: List[Rate]): F[Unit] = {
    val jsonRates = rates.map(toJson)
    val json      = jsonRates.asJson.noSpaces
    client.setEx(key, json, config.ttl)
  }
}
