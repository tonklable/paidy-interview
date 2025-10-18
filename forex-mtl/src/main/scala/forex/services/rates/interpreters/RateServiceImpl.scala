package forex.services.rates.interpreters

import cats.Monad
import forex.services.rates.Algebra
import forex.services.rates.RedisAlgebra
import forex.services.rates.ApiAlgebra
import cats.data.EitherT
import forex.domain.Rate
import forex.services.rates.errors._
import forex.services.rates.errors.Error.SystemError
import forex.domain._
import RateServiceImpl.findOrDivideRate

class RateServiceImpl[F[_]: Monad](
    cache: RedisAlgebra[F],
    api: ApiAlgebra[F]
) extends Algebra[F] {

  override def get(pair: Rate.Pair): F[Error Either Rate] =
    EitherT(cache.getAll)
      .flatMap(
        rates =>
          EitherT.fromOption(
            findOrDivideRate(rates, pair),
            SystemError(s"Cache miss or cannot compute rate for $pair"): Error
        )
      )
      .leftFlatMap(
        _ =>
          for {
            rates <- EitherT(api.getAll)
            _ <- EitherT.liftF(cache.store(rates))
            rate <- EitherT.fromOption(findOrDivideRate(rates, pair), SystemError(s"$pair not in API response"): Error)
          } yield rate
      )
      .value
}

object RateServiceImpl {
  def findOrDivideRate(rates: List[Rate], pair: Rate.Pair): Option[Rate] =
    (pair.from, pair.to) match {
      case (f, t) if f == t =>
        Some(Rate(pair, Price(1.0000) / Price(1.0000), Timestamp.now))
      case (f, Currency.USD) =>
        rates.find(_.pair == Rate.Pair(Currency.USD, f)).map(r => Rate(pair, Price(1.0000) / r.price, Timestamp.now))
      case (Currency.USD, _) =>
        rates.find(_.pair == pair).map(r => Rate(pair, r.price / Price(1.0000), Timestamp.now))
      case _ =>
        for {
          usdToFrom <- rates.find(_.pair == Rate.Pair(Currency.USD, pair.from))
          usdToTo <- rates.find(_.pair == Rate.Pair(Currency.USD, pair.to))
        } yield Rate(pair, usdToTo.price / usdToFrom.price, Timestamp.now)
    }
}
