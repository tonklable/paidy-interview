package forex.domain

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class PriceTest extends AnyWordSpec with Matchers {
  "RateCalculation" should {
    "calculate normal rate correctly" when {
      "rounding down" in {
        val result = Price(150.94121707738152925)/Price(32.51121707738152925)
        result shouldEqual Price(4.6427)
        result.value.scale shouldEqual 4
      }
      "rounding up" in {
        val result = Price(32.51921807738152925)/Price(7.15921707738152925)
        result shouldEqual Price(4.5423)
        result.value.scale shouldEqual 4
      }
      "rounding up (.5)" in {
        val result = Price(32.51769016338152925)/Price(58.15498507738152925)
        result shouldEqual Price(0.5592)
        result.value.scale shouldEqual 4
      }
      "without rounding" in {
        val result = Price(580.00000000000000000)/Price(29.00000000000000000)
        result shouldEqual Price(20.0000)
        result.value.scale shouldEqual 4
      }
    }
    "calculate small rate correctly" when {
      "rounding down correctly" in {
        val result = Price(0.85783637188016175)/Price(150.94121707738152925)
        result shouldEqual Price(0.005683)
        result.value.precision shouldEqual 4
      }
      "rounding up correctly" in {
        val result = Price(0.85808012312313000)/Price(1509.4121707738152925)
        result shouldEqual Price(0.0005685)
        result.value.precision shouldEqual 4
      }
      "rounding up correctly (.5)" in {
        val result = Price(0.85785083572037458)/Price(15.94341707738152925)
        result shouldEqual Price(0.05381)
        result.value.precision shouldEqual 4
      }
    }
  }
}
