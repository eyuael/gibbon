package gibbon.operations

import redis.RedisClient
import scala.concurrent.{Future, ExecutionContext}
import io.circe.{Encoder, Decoder}
import io.circe.syntax._
import io.circe.parser._
import akka.util.ByteString

class RedisEventStore[K: Encoder: Decoder, V: Encoder: Decoder](
  redis: RedisClient 
)(implicit ec: ExecutionContext) extends EventStore[K, V] {
  
  override def get(key: K): Future[Option[V]] = {
    redis.get(key.asJson.noSpaces).map(_.flatMap(bs => decode[V](bs.utf8String).toOption))
  }
  
  override def put(key: K, value: V): Future[Unit] = {
    redis.set(key.asJson.noSpaces, value.asJson.noSpaces).map(_ => ())
  }
  
  override def delete(key: K): Future[Unit] = {
    redis.del(key.asJson.noSpaces).map(_ => ())
  }
  
  override def getAll: Future[Map[K, V]] = {
    // Implementation for scanning all keys (use with caution in production)
    redis.keys("*").flatMap { keys =>
      Future.traverse(keys) { keyBytes =>
        val keyStr = keyBytes.utf8String
        decode[K](keyStr).toOption match {
          case Some(k) => get(k).map(v => k -> v)
          case None => Future.successful(k -> None)
        }
      }.map(_.collect { case (k, Some(v)) => k -> v }.toMap)
    }
  }
}