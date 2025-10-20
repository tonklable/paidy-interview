package forex.http.auth

import cats.data.OptionT
import cats.effect.Sync
import cats.implicits.{ catsSyntaxApplicativeId, catsSyntaxFlatMapOps }
import forex.http.rates.Converters.GetApiErrorResponseOps
import org.http4s.Status.Unauthorized
import org.http4s.{ HttpRoutes, Request, Response }
import org.typelevel.ci.CIString
import org.typelevel.log4cats.Logger

object TokenAuth {
  def apply[F[_]: Sync](expectedToken: String)(routes: HttpRoutes[F], logger: Logger[F]): HttpRoutes[F] =
    HttpRoutes { req: Request[F] =>
      val tokenHeader = req.headers.get(CIString("token")).map(_.head.value)

      tokenHeader match {
        case Some(token) if token == expectedToken =>
          routes(req)

        case _ =>
          OptionT.liftF(
            logger.error("Token not provided or token verification failed") >>
              Response[F](status = Unauthorized)
                .withEntity("Token not provided or token verification failed".asGetApiErrorResponse)
                .pure[F]
          )
      }
    }
}
