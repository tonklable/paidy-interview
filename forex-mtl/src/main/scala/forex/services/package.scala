package forex

package object services {
  type RatesService[F[_]] = rates.Algebra[F]
  final val RatesServices = rates.Interpreters

  type RedisService[F[_]] = rates.RedisAlgebra[F]
  final val RedisService = rates.Interpreters

  type OneFrameService[F[_]] = rates.ApiAlgebra[F]
  final val OneFrameService = rates.Interpreters
}
