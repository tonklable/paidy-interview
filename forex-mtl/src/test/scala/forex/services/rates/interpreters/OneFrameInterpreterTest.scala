package forex.services.rates.interpreters

import cats.Id
import forex.domain.Currency.{EUR, JPY, THB, USD}
import org.http4s.Uri
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import org.typelevel.ci.CIString

class OneFrameInterpreterTest extends AnyWordSpec with Matchers with MockitoSugar{
  "buildUrl" should {
    val pairs = List("USDEUR", "USDJPY", "USDTHB")

    "construct a valid URI with currency pairs" in {
      val base = "http://localhost:8080/rates"

      val result = OneFrameInterpreter.buildUri(base, pairs)

      result.isRight shouldBe true
      val url = result.toOption.get
      url.renderString shouldBe "http://localhost:8080/rates?pair=USDEUR&pair=USDJPY&pair=USDTHB"
      url.path.renderString shouldBe "/rates"
      url.query.multiParams("pair") should contain allElementsOf pairs
    }
    "return error with invalid base url" in {
      val base = "http://local host:8080/rates"

      val result = OneFrameInterpreter.buildUri(base, pairs)

      result.isLeft shouldBe true
    }
  }
  "buildAllPairs" should {
    "construct a list of string of all pairs in the list (except USD) with USD" in {
      val currencyList = List(USD, EUR, JPY, THB)

      val result = OneFrameInterpreter.buildAllPairs(currencyList)

      result shouldBe List("USDEUR", "USDJPY", "USDTHB")
    }
  }
  "buildRequest" should {
    "construct a request with uri and token" in {
      val uriString: String = "http://localhost:8080/rates?pair=USDEUR&pair=USDJPY&pair=USDTHB"
      val uri: Uri = Uri.unsafeFromString(uriString)
      val token = "1234567889"

      val result = OneFrameInterpreter.buildRequest[Id](uri,token)

      result.uri.renderString shouldBe uriString
      result.headers.get(CIString("token")).map(_.head.value) shouldBe Some(token)
    }
  }
}
