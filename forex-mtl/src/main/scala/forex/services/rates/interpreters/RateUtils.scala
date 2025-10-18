package forex.services.rates.interpreters

import forex.domain.{Currency, Price, Rate, Timestamp}
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

case class RateJson(
                     from: String,
                     to: String,
                     price: BigDecimal,
                     time_stamp: String
                           )

object RateJson {
  implicit val decoder: Decoder[RateJson] = deriveDecoder
  implicit val encoder: Encoder[RateJson] = deriveEncoder
}


object RateUtils {
  def toRate(json: RateJson): Option[Rate] = {
    for {
      from <- Currency.fromString(json.from)
      to <- Currency.fromString(json.to)
    } yield Rate(
      pair = Rate.Pair(from, to),
      price = Price(json.price),
      timestamp = Timestamp.now
    )
  }
  def toJson(rate:Rate) = RateJson(
    from       = rate.pair.from.toString,
    to         = rate.pair.to.toString,
    price      = rate.price.value,
    time_stamp = rate.timestamp.value.toString
  )
}
