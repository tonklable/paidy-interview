package forex.services.rates

import forex.domain.Rate
import errors._

trait ApiAlgebra[F[_]] {
  def getAll: F[Error Either List[Rate]]
}
