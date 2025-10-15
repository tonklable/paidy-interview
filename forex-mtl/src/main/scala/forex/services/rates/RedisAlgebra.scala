package forex.services.rates

import forex.domain.Rate
import errors._

trait RedisAlgebra[F[_]] {
  def get(pair: Rate.Pair): F[Error Either Rate]
  def store(rates: List[Rate]): F[Unit]
}