package forex.domain

object RateCalculation {
  def calculate(usdToFrom: Price, usdToTo: Price): Price = {
    usdToTo/usdToFrom
  }

}
