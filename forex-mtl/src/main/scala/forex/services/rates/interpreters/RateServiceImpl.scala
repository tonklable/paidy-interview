package forex.services.rates.interpreters

import cats.Monad
import forex.services.rates.Algebra
import forex.services.rates.RedisAlgebra
import forex.services.rates.ApiAlgebra
import forex.domain.Rate
import forex.services.rates.errors._
import forex.services.rates.errors.Error._
import forex.domain._
import RateServiceImpl.findOrDivideRate
import cats.implicits.{ catsSyntaxApplicativeId, catsSyntaxEitherId, toFlatMapOps }

class RateServiceImpl[F[_]: Monad](
    cache: RedisAlgebra[F],
    api: ApiAlgebra[F]
) extends Algebra[F] {

  override def get(pair: Rate.Pair): F[Error Either Rate] =
    cache.getAll.flatMap {
      case Right(rates) => findOrDivideRate(rates, pair).pure[F]
      case Left(_) =>
        api.getAll.flatMap {
          case Right(rates) =>
            cache.store(rates).flatMap(_ => findOrDivideRate(rates, pair).pure[F])
          case Left(error) => error.asLeft[Rate].pure[F]
        }
    }
}

object RateServiceImpl {
  def findOrDivideRate(rates: List[Rate], pair: Rate.Pair): Either[Error, Rate] = {
    if (rates.exists(_.price.value == 0.0))
      return SystemError(s"Cannot compute rate for $pair: zero price in rates").asLeft[Rate]
    (pair.from, pair.to) match {
      case (f, t) if f == t =>
        Rate(pair, Price(1.0000).round, Timestamp.now).asRight[Error]
      case (f, Currency.USD) =>
        rates
          .find(_.pair == Rate.Pair(Currency.USD, f))
          .map(r => Rate(pair, Price(1.0000) / r.price, Timestamp.now))
          .toRight(SystemError(s"Cannot compute rate for $pair"))
      case (Currency.USD, _) =>
        rates
          .find(_.pair == pair)
          .map(r => Rate(pair, r.price.round, Timestamp.now))
          .toRight(SystemError(s"Cannot compute rate for $pair"))
      case _ =>
        for {
          usdToFrom <- rates
                        .find(_.pair == Rate.Pair(Currency.USD, pair.from))
                        .toRight(SystemError(s"Cannot find USD -> ${pair.from} rate"))
          usdToTo <- rates
                      .find(_.pair == Rate.Pair(Currency.USD, pair.to))
                      .toRight(SystemError(s"Cannot find USD -> ${pair.to} rate"))
        } yield Rate(pair, usdToTo.price / usdToFrom.price, Timestamp.now)
    }
  }
}
