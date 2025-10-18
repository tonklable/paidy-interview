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

class RatesHttpRoutes[F[_]: Sync](rates: RatesProgram[F]) extends Http4sDsl[F] {

  import Converters._, QueryParams._, Protocol._

  private[http] val prefixPath = "/rates"

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root :? FromQueryParam(from) +& ToQueryParam(to) =>
      rates.get(RatesProgramProtocol.GetRatesRequest(from, to)).flatMap {
        case Left(err) =>
          err match {
            case SystemError(msg)      => InternalServerError(msg.asGetApiErrorResponse)
            case RateLookupFailed(msg) => ServiceUnavailable(msg.asGetApiErrorResponse)
          }
        case Right(rate) =>
          Ok(rate.asGetApiResponse)
      }
    case GET -> Root :? _ =>
      BadRequest("Invalid request. Please provide both currencies in scope.".asGetApiErrorResponse)
  }

  val routes: HttpRoutes[F] = Router(
    prefixPath -> httpRoutes
  )

}
