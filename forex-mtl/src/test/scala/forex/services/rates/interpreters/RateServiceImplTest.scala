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
  val testAllRates = List(testRate,testRate2)
  val testRedisError: Error = SystemError("Redis failure")
  val testOneFrameError: Error = OneFrameLookupFailed("OneFrame API failure")

  "RateServiceImpl.get" should {
    "return rate from cache" when {
      "cache exists" in {
        val mockCache = mock[RedisAlgebra[Id]]
        val mockApi = mock[ApiAlgebra[Id]]

        when(mockCache.get(testPair)).thenReturn(testRate.asRight)

        val service = new RateServiceImpl[Id](mockCache,mockApi)

        // Act
        val result = service.get(testPair)

        // Assert
        result shouldBe testRate.asRight
        verify(mockCache, times(1)).get(testPair)
        verify(mockApi, never()).getAll()
      }
    }
    "return rate from OneFrameAPI" when {
      "cache does not exist" in {
        val mockCache = mock[RedisAlgebra[Id]]
        val mockApi = mock[ApiAlgebra[Id]]

        when(mockCache.get(testPair)).thenReturn(testRedisError.asLeft)
        when(mockApi.getAll()).thenReturn(testAllRates.asRight)

        val service = new RateServiceImpl[Id](mockCache,mockApi)

        // Act
        val result = service.get(testPair)

        // Assert
        result shouldBe testRate.asRight
        verify(mockCache, times(1)).get(testPair)
        verify(mockApi, times(1)).getAll()
        verify(mockCache, times(1)).store(testAllRates)
      }
    }
  "return error message" when {
    "cannot connect to OneFrameAPI" in {
      val mockCache = mock[RedisAlgebra[Id]]
      val mockApi = mock[ApiAlgebra[Id]]

      when(mockCache.get(testPair)).thenReturn(testRedisError.asLeft)
      when(mockApi.getAll()).thenReturn(testOneFrameError.asLeft)

      val service = new RateServiceImpl[Id](mockCache,mockApi)

      // Act
      val result = service.get(testPair)

      // Assert
      result shouldBe testOneFrameError.asLeft
      verify(mockCache, times(1)).get(testPair)
      verify(mockApi, times(1)).getAll()
      verify(mockCache, never()).store(testAllRates)
    }
  }
  }
}
