package forex.services.rates.interpreters

import cats.effect.{ Concurrent, ContextShift, Resource }
import dev.profunktor.redis4cats.Redis
import dev.profunktor.redis4cats.effect.Log.NoOp._
import scala.concurrent.duration.FiniteDuration

trait RedisClient[F[_]] {
  def get(key: String): F[Option[String]]
  def setEx(key: String, value: String, mins: FiniteDuration): F[Unit]
}
object RedisClient {
  def make[F[_]: Concurrent: ContextShift](uri: String): Resource[F, RedisClient[F]] =
    Redis[F].utf8(uri).map { redis =>
      new RedisClient[F] {
        override def get(key: String): F[Option[String]]                              = redis.get(key)
        override def setEx(key: String, value: String, mins: FiniteDuration): F[Unit] = redis.setEx(key, value, mins)
      }
    }
}
