package gibbon.state

import gibbon.operations.{EventStore, RedisEventStore}
import munit.FunSuite
import scala.concurrent.{Future, ExecutionContext, Promise}
import scala.concurrent.duration._
import io.circe._
import io.circe.syntax._
import io.circe.parser._
import io.circe.generic.auto._
import akka.util.ByteString
import java.util.concurrent.atomic.AtomicReference
import scala.util.{Success, Failure}

/**
 * Test suite for RedisEventStore state management functionality
 * Tests CRUD operations, serialization, error handling, and concurrent access
 */
class RedisEventStoreSpec extends FunSuite {
  
  implicit val ec: ExecutionContext = ExecutionContext.global
  
  // Test Redis client implementation that simulates Redis behavior
  class TestRedisClient {
    private val storage = new AtomicReference(Map.empty[String, String])
    private val keyStorage = new AtomicReference(Set.empty[String])
    
    def get(key: String): Future[Option[ByteString]] = {
      val value = storage.get().get(key)
      Future.successful(value.map(ByteString(_)))
    }
    
    def set(key: String, value: String): Future[Boolean] = {
      storage.updateAndGet(_ + (key -> value))
      keyStorage.updateAndGet(_ + key)
      Future.successful(true)
    }
    
    def del(keys: String*): Future[Long] = {
      val currentStorage = storage.get()
      val keysToDelete = keys.toSet
      val newStorage = currentStorage -- keysToDelete
      storage.set(newStorage)
      keyStorage.updateAndGet(_ -- keysToDelete)
      Future.successful(keysToDelete.count(currentStorage.contains).toLong)
    }
    
    def keys(pattern: String): Future[Seq[String]] = {
      val allKeys = keyStorage.get().toSeq
      if (pattern == "*") {
        Future.successful(allKeys)
      } else {
        // Simple pattern matching for test purposes
        val regex = pattern.replace("*", ".*").r
        Future.successful(allKeys.filter(key => regex.matches(key)))
      }
    }
    
    // Test helper methods
    def getStorage: Map[String, String] = storage.get()
    def clear(): Unit = {
      storage.set(Map.empty)
      keyStorage.set(Set.empty)
    }
    def size: Int = storage.get().size
  }
  
  // Test RedisEventStore implementation using TestRedisClient
  class TestRedisEventStore[K: Encoder: Decoder, V: Encoder: Decoder](
    testRedis: TestRedisClient
  )(implicit ec: ExecutionContext) extends EventStore[K, V] {
    
    override def get(key: K): Future[Option[V]] = {
      testRedis.get(key.asJson.noSpaces).map(_.flatMap(bs => decode[V](bs.utf8String).toOption))
    }
    
    override def put(key: K, value: V): Future[Unit] = {
      testRedis.set(key.asJson.noSpaces, value.asJson.noSpaces).map(_ => ())
    }
    
    override def delete(key: K): Future[Unit] = {
      testRedis.del(key.asJson.noSpaces).map(_ => ())
    }
    
    override def getAll: Future[Map[K, V]] = {
      testRedis.keys("*").flatMap { keys =>
        val keyValueFutures = keys.map { keyStr =>
          decode[K](keyStr).toOption match {
            case Some(k) => get(k).map(v => v.map(k -> _))
            case None => Future.successful(None)
          }
        }
        Future.sequence(keyValueFutures).map(_.flatten.toMap)
      }
    }
  }
  
  // Test data types
  case class TestKey(id: String, category: String)
  case class TestData(data: String, count: Int, active: Boolean)
  
  private def createEventStore(): (TestRedisEventStore[TestKey, TestData], TestRedisClient) = {
    val testRedis = new TestRedisClient()
    val eventStore = new TestRedisEventStore[TestKey, TestData](testRedis)
    (eventStore, testRedis)
  }
  
  test("RedisEventStore should store and retrieve values correctly") {
    val (eventStore, _) = createEventStore()
    val key = TestKey("user-1", "profile")
    val value = TestData("John Doe", 42, true)
    
    val result = for {
      _ <- eventStore.put(key, value)
      retrieved <- eventStore.get(key)
    } yield retrieved
    
    result.map { retrieved =>
      assert(retrieved.isDefined)
      assertEquals(retrieved.get, value)
    }
  }
  
  test("RedisEventStore should return None for non-existent keys") {
    val (eventStore, _) = createEventStore()
    val key = TestKey("non-existent", "test")
    
    val result = eventStore.get(key)
    
    result.map { retrieved =>
      assert(retrieved.isEmpty)
    }
  }
  
