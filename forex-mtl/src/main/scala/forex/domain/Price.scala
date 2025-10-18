package forex.domain

import scala.math.BigDecimal.RoundingMode

case class Price(value: BigDecimal) extends AnyVal {
  def /(other: Price): Price = {
    val result = value / other.value
    val scale  = if (result >= 0.1) 4 else 4 + (result.scale - result.precision)
    Price(result.setScale(scale, RoundingMode.HALF_UP))
  }
}

object Price {
  def apply(value: Integer): Price =
    Price(BigDecimal(value))
}
