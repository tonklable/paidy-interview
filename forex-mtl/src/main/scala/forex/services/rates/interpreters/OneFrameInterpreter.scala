package forex.services.rates.interpreters

import forex.services.rates.ApiAlgebra
import forex.config.OneFrameConfig
import forex.domain.{Currency, Rate}
import forex.services.rates.errors._
import org.http4s.Method.GET
import cats.implicits._
import org.http4s.{Header, ParseFailure, Query, Request, Uri}
import org.http4s.client.Client
import org.typelevel.ci.CIString
import org.http4s.EntityDecoder
import org.http4s.circe.jsonOf
import RateUtils.toRate
import forex.services.rates.errors.Error.{OneFrameLookupFailed, SystemError}
import cats.effect._

class OneFrameInterpreter[F[_]: Sync](client: Client[F], config: OneFrameConfig) extends ApiAlgebra[F] {

  implicit val ratesDecoder: EntityDecoder[F, List[RateJson]] = {
    jsonOf[F, List[RateJson]]
  }

  override def getAll: F[Error Either List[Rate]] = {
    val allPairs = OneFrameInterpreter.buildAllPairs(Currency.all)
    OneFrameInterpreter.buildUri(config.url, allPairs) match {
      case Left(error) => (SystemError(s"Invalid URI: ${error.details}"): Error).asLeft[List[Rate]].pure[F]
      case Right(uri) =>
        val request = OneFrameInterpreter.buildRequest[F](uri, config.token)
        client.expect[List[RateJson]](request).attempt.flatMap {
          case Left(error) =>
            (OneFrameLookupFailed(s"API failed: ${error.getMessage}"): Error).asLeft[List[Rate]].pure[F]
          case Right(jsonRates) => jsonRates.flatMap(toRate).asRight[Error].pure[F]
        }
    }
  }
}

object OneFrameInterpreter {
  def buildAllPairs(currencyList: List[Currency]): List[String] =
    for {
      from <- List("USD")
      to <- currencyList.map(_.toString) if from != to
    } yield s"$from$to"

  def buildUri(base: String, pairs: List[String]): Either[ParseFailure, Uri] =
    Uri.fromString(base).map { uri =>
      val query = Query.fromPairs(pairs.map("pair" -> _): _*)
      uri.copy(query = query)
    }

  def buildRequest[F[_]](uri: Uri, token: String): Request[F] = {
    val header = Header.Raw(CIString("token"), token)
    Request[F](GET, uri).withHeaders(header)
  }

}