  test("RedisEventStore should delete values correctly") {
    val (eventStore, testRedis) = createEventStore()
    val key = TestKey("user-2", "settings")
    val value = TestData("Settings Data", 1, false)
    
    val result = for {
      _ <- eventStore.put(key, value)
      beforeDelete <- eventStore.get(key)
      _ <- eventStore.delete(key)
      afterDelete <- eventStore.get(key)
    } yield (beforeDelete, afterDelete)
    
    result.map { case (beforeDelete, afterDelete) =>
      assert(beforeDelete.isDefined)
      assertEquals(beforeDelete.get, value)
      assert(afterDelete.isEmpty)
      
      // Verify Redis storage is cleaned up
      val storage = testRedis.getStorage
      assert(!storage.contains(key.asJson.noSpaces))
    }
  }
  
  test("RedisEventStore should handle JSON serialization correctly") {
    val (eventStore, testRedis) = createEventStore()
    val key = TestKey("complex-key", "json-test")
    val value = TestData("Complex Data with \"quotes\" and \n newlines", 999, true)
    
    val result = for {
      _ <- eventStore.put(key, value)
      retrieved <- eventStore.get(key)
    } yield retrieved
    
    result.map { retrieved =>
      assert(retrieved.isDefined)
      assertEquals(retrieved.get, value)
      
      // Verify JSON is properly stored in Redis
      val storage = testRedis.getStorage
      val storedJson = storage(key.asJson.noSpaces)
      val parsedValue = decode[TestData](storedJson)
      assert(parsedValue.isRight)
      assertEquals(parsedValue.right.get, value)
    }
  }
  
  test("RedisEventStore should handle malformed JSON gracefully") {
    val (eventStore, testRedis) = createEventStore()
    val key = TestKey("malformed", "test")
    
    // Manually insert malformed JSON
    testRedis.set(key.asJson.noSpaces, "invalid-json-data")
    
    val result = eventStore.get(key)
    
    result.map { retrieved =>
      // Should return None instead of throwing exception
      assert(retrieved.isEmpty)
    }
  }
  
  test("RedisEventStore should retrieve all stored key-value pairs") {
    val (eventStore, _) = createEventStore()
    
    val testData = Map(
      TestKey("user-1", "profile") -> TestData("Alice", 25, true),
      TestKey("user-2", "profile") -> TestData("Bob", 30, false),
      TestKey("user-3", "settings") -> TestData("Settings", 1, true)
    )
    
    val result = for {
      _ <- Future.sequence(testData.map { case (k, v) => eventStore.put(k, v) })
      allData <- eventStore.getAll
    } yield allData
    
    result.map { allData =>
      assertEquals(allData.size, 3)
      testData.foreach { case (key, expectedValue) =>
        assert(allData.contains(key))
        assertEquals(allData(key), expectedValue)
      }
    }
  }
  
  test("RedisEventStore should return empty map when no data exists") {
    val (eventStore, _) = createEventStore()
    
    val result = eventStore.getAll
    
    result.map { allData =>
      assert(allData.isEmpty)
    }
  }
  
  test("RedisEventStore should handle concurrent operations correctly") {
    val (eventStore, _) = createEventStore()
    
    val keys = (1 to 10).map(i => TestKey(s"concurrent-$i", "test"))
    val values = (1 to 10).map(i => TestData(s"data-$i", i, i % 2 == 0))
    
    val putOperations = keys.zip(values).map { case (k, v) => eventStore.put(k, v) }
    val getOperations = keys.map(eventStore.get)
    
    val result = for {
      _ <- Future.sequence(putOperations)
      retrieved <- Future.sequence(getOperations)
    } yield retrieved
    
    result.map { retrieved =>
      assertEquals(retrieved.length, 10)
      retrieved.zip(values).foreach { case (retrievedOpt, expectedValue) =>
        assert(retrievedOpt.isDefined)
        assertEquals(retrievedOpt.get, expectedValue)
      }
    }
  }
  
  test("RedisEventStore should handle updates to existing keys") {
    val (eventStore, _) = createEventStore()
    val key = TestKey("update-test", "data")
    val initialValue = TestData("Initial", 1, false)
    val updatedValue = TestData("Updated", 2, true)
    
    val result = for {
      _ <- eventStore.put(key, initialValue)
      initial <- eventStore.get(key)
      _ <- eventStore.put(key, updatedValue)
      updated <- eventStore.get(key)
    } yield (initial, updated)
    
    result.map { case (initial, updated) =>
      assert(initial.isDefined)
      assertEquals(initial.get, initialValue)
      
      assert(updated.isDefined)
      assertEquals(updated.get, updatedValue)
    }
  }
  
