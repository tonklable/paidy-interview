package forex.http.rates

import cats.effect.IO
import forex.domain.{Currency, Price, Rate, Timestamp}
import forex.programs.RatesProgram
import forex.programs.rates.Protocol.GetRatesRequest
import forex.programs.rates.errors.Error
import org.http4s._
import org.http4s.implicits._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers._
import io.circe.parser._
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger


class RatesHttpRoutesTest extends AnyWordSpec with Matchers with MockitoSugar {

  val testRate = Rate(
    Rate.Pair(Currency.USD, Currency.EUR),
    Price(BigDecimal("0.85")),
    Timestamp.now
  )

  val logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  "RatesHttpRoutes" should {

    "return 200 OK with rate when request is valid" in {
      val mockProgram = mock[RatesProgram[IO]]

      when(mockProgram.get(any[GetRatesRequest]()))
        .thenReturn(IO.pure(Right(testRate)))

      val routes = new RatesHttpRoutes[IO](mockProgram, logger).routes

      val request = Request[IO](
        method = Method.GET,
        uri = uri"/rates?from=USD&to=EUR"
      )

      val response = routes.orNotFound.run(request).unsafeRunSync()

      response.status shouldBe Status.Ok
      verify(mockProgram, times(1)).get(GetRatesRequest(Currency.USD, Currency.EUR))
    }

    "return 500 Internal Server Error when SystemError occurs" in {
      val mockProgram = mock[RatesProgram[IO]]

      when(mockProgram.get(any[GetRatesRequest]()))
        .thenReturn(IO.pure(Left(Error.SystemError("Data parsing failed"))))

      val routes = new RatesHttpRoutes[IO](mockProgram, logger).routes

      val request = Request[IO](
        method = Method.GET,
        uri = uri"/rates?from=USD&to=EUR"
      )

      val response = routes.orNotFound.run(request).unsafeRunSync()

      response.status shouldBe Status.InternalServerError

      val body = response.as[String].unsafeRunSync()
      val json = parse(body).getOrElse(fail("Invalid JSON"))

      (json \\ "error").head.asString shouldBe Some(
        "Internal error. Please contact the developer."
      )
    }

    "return 503 Service Unavailable when RateLookupFailed occurs" in {
      val mockProgram = mock[RatesProgram[IO]]

      when(mockProgram.get(any[GetRatesRequest]()))
        .thenReturn(IO.pure(Left(Error.RateLookupFailed("OneFrame API is down"))))

      val routes = new RatesHttpRoutes[IO](mockProgram, logger).routes

      val request = Request[IO](
        method = Method.GET,
        uri = uri"/rates?from=USD&to=EUR"
      )

      val response = routes.orNotFound.run(request).unsafeRunSync()

      response.status shouldBe Status.ServiceUnavailable

      val body = response.as[String].unsafeRunSync()
      val json = parse(body).getOrElse(fail("Invalid JSON"))

      (json \\ "error").head.asString shouldBe Some(
        "Unable to reach external rate service. Please try again later."
      )
    }

    "return 400 Bad Request when 'from' parameter is missing" in {
      val mockProgram = mock[RatesProgram[IO]]

      val routes = new RatesHttpRoutes[IO](mockProgram, logger).routes

      val request = Request[IO](
        method = Method.GET,
        uri = uri"/rates?to=EUR"
      )

      val response = routes.orNotFound.run(request).unsafeRunSync()

      response.status shouldBe Status.BadRequest

      val body = response.as[String].unsafeRunSync()
      val json = parse(body).getOrElse(fail("Invalid JSON"))

      (json \\ "error").head.asString shouldBe Some(
        "Invalid request. Please provide both currencies in scope."
      )
    }

    "return 400 Bad Request when 'to' parameter is missing" in {
      val mockProgram = mock[RatesProgram[IO]]

      val routes = new RatesHttpRoutes[IO](mockProgram, logger).routes

      val request = Request[IO](
        method = Method.GET,
        uri = uri"/rates?from=USD"
      )

      val response = routes.orNotFound.run(request).unsafeRunSync()

      response.status shouldBe Status.BadRequest

      val body = response.as[String].unsafeRunSync()
      val json = parse(body).getOrElse(fail("Invalid JSON"))

      (json \\ "error").head.asString shouldBe Some(
        "Invalid request. Please provide both currencies in scope."
      )
    }

    "return 400 Bad Request when both parameters are missing" in {
      val mockProgram = mock[RatesProgram[IO]]

      val routes = new RatesHttpRoutes[IO](mockProgram, logger).routes

      val request = Request[IO](
        method = Method.GET,
        uri = uri"/rates"
      )

      val response = routes.orNotFound.run(request).unsafeRunSync()

      response.status shouldBe Status.BadRequest

      val body = response.as[String].unsafeRunSync()
      val json = parse(body).getOrElse(fail("Invalid JSON"))

      (json \\ "error").head.asString shouldBe Some(
        "Invalid request. Please provide both currencies in scope."
      )
    }

    "return 400 Bad Request when currency is invalid" in {
      val mockProgram = mock[RatesProgram[IO]]

      val routes = new RatesHttpRoutes[IO](mockProgram, logger).routes

      val request = Request[IO](
        method = Method.GET,
        uri = uri"/rates?from=XXX&to=EUR"
      )

      val response = routes.orNotFound.run(request).unsafeRunSync()

      response.status shouldBe Status.BadRequest

      val body = response.as[String].unsafeRunSync()
      val json = parse(body).getOrElse(fail("Invalid JSON"))

      (json \\ "error").head.asString shouldBe Some(
        "Invalid request. Please provide both currencies in scope."
      )
    }

    "return 404 Not Found for invalid path" in {
      val mockProgram = mock[RatesProgram[IO]]

      val routes = new RatesHttpRoutes[IO](mockProgram, logger).routes

      val request = Request[IO](
        method = Method.GET,
        uri = uri"/invalid"
      )

      val response = routes.orNotFound.run(request).unsafeRunSync()

      response.status shouldBe Status.NotFound
    }
  }
}