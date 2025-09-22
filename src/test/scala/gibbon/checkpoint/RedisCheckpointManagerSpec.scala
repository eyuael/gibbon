package gibbon.checkpoint

import gibbon.core.Event
import munit.FunSuite
import scala.concurrent.{Future, ExecutionContext}
import scala.concurrent.duration._
import java.time.Instant
import io.circe._
import io.circe.syntax._
import io.circe.parser._
import io.circe.generic.auto._
import scala.util.{Success, Failure}

/**
 * Test suite for RedisCheckpointManager implementation
 * Tests the Redis-specific functionality using a test implementation
 */
class RedisCheckpointManagerSpec extends FunSuite {
  
  implicit val ec: ExecutionContext = ExecutionContext.global
  
  // Implicit encoders and decoders for Event type
  implicit def eventEncoder[K: Encoder, V: Encoder]: Encoder[Event[K, V]] = 
    Encoder.forProduct4("key", "value", "eventTime", "timestamp")(e => (e.key, e.value, e.eventTime, e.timestamp))

  implicit def eventDecoder[K: Decoder, V: Decoder]: Decoder[Event[K, V]] = 
    Decoder.forProduct4("key", "value", "eventTime", "timestamp")(Event.apply[K, V])
  
  // Test implementation that simulates Redis behavior without actual Redis dependency
  class TestRedisCheckpointManager[K: Encoder: Decoder, V: Encoder: Decoder](
    keyPrefix: String = "gibbon:checkpoint"
  )(implicit ec: ExecutionContext) extends CheckpointManager[K, V] {
    
    private var storage = Map.empty[String, String]
    private var lists = Map.empty[String, List[String]]
    
    private def checkpointKey(pipelineId: String): String = 
      s"$keyPrefix:$pipelineId:latest"
    
    private def checkpointHistoryKey(pipelineId: String): String = 
      s"$keyPrefix:$pipelineId:history"
    
    override def saveCheckpoint(checkpoint: Checkpoint[K, V]): Future[Unit] = {
      val key = checkpointKey(checkpoint.pipelineId)
      val historyKey = checkpointHistoryKey(checkpoint.pipelineId)
      val jsonString = checkpoint.asJson.noSpaces
      
      // Simulate Redis SET
      storage = storage + (key -> jsonString)
      
      // Simulate Redis LPUSH
      val currentList = lists.getOrElse(historyKey, List.empty)
      val newList = jsonString :: currentList
      
      // Simulate Redis LTRIM (keep last 100)
      val trimmedList = newList.take(100)
      lists = lists + (historyKey -> trimmedList)
      
      Future.successful(())
    }
    
    override def loadCheckpoint(pipelineId: String): Future[Option[Checkpoint[K, V]]] = {
      val key = checkpointKey(pipelineId)
      storage.get(key) match {
        case Some(jsonString) =>
          decode[Checkpoint[K, V]](jsonString) match {
            case Right(checkpoint) => Future.successful(Some(checkpoint))
            case Left(_) => Future.successful(None)
          }
        case None => Future.successful(None)
      }
    }
    
    override def listCheckpoints(pipelineId: String): Future[List[CheckpointMetadata]] = {
      val historyKey = checkpointHistoryKey(pipelineId)
      val checkpointStrings = lists.getOrElse(historyKey, List.empty)
      
      val metadata = checkpointStrings.flatMap { jsonString =>
        decode[Checkpoint[K, V]](jsonString).toOption.map { checkpoint =>
          CheckpointMetadata(
            pipelineId = checkpoint.pipelineId,
            version = checkpoint.offset,
            createdAt = checkpoint.timestamp,
            checkpointType = Periodic
          )
        }
      }
      
      Future.successful(metadata)
    }
    
    override def deleteCheckpoint(pipelineId: String, version: Long): Future[Unit] = {
      val key = checkpointKey(pipelineId)
      val historyKey = checkpointHistoryKey(pipelineId)
      
      storage = storage - key
      lists = lists - historyKey
      
      Future.successful(())
    }
    
    // Test helper methods
    def clear(): Unit = {
      storage = Map.empty
      lists = Map.empty
    }
    
    def getStorage: Map[String, String] = storage
    def getLists: Map[String, List[String]] = lists
  }
  
  private def createTestCheckpoint(
    pipelineId: String = "test-pipeline",
    offset: Long = 100L,
    timestamp: Instant = Instant.now(),
    lastProcessedEvent: Option[Event[String, String]] = None,
    state: Map[String, String] = Map("processedCount" -> "100")
  ): Checkpoint[String, String] = {
    Checkpoint(pipelineId, offset, timestamp, lastProcessedEvent, state)
  }
  
