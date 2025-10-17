package forex.services.rates.interpreters

import forex.services.rates.ApiAlgebra
import cats.effect.Sync
import forex.config.OneFrameConfig
import forex.domain.{Currency, Rate}
import forex.services.rates.errors._
import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder
import org.http4s.{ParseFailure, Uri}
import org.http4s.client.Client
import org.http4s.Query

case class OneFrameRateJson(
   from: String,
   to: String,
   bid: BigDecimal,
   ask: BigDecimal,
   price: BigDecimal,
   time_stamp: String
  )

object OneFrameRateJson {
  implicit val decoder: Decoder[OneFrameRateJson] = deriveDecoder
}

class OneFrameInterpreter[F[_]: Sync](client: Client[F], config: OneFrameConfig) extends ApiAlgebra[F] {
  val allPairs: List[String]= for {
    from <- List("USD")
    to <- Currency.all.map(_.toString) if from != to
  } yield s"$from$to"

  val url: F[Uri] = Sync[F].fromEither(OneFrameInterpreter.buildUri(config.url, allPairs))

  override def getAll(): F[Either[Error, List[Rate]]] = {
    println(client)
    println(config)
    Sync[F].pure(Right(List.empty))

  }
}


object OneFrameInterpreter {
  def buildUri(base: String, pairs: List[String]): Either[ParseFailure, Uri] = {
    Uri.fromString(base).map { uri =>
      val query = Query.fromPairs(pairs.map("pair" -> _): _*)
      uri.copy(query = query)
    }
  }
}