package forex.http.rates

import forex.domain._

object Converters {
  import Protocol._

  private[http] implicit class GetApiResponseOps(val rate: Rate) extends AnyVal {
    def asGetApiResponse: GetApiResponse =
      GetApiResponse(
        from = rate.pair.from,
        to = rate.pair.to,
        price = rate.price,
        timestamp = rate.timestamp
      )
  }

  private[http] implicit class GetApiErrorResponseOps(val error: String) extends AnyVal {
    def asGetApiErrorResponse: GetApiErrorResponse =
      GetApiErrorResponse(
        error = error
      )
  }

}
