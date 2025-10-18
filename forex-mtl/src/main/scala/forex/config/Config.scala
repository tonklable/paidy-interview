package forex.config

import cats.effect.Sync
import fs2.Stream

import pureconfig.ConfigSource
import pureconfig.generic.auto._
import io.github.cdimascio.dotenv.Dotenv

object Config {

  private def loadEnv(): Unit = {
    val dotenv = Dotenv.configure().ignoreIfMissing().load()
    dotenv.entries().forEach { entry =>
      val _ = System.setProperty(entry.getKey, entry.getValue)
    }
  }

  /**
    * @param path the property path inside the default configuration
    */
  def stream[F[_]: Sync](path: String): Stream[F, ApplicationConfig] =
    Stream.eval(Sync[F].delay(loadEnv())) >>
      Stream.eval(Sync[F].delay(ConfigSource.default.at(path).loadOrThrow[ApplicationConfig]))
}
