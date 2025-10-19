package forex.http.auth

import cats.effect.IO
import io.circe.parser.parse
import org.http4s._
import org.http4s.implicits._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.http4s.dsl.io._
import org.typelevel.ci.CIString

class TokenAuthTest extends AnyWordSpec with Matchers {

  val expectedToken = "secret-token"

  val testRoutes: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root / "test" =>
      Ok("Success")
  }

  "TokenAuth" should {
    "allow request with valid token" in {
      val authedRoutes = TokenAuth[IO](expectedToken)(testRoutes)

      val request = Request[IO](
        method = Method.GET,
        uri = uri"/test"
      ).withHeaders(Header.Raw(CIString("token"), expectedToken))

      val response = authedRoutes.orNotFound.run(request).unsafeRunSync()

      response.status shouldBe Status.Ok
    }

    "reject request with invalid token" in {
      val authedRoutes = TokenAuth[IO](expectedToken)(testRoutes)

      val request = Request[IO](
        method = Method.GET,
        uri = uri"/test"
      ).withHeaders(Header.Raw(CIString("token"), "wrong-token"))

      val response = authedRoutes.orNotFound.run(request).unsafeRunSync()

      response.status shouldBe Status.Unauthorized
      val body = response.as[String].unsafeRunSync()
      val json = parse(body).getOrElse(fail("Invalid JSON"))

      (json \\ "error").head.asString shouldBe Some(
        "Token not provided or token verification failed"
      )
    }

    "reject request with missing token" in {
      val authedRoutes = TokenAuth[IO](expectedToken)(testRoutes)

      val request = Request[IO](
        method = Method.GET,
        uri = uri"/test"
      )

      val response = authedRoutes.orNotFound.run(request).unsafeRunSync()

      response.status shouldBe Status.Unauthorized
      val body = response.as[String].unsafeRunSync()
      val json = parse(body).getOrElse(fail("Invalid JSON"))

      (json \\ "error").head.asString shouldBe Some(
        "Token not provided or token verification failed"
      )
    }

    "reject request with empty token" in {
      val authedRoutes = TokenAuth[IO](expectedToken)(testRoutes)

      val request = Request[IO](
        method = Method.GET,
        uri = uri"/test"
      ).withHeaders(Header.Raw(CIString("token"), ""))

      val response = authedRoutes.orNotFound.run(request).unsafeRunSync()

      response.status shouldBe Status.Unauthorized
      val body = response.as[String].unsafeRunSync()
      val json = parse(body).getOrElse(fail("Invalid JSON"))

      (json \\ "error").head.asString shouldBe Some(
        "Token not provided or token verification failed"
      )
    }
  }
}