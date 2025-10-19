package forex.http
package rates

import cats.effect.Sync
import cats.syntax.flatMap._
import forex.programs.RatesProgram
import forex.programs.rates.errors.Error.{ RateLookupFailed, SystemError }
import forex.programs.rates.{ Protocol => RatesProgramProtocol }
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import org.typelevel.log4cats.Logger

class RatesHttpRoutes[F[_]: Sync](rates: RatesProgram[F], logger: Logger[F]) extends Http4sDsl[F] {

  import Converters._, QueryParams._, Protocol._

  private[http] val prefixPath = "/rates"

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root :? FromQueryParam(from) +& ToQueryParam(to) =>
      rates.get(RatesProgramProtocol.GetRatesRequest(from, to)).flatMap {
        case Left(err) =>
          err match {
            case SystemError(e) =>
              logger.error(e) >>
                InternalServerError("Internal error. Please contact the developer.".asGetApiErrorResponse)
            case RateLookupFailed(e) =>
              logger.error(e) >>
                ServiceUnavailable(
                  "Unable to reach external rate service. Please try again later.".asGetApiErrorResponse
                )
          }
        case Right(rate) =>
          Ok(rate.asGetApiResponse)
      }
    case GET -> Root :? _ =>
      logger.error("Currency is out of scope or not sufficient") >>
      BadRequest("Invalid request. Please provide both currencies in scope.".asGetApiErrorResponse)
  }

  val routes: HttpRoutes[F] = Router(
    prefixPath -> httpRoutes
  )

}
