package forex

import scala.concurrent.ExecutionContext
import cats.effect._
import forex.config._
import forex.services.rates.interpreters.RedisClient
import fs2.Stream
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.ember.client.EmberClientBuilder

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] =
    new Application[IO].stream(executionContext).compile.drain.as(ExitCode.Success)

}

class Application[F[_]: ConcurrentEffect: Timer: ContextShift] {

  def stream(ec: ExecutionContext): Stream[F, Unit] =
    for {
      config <- Config.stream("app")
      httpClient <- Stream.resource(EmberClientBuilder.default[F].build)
      redisClient <-  Stream.resource(RedisClient.make[F](config.redis.url))
      module = new Module[F](config,httpClient,redisClient)
      _ <- BlazeServerBuilder[F](ec)
            .bindHttp(config.http.port, config.http.host)
            .withHttpApp(module.httpApp)
            .serve
    } yield ()

}