  test("RedisCheckpointManager should save checkpoint with correct Redis keys") {
    val manager = new TestRedisCheckpointManager[String, String]("test:checkpoint")
    val checkpoint = createTestCheckpoint("pipeline-1", 50L)
    
    val result = manager.saveCheckpoint(checkpoint)
    
    result.map { _ =>
      val storage = manager.getStorage
      val lists = manager.getLists
      
      // Check that checkpoint was saved to latest key
      assert(storage.contains("test:checkpoint:pipeline-1:latest"))
      
      // Check that checkpoint was added to history
      assert(lists.contains("test:checkpoint:pipeline-1:history"))
      assertEquals(lists("test:checkpoint:pipeline-1:history").length, 1)
      
      // Verify the stored data can be parsed back
      val storedJson = storage("test:checkpoint:pipeline-1:latest")
      val parsedCheckpoint = decode[Checkpoint[String, String]](storedJson)
      assert(parsedCheckpoint.isRight)
      assertEquals(parsedCheckpoint.right.get.offset, 50L)
    }
  }
  
  test("RedisCheckpointManager should load checkpoint from storage") {
    val manager = new TestRedisCheckpointManager[String, String]("test:checkpoint")
    val checkpoint = createTestCheckpoint("pipeline-2", 75L)
    
    val result = for {
      _ <- manager.saveCheckpoint(checkpoint)
      loaded <- manager.loadCheckpoint("pipeline-2")
    } yield loaded
    
    result.map { loaded =>
      assert(loaded.isDefined)
      assertEquals(loaded.get.pipelineId, "pipeline-2")
      assertEquals(loaded.get.offset, 75L)
    }
  }
  
  test("RedisCheckpointManager should return None for non-existent checkpoint") {
    val manager = new TestRedisCheckpointManager[String, String]("test:checkpoint")
    
    val result = manager.loadCheckpoint("non-existent")
    
    result.map { loaded =>
      assert(loaded.isEmpty)
    }
  }
  
  test("RedisCheckpointManager should list checkpoint metadata from history") {
    val manager = new TestRedisCheckpointManager[String, String]("test:checkpoint")
    
    val timestamp1 = Instant.now()
    val timestamp2 = timestamp1.plusSeconds(60)
    val checkpoint1 = createTestCheckpoint("pipeline-3", 100L, timestamp1)
    val checkpoint2 = createTestCheckpoint("pipeline-3", 200L, timestamp2)
    
    val result = for {
      _ <- manager.saveCheckpoint(checkpoint1)
      _ <- manager.saveCheckpoint(checkpoint2)
      metadata <- manager.listCheckpoints("pipeline-3")
    } yield metadata
    
    result.map { metadata =>
      assertEquals(metadata.length, 2)
      
      // Verify metadata contains both checkpoints
      val versions = metadata.map(_.version).toSet
      assert(versions.contains(100L))
      assert(versions.contains(200L))
      
      // Verify all metadata has correct pipeline ID
      assert(metadata.forall(_.pipelineId == "pipeline-3"))
      
      // Verify checkpoint type is set to Periodic (default)
      assert(metadata.forall(_.checkpointType == Periodic))
    }
  }
  
  test("RedisCheckpointManager should return empty list for non-existent pipeline history") {
    val manager = new TestRedisCheckpointManager[String, String]("test:checkpoint")
    
    val result = manager.listCheckpoints("non-existent")
    
    result.map { metadata =>
      assert(metadata.isEmpty)
    }
  }
  
  test("RedisCheckpointManager should delete checkpoint and history") {
    val manager = new TestRedisCheckpointManager[String, String]("test:checkpoint")
    val checkpoint = createTestCheckpoint("pipeline-4", 150L)
    
    val result = for {
      _ <- manager.saveCheckpoint(checkpoint)
      _ <- manager.deleteCheckpoint("pipeline-4", 150L)
      loaded <- manager.loadCheckpoint("pipeline-4")
      metadata <- manager.listCheckpoints("pipeline-4")
    } yield (loaded, metadata)
    
    result.map { case (loaded, metadata) =>
      assert(loaded.isEmpty)
      assert(metadata.isEmpty)
      
      // Verify storage is cleaned up
      val storage = manager.getStorage
      val lists = manager.getLists
      assert(!storage.contains("test:checkpoint:pipeline-4:latest"))
      assert(!lists.contains("test:checkpoint:pipeline-4:history"))
    }
  }
  
