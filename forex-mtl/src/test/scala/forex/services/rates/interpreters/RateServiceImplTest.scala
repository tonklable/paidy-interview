package forex.services.rates.interpreters

import cats.effect.IO
import cats.effect.concurrent.Semaphore
import cats.implicits.catsSyntaxEitherId
import forex.domain._
import forex.services.rates.RedisAlgebra
import forex.services.rates.ApiAlgebra
import forex.services.rates.errors.Error._
import org.mockito.Mockito.{never, times, verify, when}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import forex.services.rates.errors._
import org.mockito.ArgumentMatchers.any


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
  val testOneFrameBusy: Error = OneFrameBusy("Too many concurrent requests")
  val mockGuard: Semaphore[IO] = new Semaphore[IO] {
    override def withPermit[A](t: IO[A]): IO[A] = t
    override def tryAcquire: IO[Boolean] = IO.pure(true)
    override def release: IO[Unit] = IO.unit
    override def acquire: IO[Unit] = IO.unit
    override def available: IO[Long] = IO.pure(1L)
    override def count: IO[Long] = IO.pure(1L)
    override def acquireN(n: Long): IO[Unit] = IO.unit
    override def tryAcquireN(n: Long): IO[Boolean] = IO.pure(true)
    override def releaseN(n: Long): IO[Unit] = IO.unit
  }

  "RateServiceImpl.get" should {
    "return rate from cache" when {
      "cache exists" in {
        val mockCache = mock[RedisAlgebra[IO]]
        val mockApi = mock[ApiAlgebra[IO]]

        when(mockCache.getAll).thenReturn(IO.pure(testAllRates.asRight))

        val service = new RateServiceImpl[IO](mockCache,mockApi, mockGuard)

        val resultIO = service.get(testPair)
        val result = resultIO.unsafeRunSync()

        val expectedResult = testRate.asRight.toOption.get
        val actualResult = result.toOption.get

        actualResult.pair shouldBe expectedResult.pair
        actualResult.price shouldBe expectedResult.price
        actualResult.timestamp shouldBe a [Timestamp]
        verify(mockCache, times(1)).getAll
        verify(mockApi, never()).getAll
      }
    }
    "return rate from OneFrameAPI" when {
      "cache does not exist when pair contains USD" in {
        val mockCache = mock[RedisAlgebra[IO]]
        val mockApi = mock[ApiAlgebra[IO]]

        when(mockCache.getAll).thenReturn(IO.pure(testRedisError.asLeft))
        when(mockApi.getAll).thenReturn(IO.pure(testAllRates.asRight))
        when(mockCache.store(any[List[Rate]]())).thenReturn(IO.unit)

        val service = new RateServiceImpl[IO](mockCache,mockApi, mockGuard)

        val resultIO = service.get(testPair)
        val result = resultIO.unsafeRunSync()

        val expectedResult = testRate.asRight.toOption.get
        val actualResult = result.toOption.get

        actualResult.pair shouldBe expectedResult.pair
        actualResult.price shouldBe expectedResult.price
        actualResult.timestamp shouldBe a [Timestamp]
        verify(mockCache, times(1)).getAll
        verify(mockApi, times(1)).getAll
        verify(mockCache, times(1)).store(testAllRates)
      }
    }
    "return rate from OneFrameAPI" when {
      "cache does not exist when pair does not contain USD" in {
        val mockCache = mock[RedisAlgebra[IO]]
        val mockApi = mock[ApiAlgebra[IO]]

        when(mockCache.getAll).thenReturn(IO.pure(testRedisError.asLeft))
        when(mockApi.getAll).thenReturn(IO.pure(testAllRates.asRight))
        when(mockCache.store(any[List[Rate]]())).thenReturn(IO.unit)

        val service = new RateServiceImpl[IO](mockCache,mockApi, mockGuard)

        val resultIO = service.get(testPair3)
        val result = resultIO.unsafeRunSync()

        val expectedResult = testRate3.asRight.toOption.get
        val actualResult = result.toOption.get

        actualResult.pair shouldBe expectedResult.pair
        actualResult.price shouldBe expectedResult.price
        actualResult.timestamp shouldBe a [Timestamp]
        verify(mockCache, times(1)).getAll
        verify(mockApi, times(1)).getAll
        verify(mockCache, times(1)).store(testAllRates)
      }
    }
    "return error message" when {
      "cannot connect to OneFrameAPI" in {
        val mockCache = mock[RedisAlgebra[IO]]
        val mockApi = mock[ApiAlgebra[IO]]

        when(mockCache.getAll).thenReturn(IO.pure(testRedisError.asLeft))
        when(mockApi.getAll).thenReturn(IO.pure(testOneFrameError.asLeft))

        val service = new RateServiceImpl[IO](mockCache,mockApi, mockGuard)

        val resultIO = service.get(testPair)
        val result = resultIO.unsafeRunSync()

        result shouldBe testOneFrameError.asLeft
        verify(mockCache, times(1)).getAll
        verify(mockApi, times(1)).getAll
        verify(mockCache, never()).store(testAllRates)
      }
      "too many concurrent requests" in {
        val mockCache = mock[RedisAlgebra[IO]]
        val mockApi = mock[ApiAlgebra[IO]]
        val mockGuardReject: Semaphore[IO] = new Semaphore[IO] {
          override def withPermit[A](t: IO[A]): IO[A] = t
          override def tryAcquire: IO[Boolean] = IO.pure(false)
          override def release: IO[Unit] = IO.unit
          override def acquire: IO[Unit] = IO.unit
          override def available: IO[Long] = IO.pure(1L)
          override def count: IO[Long] = IO.pure(1L)
          override def acquireN(n: Long): IO[Unit] = IO.unit
          override def tryAcquireN(n: Long): IO[Boolean] = IO.pure(false)
          override def releaseN(n: Long): IO[Unit] = IO.unit
        }

        when(mockCache.getAll).thenReturn(IO.pure(testRedisError.asLeft))

        val service = new RateServiceImpl[IO](mockCache,mockApi, mockGuardReject)

        val resultIO = service.get(testPair)
        val result = resultIO.unsafeRunSync()

        result shouldBe testOneFrameBusy.asLeft
        verify(mockCache, times(1)).getAll
        verify(mockApi, never()).getAll
        verify(mockCache, never()).store(testAllRates)
      }
    }
  }
  "findOrDivideRate" should {
    val now = Timestamp.now

    val usdEur = Rate(Rate.Pair(Currency.USD, Currency.EUR), Price(BigDecimal(0.9)), now)
    val usdJpy = Rate(Rate.Pair(Currency.USD, Currency.JPY), Price(BigDecimal(150.0)), now)
    val allRates = List(usdEur, usdJpy)

    "return None if detecting 0 rates" in {
      val usdEur = Rate(Rate.Pair(Currency.USD, Currency.EUR), Price(BigDecimal(0)), now)
      val usdJpy = Rate(Rate.Pair(Currency.USD, Currency.JPY), Price(BigDecimal(150.0)), now)
      val allRates = List(usdEur, usdJpy)

      val result = RateServiceImpl.findOrDivideRate(allRates, Rate.Pair(Currency.USD, Currency.EUR), now)

      result.isLeft shouldBe true
      result match {
        case Left(err) =>
          err shouldBe SystemError(s"Cannot compute rate for ${Rate.Pair(Currency.USD, Currency.EUR)}: zero price in rates")
        case Right(_) =>
          fail("Expected Left but got Right")
      }
    }

    "return correct rate for USDEUR" in {
      val pair = Rate.Pair(Currency.USD, Currency.EUR)
      val result = RateServiceImpl.findOrDivideRate(allRates, Rate.Pair(Currency.USD, Currency.EUR), now)

      result.isRight shouldBe true

      val rate = result.toOption.get
      val expectedPrice = usdEur.price
      rate.price shouldBe expectedPrice
      rate.pair shouldBe pair
    }
    "return correct rate for JPYEUR" in {
      val pair = Rate.Pair(Currency.JPY, Currency.EUR)
      val result = RateServiceImpl.findOrDivideRate(allRates, pair, now)

      result.isRight shouldBe true

      val rate = result.toOption.get
      val expectedPrice = usdEur.price / usdJpy.price
      rate.price shouldBe expectedPrice
      rate.pair shouldBe pair
    }
    "return correct rate for EURUSD" in {
      val pair = Rate.Pair(Currency.EUR, Currency.USD)
      val result = RateServiceImpl.findOrDivideRate(allRates, pair, now)

      result.isRight shouldBe true

      val rate = result.toOption.get
      val expectedPrice = Price(BigDecimal(1)) / usdEur.price
      rate.price shouldBe expectedPrice
      rate.pair shouldBe pair
    }
    "return correct rate for JPYJPY (same currency)" in {
      val pair = Rate.Pair(Currency.JPY, Currency.JPY)
      val result = RateServiceImpl.findOrDivideRate(allRates, pair, now)

      result.isRight shouldBe true

      val rate = result.toOption.get
      val expectedPrice = Price(1.0000)
      rate.price shouldBe expectedPrice
      rate.pair shouldBe pair
    }
  }
}
