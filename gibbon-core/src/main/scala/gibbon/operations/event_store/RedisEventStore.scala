package gibbon.operations.event_store

import scala.concurrent.{Future, ExecutionContext}
import io.circe.{Encoder, Decoder}
import io.circe.syntax._
import io.circe.parser._

// Simplified EventStore implementation without external Redis dependency
class RedisEventStore[K: Encoder: Decoder, V: Encoder: Decoder](
  connectionString: String 
)(implicit ec: ExecutionContext) extends EventStore[K, V] {
  
  // Placeholder implementation - Redis client functionality would be implemented in runtime-specific modules
  private val store = scala.collection.mutable.Map[String, String]()
  
  override def get(key: K): Future[Option[V]] = Future {
    store.get(key.asJson.noSpaces).flatMap(jsonStr => decode[V](jsonStr).toOption)
  }
  
  override def put(key: K, value: V): Future[Unit] = Future {
    val keyStr = key.asJson.noSpaces
    println(s"DEBUG: put called with key: $keyStr, store object id: ${System.identityHashCode(store)}")
    store.put(keyStr, value.asJson.noSpaces)
    ()
  }
  
  override def delete(key: K): Future[Unit] = Future {
    store.remove(key.asJson.noSpaces)
    ()
  }
  
  override def getAll: Future[Map[K, V]] = Future {
    store.toMap.flatMap { case (keyStr, valueStr) =>
      for {
        k <- decode[K](keyStr).toOption
        v <- decode[V](valueStr).toOption
      } yield k -> v
    }
  }
  
  /**
   * Scan for keys matching a pattern
   * 
   * @param pattern Pattern to match keys against (regex pattern)
   * @return List of matching string keys
   */
  def scanKeys(pattern: String): Future[List[String]] = Future {
    // Debug: print all keys and the pattern
    println(s"DEBUG: scanKeys called with pattern: $pattern")
    println(s"DEBUG: store keys: ${store.keys.toList}")
    println(s"DEBUG: store object id: ${System.identityHashCode(store)}")
    // Don't replace * with .* if the pattern already contains .*
    val regexPattern = if (pattern.contains(".*")) pattern else pattern.replace("*", ".*")
    println(s"DEBUG: regex pattern: $regexPattern")
    val result = store.keys.filter { key =>
      val matches = key.matches(regexPattern)
      println(s"DEBUG: Testing key '$key' against pattern '$regexPattern': $matches")
      matches
    }.toList
    println(s"DEBUG: scanKeys result: $result")
    result
  }
  
  /**
   * Get all keys in the store
   * 
   * @return List of all keys
   */
  def getAllKeys: Future[List[K]] = Future {
    store.keys.flatMap { keyStr =>
      decode[K](keyStr).toOption
    }.toList
  }
  
  /**
   * Clear all data from the store
   * 
   * @return Future indicating completion
   */
  def clear(): Future[Unit] = Future {
    store.clear()
    ()
  }
}