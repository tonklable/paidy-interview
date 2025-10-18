package forex.services.rates

import forex.domain.Rate
import errors._

trait RedisAlgebra[F[_]] {
  def getAll: F[Error Either List[Rate]]
  def store(rates: List[Rate]): F[Unit]
}
