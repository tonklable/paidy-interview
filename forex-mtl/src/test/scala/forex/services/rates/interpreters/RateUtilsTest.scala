package forex.services.rates.interpreters
import forex.domain.{Currency, Price, Rate, Timestamp}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec


class RateUtilsTest extends AnyWordSpec with Matchers{
  "toRate" should {
    "transform RateJson to Rate" in {
      val json = RateJson(
        from = "USD",
        to = "EUR",
        price = BigDecimal(1.15),
        time_stamp = "2025-10-18T10:00:00Z"
      )

      val result = RateUtils.toRate(json)

      val rate = result.get
      rate.pair.from.toString shouldBe "USD"
      rate.pair.to.toString shouldBe "EUR"
      rate.price.value shouldBe BigDecimal(1.15)
    }
  }
  "toJson" should {
    "transform Rate to RateJson" in {
      val rate = Rate(
        pair = Rate.Pair(
          from = Currency.USD,
          to = Currency.EUR
        ),
        price = Price(1.15),
        timestamp = Timestamp.now
      )

      val result = RateUtils.toJson(rate)

      result.from shouldBe "USD"
      result.to shouldBe "EUR"
      result.price shouldBe BigDecimal(1.15)
      result.time_stamp should not be empty
    }
  }
}
