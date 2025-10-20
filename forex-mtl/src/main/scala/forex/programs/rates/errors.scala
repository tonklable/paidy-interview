package forex.programs.rates

import forex.services.rates.errors.{ Error => RatesServiceError }

object errors {

  sealed trait Error extends Exception
  object Error {
    final case class ServiceBusy(msg: String) extends Exception(msg) with Error
    final case class RateLookupFailed(msg: String) extends Exception(msg) with Error
    final case class SystemError(msg: String) extends Exception(msg) with Error
  }

  def toProgramError(error: RatesServiceError): Error = error match {
    case RatesServiceError.OneFrameBusy(msg) => Error.ServiceBusy(msg)
    case RatesServiceError.OneFrameLookupFailed(msg) => Error.RateLookupFailed(msg)
    case RatesServiceError.SystemError(msg)          => Error.SystemError(msg)
  }
}
