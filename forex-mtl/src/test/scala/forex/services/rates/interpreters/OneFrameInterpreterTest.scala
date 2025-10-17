package forex.services.rates.interpreters

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class OneFrameInterpreterTest extends AnyWordSpec with Matchers{
  "buildUrl" should {
    "construct a valid URI with currency pairs" in {
      val base = "http://localhost:8080/rates"
      val pairs = List("USDEUR", "USDJPY", "USDTHB")

      val result = OneFrameInterpreter.buildUri(base, pairs)

      val url = result.toOption.get
      url.renderString shouldBe "http://localhost:8080/rates?pair=USDEUR&pair=USDJPY&pair=USDTHB"
      url.path.renderString shouldBe "/rates"
      url.query.multiParams("pair") should contain allElementsOf pairs
    }
  }
}
