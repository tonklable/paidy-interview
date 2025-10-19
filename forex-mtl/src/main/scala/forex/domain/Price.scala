package forex.domain

import scala.math.BigDecimal.RoundingMode

case class Price(value: BigDecimal) extends AnyVal {
  def /(other: Price): Price = {
    val result = value / other.value
    Price(result).round
  }
  def round: Price = {
    val scale = if (value >= 0.1) 4 else 4 + (value.scale - value.precision)
    Price(value.setScale(scale, RoundingMode.HALF_UP))
  }
}

object Price {
  def apply(value: Integer): Price =
    Price(BigDecimal(value))
}
