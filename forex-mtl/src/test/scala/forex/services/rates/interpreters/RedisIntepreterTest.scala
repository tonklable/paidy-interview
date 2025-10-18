package forex.services.rates.interpreters

import cats.Id
import forex.config.RedisConfig
import forex.domain.{Currency, Price, Rate, Timestamp}
import forex.services.rates.interpreters.RateUtils.toJson
import io.circe.syntax.EncoderOps
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.duration.FiniteDuration

class RedisIntepreterTest extends AnyWordSpec with Matchers with MockitoSugar {
  val config = RedisConfig("redis://localhost:1234", FiniteDuration.apply(5, "seconds"))
  val testPairRate = Rate.Pair(Currency.USD, Currency.EUR)
  val testKey = "rates"
  val testPairString = "USDJPY"
  val mockClient: RedisClient[Id] = mock[RedisClient[Id]]
  val interpreter = new RedisInterpreter[Id](mockClient, config)
  "RedisInterpreter.getAll" should {
    "return Left for non-existent keys" in {
      when(mockClient.get(testKey)).thenReturn(None)

      val result = interpreter.getAll
      result.isLeft shouldBe true
    }

    "return Right when cache hits with valid JSON" when {
      "pair contains USD" in {
        val mockClient = mock[RedisClient[Id]]

        val rateJson = """[
                         |  {
                         |    "from": "USD",
                         |    "to": "JPY",
                         |    "price": 0.71810472617368925,
                         |    "time_stamp": "2025-10-18T02:32:44.612Z"
                         |  },
                         |  {
                         |    "from": "USD",
                         |    "to": "EUR",
                         |    "price": 0.6305395913802694,
                         |    "time_stamp": "2025-10-18T02:32:44.612Z"
                         |  }
                         |]""".stripMargin
        when(mockClient.get(testKey)).thenReturn(Some(rateJson))

        val interpreter = new RedisInterpreter[Id](mockClient, config)
        val result = interpreter.getAll
        println(result)
        result.isRight shouldBe true

        val rates = result.toOption.get
        rates should have size 2

        rates.map(_.pair.from) should contain only Currency.USD
        rates.map(_.pair.to) should contain allOf(Currency.JPY, Currency.EUR)
        rates.map(_.price.value) should contain allOf(
          BigDecimal("0.71810472617368925"),
          BigDecimal("0.6305395913802694")
        )
      }
    }
  }
  "RedisInterpreter.store" should {
    "store correct value" in {

      val rates = List(
        Rate(
          pair = Rate.Pair(Currency.USD, Currency.EUR),
          price = Price(0.71810472617368925),
          timestamp = Timestamp.now
        ),
        Rate(
          pair = Rate.Pair(Currency.USD, Currency.JPY),
          price = Price(0.6305395913802694),
          timestamp = Timestamp.now
        )
      )

      interpreter.store(rates)
      val expectedJson = rates.map(toJson).asJson.noSpaces
      verify(mockClient,times(1)).setEx("rates", expectedJson, config.ttl)
    }
  }
}