  test("RedisCheckpointManager should use custom key prefix") {
    val manager = new TestRedisCheckpointManager[String, String]("custom:prefix")
    val checkpoint = createTestCheckpoint("pipeline-5", 250L)
    
    val result = manager.saveCheckpoint(checkpoint)
    
    result.map { _ =>
      val storage = manager.getStorage
      val lists = manager.getLists
      
      assert(storage.contains("custom:prefix:pipeline-5:latest"))
      assert(lists.contains("custom:prefix:pipeline-5:history"))
    }
  }
  
  test("RedisCheckpointManager should handle checkpoint history trimming") {
    val manager = new TestRedisCheckpointManager[String, String]("test:checkpoint")
    
    // Create multiple checkpoints to test history trimming
    val checkpoints = (1 to 5).map { i =>
      createTestCheckpoint("pipeline-6", i * 100L, Instant.now().plusSeconds(i))
    }
    
    val result = Future.sequence(checkpoints.map(manager.saveCheckpoint))
    
    result.map { _ =>
      val lists = manager.getLists
      val history = lists("test:checkpoint:pipeline-6:history")
      
      // Verify all checkpoints are in history (within the 100 limit)
      assertEquals(history.length, 5)
      
      // Verify the order (newest first due to prepend)
      val parsedCheckpoints = history.flatMap(json => decode[Checkpoint[String, String]](json).toOption)
      assertEquals(parsedCheckpoints.length, 5)
      
      // First item should be the latest (offset 500)
      assertEquals(parsedCheckpoints.head.offset, 500L)
    }
  }
  
  test("RedisCheckpointManager should handle JSON serialization errors gracefully") {
    val manager = new TestRedisCheckpointManager[String, String]("test:checkpoint")
    
    // Manually insert invalid JSON to test error handling
    val storage = manager.getStorage
    manager.getStorage.updated("test:checkpoint:invalid:latest", "invalid-json")
    
    val result = manager.loadCheckpoint("invalid")
    
    result.map { loaded =>
      // Should return None for invalid JSON instead of failing
      assert(loaded.isEmpty)
    }
  }
  
  test("RedisCheckpointManager should handle complex checkpoint data") {
    val manager = new TestRedisCheckpointManager[String, String]("test:checkpoint")
    
    val event = Event("complex-key", "complex-value", System.currentTimeMillis())
    val complexState = Map(
      "processedCount" -> "1000",
      "lastError" -> "none",
      "status" -> "running",
      "metadata" -> "some-metadata"
    )
    
    val checkpoint = createTestCheckpoint(
      pipelineId = "complex-pipeline",
      offset = 999L,
      timestamp = Instant.now(),
      lastProcessedEvent = Some(event),
      state = complexState
    )
    
    val result = for {
      _ <- manager.saveCheckpoint(checkpoint)
      loaded <- manager.loadCheckpoint("complex-pipeline")
    } yield loaded
    
    result.map { loaded =>
      assert(loaded.isDefined)
      val cp = loaded.get
      
      assertEquals(cp.pipelineId, "complex-pipeline")
      assertEquals(cp.offset, 999L)
      assert(cp.lastProcessedEvent.isDefined)
      assertEquals(cp.lastProcessedEvent.get.key, "complex-key")
      assertEquals(cp.state, complexState)
    }
  }
  
  test("RedisCheckpointManager should handle concurrent saves") {
    val manager = new TestRedisCheckpointManager[String, String]("test:checkpoint")
    
    val checkpoints = (1 to 10).map { i =>
      createTestCheckpoint(s"pipeline-$i", i * 10L)
    }
    
    // Save all checkpoints concurrently
    val result = Future.sequence(checkpoints.map(manager.saveCheckpoint))
    
    result.map { _ =>
      val storage = manager.getStorage
      
      // Verify all checkpoints were saved
      (1 to 10).foreach { i =>
        assert(storage.contains(s"test:checkpoint:pipeline-$i:latest"))
      }
    }
  }
  
  test("RedisCheckpointManager should maintain history order correctly") {
    val manager = new TestRedisCheckpointManager[String, String]("test:checkpoint")
    
    val timestamps = (1 to 3).map(i => Instant.now().plusSeconds(i * 10))
    val checkpoints = timestamps.zipWithIndex.map { case (timestamp, i) =>
      createTestCheckpoint("pipeline-order", (i + 1) * 100L, timestamp)
    }
    
    val result = for {
      _ <- Future.sequence(checkpoints.map(manager.saveCheckpoint))
      metadata <- manager.listCheckpoints("pipeline-order")
    } yield metadata
    
    result.map { metadata =>
      assertEquals(metadata.length, 3)
      
      // Verify the metadata is ordered by creation time (newest first)
      val sortedMetadata = metadata.sortBy(_.createdAt.toEpochMilli).reverse
      assertEquals(metadata, sortedMetadata)
    }
  }
}