package forex.services.rates.interpreters

import forex.services.rates.ApiAlgebra
import cats.effect.Sync
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
import cats.implicits.catsSyntaxApplicativeError
import cats.data.EitherT
import RateUtils.toRate



class OneFrameInterpreter[F[_]: Sync](client: Client[F], config: OneFrameConfig) extends ApiAlgebra[F] {

  implicit val ratesDecoder: EntityDecoder[F, List[RateJson]] = {
    jsonOf[F, List[RateJson]]
  }


  override def getAll: F[Either[Error, List[Rate]]] = {
    val allPairs = OneFrameInterpreter.buildAllPairs(Currency.all)
    (for {
      uri <- EitherT.fromEither[F](
        OneFrameInterpreter.buildUri(config.url, allPairs)
          .leftMap(e => Error.OneFrameLookupFailed(s"Invalid URI: ${e.details}"): Error)
      )

      request = OneFrameInterpreter.buildRequest[F](uri, config.token)

      jsonRates <- client.expect[List[RateJson]](request)
        .attemptT
        .leftMap(e => Error.OneFrameLookupFailed(s"API failed: ${e.getMessage}"): Error)

      rates = jsonRates.flatMap(toRate)

    } yield rates).value
  }
}

object OneFrameInterpreter {
  def buildAllPairs(currencyList: List[Currency]): List[String]= for {
    from <- List("USD")
    to <- currencyList.map(_.toString) if from != to
  } yield s"$from$to"

  def buildUri(base: String, pairs: List[String]): Either[ParseFailure, Uri] = {
    Uri.fromString(base).map { uri =>
      val query = Query.fromPairs(pairs.map("pair" -> _): _*)
      uri.copy(query = query)
    }
  }

  def buildRequest[F[_]](uri: Uri, token: String): Request[F] = {
    val header = Header.Raw(CIString("token"), token)
    Request[F](GET, uri).withHeaders(header)
  }

}