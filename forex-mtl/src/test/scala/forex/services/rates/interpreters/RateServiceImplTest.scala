package forex.services.rates.interpreters

import cats.Id
import cats.implicits.catsSyntaxEitherId
import forex.domain.{Currency, Price, Rate, Timestamp}
import forex.services.rates.RedisAlgebra
import forex.services.rates.ApiAlgebra
import forex.services.rates.errors.Error.{OneFrameLookupFailed, SystemError}
import org.mockito.Mockito.{never, times, verify, when}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import forex.services.rates.errors._


class RateServiceImplTest extends AnyWordSpec with Matchers with MockitoSugar {
  val testPair = Rate.Pair(Currency.USD, Currency.EUR)
  val testPair2 = Rate.Pair(Currency.USD, Currency.JPY)
  val testPair3 = Rate.Pair(Currency.JPY, Currency.EUR)
  val testRate = Rate(
    testPair,
    Price(BigDecimal("0.85")),
    Timestamp.now
  )
  val testRate2 = Rate(
    testPair2,
    Price(BigDecimal("0.10")),
    Timestamp.now
  )
  val testRate3 = Rate(
    testPair3,
    Price(BigDecimal("0.85"))/Price(BigDecimal("0.10")),
    Timestamp.now
  )
  val testAllRates = List(testRate,testRate2)
  val testRedisError: Error = SystemError("Redis failure")
  val testOneFrameError: Error = OneFrameLookupFailed("OneFrame API failure")

  "RateServiceImpl.get" should {
    "return rate from cache" when {
      "cache exists" in {
        val mockCache = mock[RedisAlgebra[Id]]
        val mockApi = mock[ApiAlgebra[Id]]

        when(mockCache.getAll).thenReturn(testAllRates.asRight)

        val service = new RateServiceImpl[Id](mockCache,mockApi)

        // Act
        val result = service.get(testPair)

        // Assert
        result shouldBe testRate.asRight
        verify(mockCache, times(1)).getAll
        verify(mockApi, never()).getAll
      }
    }
    "return rate from OneFrameAPI" when {
      "cache does not exist when pair contains USD" in {
        val mockCache = mock[RedisAlgebra[Id]]
        val mockApi = mock[ApiAlgebra[Id]]

        when(mockCache.getAll).thenReturn(testRedisError.asLeft)
        when(mockApi.getAll).thenReturn(testAllRates.asRight)

        val service = new RateServiceImpl[Id](mockCache,mockApi)

        // Act
        val result = service.get(testPair)

        // Assert
        result shouldBe testRate.asRight
        verify(mockCache, times(1)).getAll
        verify(mockApi, times(1)).getAll
        verify(mockCache, times(1)).store(testAllRates)
      }
    }
    "return rate from OneFrameAPI" when {
      "cache does not exist when pair does not contain USD" in {
        val mockCache = mock[RedisAlgebra[Id]]
        val mockApi = mock[ApiAlgebra[Id]]

        when(mockCache.getAll).thenReturn(testRedisError.asLeft)
        when(mockApi.getAll).thenReturn(testAllRates.asRight)

        val service = new RateServiceImpl[Id](mockCache,mockApi)

        // Act
        val result = service.get(testPair3)

        // Assert
        result shouldBe testRate3.asRight
        verify(mockCache, times(1)).getAll
        verify(mockApi, times(1)).getAll
        verify(mockCache, times(1)).store(testAllRates)
      }
    }
    "return error message" when {
      "cannot connect to OneFrameAPI" in {
        val mockCache = mock[RedisAlgebra[Id]]
        val mockApi = mock[ApiAlgebra[Id]]

        when(mockCache.getAll).thenReturn(testRedisError.asLeft)
        when(mockApi.getAll).thenReturn(testOneFrameError.asLeft)

        val service = new RateServiceImpl[Id](mockCache,mockApi)

        // Act
        val result = service.get(testPair)

        // Assert
        result shouldBe testOneFrameError.asLeft
        verify(mockCache, times(1)).getAll
        verify(mockApi, times(1)).getAll
        verify(mockCache, never()).store(testAllRates)
      }
    }
  }
  "findOrDivideRate" should {
    val now = Timestamp.now

    val usdEur = Rate(Rate.Pair(Currency.USD, Currency.EUR), Price(BigDecimal(0.9)), now)
    val usdJpy = Rate(Rate.Pair(Currency.USD, Currency.JPY), Price(BigDecimal(150.0)), now)
    val allRates = List(usdEur, usdJpy)

    "return correct rate for USDEUR" in {
      val result = RateServiceImpl.findOrDivideRate(allRates, Rate.Pair(Currency.USD, Currency.EUR))
      result shouldBe Some(usdEur)
    }
    "return correct rate for JPYEUR" in {
      val pair = Rate.Pair(Currency.JPY, Currency.EUR)
      val result = RateServiceImpl.findOrDivideRate(allRates, pair)
      result should not be empty

      val expectedPrice = usdEur.price / usdJpy.price
      result.get.price shouldBe expectedPrice
      result.get.pair shouldBe pair
    }
    "return correct rate for EURUSD" in {
      val pair = Rate.Pair(Currency.EUR, Currency.USD)
      val result = RateServiceImpl.findOrDivideRate(allRates, pair)
      result should not be empty

      val expectedPrice = Price(BigDecimal(1)) / usdEur.price
      result.get.price shouldBe expectedPrice
      result.get.pair shouldBe pair
    }
    "return correct rate for JPYJPY (same currency)" in {
      val pair = Rate.Pair(Currency.JPY, Currency.JPY)
      val result = RateServiceImpl.findOrDivideRate(allRates, pair)
      result should not be empty

      val expectedPrice = Price(1.0000)
      result.get.price shouldBe expectedPrice
      result.get.pair shouldBe pair
    }
  }
}
