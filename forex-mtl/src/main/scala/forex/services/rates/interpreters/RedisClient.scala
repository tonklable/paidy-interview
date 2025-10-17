package forex.services.rates.interpreters

import cats.effect.{Resource, Sync}

trait RedisClient[F[_]] {
  def get(key: String): F[Option[String]]
  def setEx(key:String, value:String, seconds:Int): F[Unit]
}
object RedisClient{
  def make[F[_]: Sync](host: String, port: Int): Resource[F, RedisClient[F]] = ???
}