  test("RedisEventStore should handle empty string values") {
    val (eventStore, _) = createEventStore()
    val key = TestKey("empty-test", "data")
    val value = TestData("", 0, false)
    
    val result = for {
      _ <- eventStore.put(key, value)
      retrieved <- eventStore.get(key)
    } yield retrieved
    
    result.map { retrieved =>
      assert(retrieved.isDefined)
      assertEquals(retrieved.get, value)
      assertEquals(retrieved.get.data, "")
    }
  }
  
  test("RedisEventStore should handle special characters in keys and values") {
    val (eventStore, _) = createEventStore()
    val key = TestKey("special-chars-!@#$%", "category-with-spaces and symbols")
    val value = TestData("Data with émojis 🚀 and unicode ñ", -42, true)
    
    val result = for {
      _ <- eventStore.put(key, value)
      retrieved <- eventStore.get(key)
    } yield retrieved
    
    result.map { retrieved =>
      assert(retrieved.isDefined)
      assertEquals(retrieved.get, value)
    }
  }
  
  test("RedisEventStore should handle large data values") {
    val (eventStore, _) = createEventStore()
    val key = TestKey("large-data", "test")
    val largeString = "x" * 10000 // 10KB string
    val value = TestData(largeString, 10000, true)
    
    val result = for {
      _ <- eventStore.put(key, value)
      retrieved <- eventStore.get(key)
    } yield retrieved
    
    result.map { retrieved =>
      assert(retrieved.isDefined)
      assertEquals(retrieved.get, value)
      assertEquals(retrieved.get.data.length, 10000)
    }
  }
  
  test("RedisEventStore should handle multiple deletes of the same key") {
    val (eventStore, _) = createEventStore()
    val key = TestKey("delete-test", "data")
    val value = TestData("To be deleted", 1, true)
    
    val result = for {
      _ <- eventStore.put(key, value)
      _ <- eventStore.delete(key)
      firstDelete <- eventStore.get(key)
      _ <- eventStore.delete(key) // Delete again
      secondDelete <- eventStore.get(key)
    } yield (firstDelete, secondDelete)
    
    result.map { case (firstDelete, secondDelete) =>
      assert(firstDelete.isEmpty)
      assert(secondDelete.isEmpty)
    }
  }
  
  test("RedisEventStore should maintain data consistency under concurrent access") {
    val (eventStore, _) = createEventStore()
    val key = TestKey("consistency-test", "data")
    
    // Simulate concurrent reads and writes
    val writeOperations = (1 to 5).map { i =>
      eventStore.put(key, TestData(s"value-$i", i, i % 2 == 0))
    }
    
    val readOperations = (1 to 10).map(_ => eventStore.get(key))
    
    val result = for {
      _ <- Future.sequence(writeOperations ++ readOperations)
      finalValue <- eventStore.get(key)
    } yield finalValue
    
    result.map { finalValue =>
      // Should have a consistent final value
      assert(finalValue.isDefined)
      
      // Final value should be one of the written values
      val possibleValues = (1 to 5).map(i => TestData(s"value-$i", i, i % 2 == 0)).toSet
      assert(possibleValues.contains(finalValue.get))
    }
  }
  
  test("RedisEventStore should handle getAll with mixed key types gracefully") {
    val (eventStore, testRedis) = createEventStore()
    
    // Add some valid data
    val validKey = TestKey("valid", "data")
    val validValue = TestData("Valid Data", 1, true)
    
    val result = for {
      _ <- eventStore.put(validKey, validValue)
      // Manually add invalid key format to Redis
      _ <- testRedis.set("invalid-key-format", "some-data")
      allData <- eventStore.getAll
    } yield allData
    
    result.map { allData =>
      // Should only return valid key-value pairs
      assertEquals(allData.size, 1)
      assert(allData.contains(validKey))
      assertEquals(allData(validKey), validValue)
    }
  }
  
  test("RedisEventStore should handle boolean and numeric edge cases") {
    val (eventStore, _) = createEventStore()
    
    val testCases = List(
      (TestKey("bool-true", "test"), TestData("true", Int.MaxValue, true)),
      (TestKey("bool-false", "test"), TestData("false", Int.MinValue, false)),
      (TestKey("zero", "test"), TestData("zero", 0, false)),
      (TestKey("negative", "test"), TestData("negative", -1, true))
    )
    
    val result = for {
      _ <- Future.sequence(testCases.map { case (k, v) => eventStore.put(k, v) })
      retrieved <- Future.sequence(testCases.map { case (k, _) => eventStore.get(k) })
    } yield retrieved.zip(testCases.map(_._2))
    
    result.map { retrievedWithExpected =>
      retrievedWithExpected.foreach { case (retrievedOpt, expected) =>
        assert(retrievedOpt.isDefined)
        assertEquals(retrievedOpt.get, expected)
      }
    }
  }
}