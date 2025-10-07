package forex.domain

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class RateCalculationTest extends AnyWordSpec with Matchers {
  "RateCalculation" should {
    "calculate normal rate correctly" when {
      "rounding down" in {
       Price(150.94121707738152925)/Price(32.51121707738152925) shouldEqual Price(4.6427)
      }
      "rounding up" in {
        Price(32.51921807738152925)/Price(7.15921707738152925) shouldEqual Price(4.5423)
      }
      "rounding up (.5)" in {
        Price(32.51769016338152925)/Price(58.15498507738152925) shouldEqual Price(0.5592)
      }
      "without rounding" in {
        Price(58.00000000000000000)/Price(29.00000000000000000) shouldEqual Price(2.0000)
      }
    }
    "calculate small rate correctly" when {
      "rounding down correctly" in {
        Price(0.85783637188016175)/Price(150.94121707738152925) shouldEqual Price(0.005683)
      }
      "rounding up correctly" in {
        Price(0.85808012312313000)/Price(1509.4121707738152925) shouldEqual Price(0.0005685)
      }
      "rounding up correctly (.5)" in {
        Price(0.85785083572037458)/Price(15.94341707738152925) shouldEqual Price(0.05381)
      }
    }
  }
}
