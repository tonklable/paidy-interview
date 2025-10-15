package forex.services.rates.interpreters

import cats.Monad
import forex.services.rates.Algebra
import forex.services.rates.RedisAlgebra
import forex.services.rates.ApiAlgebra
import cats.data.EitherT
//import cats.syntax.applicative._
//import cats.syntax.either._
import forex.domain.Rate
import forex.services.rates.errors._
import forex.services.rates.errors.Error.SystemError
//import forex.domain.RateCalculation

class RateServiceImpl[F[_]: Monad](
    cache: RedisAlgebra[F],
    api: ApiAlgebra[F]
    ) extends Algebra[F] {

  override def get(pair: Rate.Pair): F[Error Either Rate] = {
    EitherT(cache.get(pair))
      .leftFlatMap(_ => EitherT(api.getAll()).flatMap(rates => EitherT.fromEither(rates.find(_.pair == pair).toRight[Error](SystemError(s"$pair not in API response"))))).value
  }
}