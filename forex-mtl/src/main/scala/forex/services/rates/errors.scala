package forex.services.rates

object errors {

  sealed trait Error
  object Error {
    final case class OneFrameBusy(mgs: String) extends Error
    final case class OneFrameLookupFailed(msg: String) extends Error
    final case class SystemError(msg: String) extends Error
  }

}
