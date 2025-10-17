package forex.domain

import cats.Show

sealed trait Currency

object Currency {
  case object AUD extends Currency
  case object CAD extends Currency
  case object CHF extends Currency
  case object EUR extends Currency
  case object GBP extends Currency
  case object NZD extends Currency
  case object JPY extends Currency
  case object SGD extends Currency
  case object USD extends Currency

  val all: List[Currency] = List(AUD, CAD, CHF, EUR, GBP, NZD, JPY, SGD, USD)


  implicit val show: Show[Currency] = Show.show(_.toString)

  def fromString(s: String): Option[Currency] = all.find(_.toString.equalsIgnoreCase(s))

}
