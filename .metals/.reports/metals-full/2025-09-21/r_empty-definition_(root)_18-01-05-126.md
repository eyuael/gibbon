error id: file://<WORKSPACE>/src/main/scala/gibbon/operations/RedisEventStore.scala:
file://<WORKSPACE>/src/main/scala/gibbon/operations/RedisEventStore.scala
empty definition using pc, found symbol in pc: 
empty definition using semanticdb
empty definition using fallback
non-local guesses:
	 -io/circe/syntax/redis.
	 -io/circe/parser/redis.
	 -redis.
	 -scala/Predef.redis.
offset: 35
uri: file://<WORKSPACE>/src/main/scala/gibbon/operations/RedisEventStore.scala
text:
```scala
package gibbon.operations

import r@@edis.RedisClient
import scala.concurrent.{Future, ExecutionContext}
import io.circe.{Encoder, Decoder}
import io.circe.syntax._
import io.circe.parser._

class RedisEventStore[K: Encoder: Decoder, V: Encoder: Decoder](
  redis: RedisClient
)(implicit ec: ExecutionContext) extends EventStore[K, V] {
  
  override def get(key: K): Future[Option[V]] = {
    redis.get(key.asJson.noSpaces).map(_.flatMap(decode[V](_).toOption))
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
      Future.traverse(keys) { keyStr =>
        decode[K](keyStr).toOption match {
          case Some(k) => get(k).map(v => k -> v)
          case None => Future.successful(None)
        }
      }.map(_.flatten.collect { case (k, Some(v)) => k -> v }.toMap)
    }
  }
}
```


#### Short summary: 

empty definition using pc, found symbol in pc: 