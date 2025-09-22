package gibbon.checkpoint

import gibbon.core.Event
import munit.FunSuite
import scala.concurrent.{Future, ExecutionContext}
import scala.concurrent.duration._
import java.time.Instant
import scala.util.{Success, Failure}

/**
 * Test suite for CheckpointManager interface
 * Tests the core checkpoint operations: save, load, list, delete
 */
class CheckpointManagerSpec extends FunSuite {
  
  implicit val ec: ExecutionContext = ExecutionContext.global
  
  // Mock implementation for testing the interface
  class MockCheckpointManager extends CheckpointManager[String, String] {
    private var checkpoints = Map.empty[String, Checkpoint[String, String]]
    private var history = Map.empty[String, List[Checkpoint[String, String]]]
    
    override def saveCheckpoint(checkpoint: Checkpoint[String, String]): Future[Unit] = {
      checkpoints = checkpoints + (checkpoint.pipelineId -> checkpoint)
      val currentHistory = history.getOrElse(checkpoint.pipelineId, List.empty)
      history = history + (checkpoint.pipelineId -> (checkpoint :: currentHistory))
      Future.successful(())
    }
    
    override def loadCheckpoint(pipelineId: String): Future[Option[Checkpoint[String, String]]] = {
      Future.successful(checkpoints.get(pipelineId))
    }
    
    override def listCheckpoints(pipelineId: String): Future[List[CheckpointMetadata]] = {
      val checkpointHistory = history.getOrElse(pipelineId, List.empty)
      val metadata = checkpointHistory.map { checkpoint =>
        CheckpointMetadata(
          pipelineId = checkpoint.pipelineId,
          version = checkpoint.offset,
          createdAt = checkpoint.timestamp,
          checkpointType = Periodic
        )
      }
      Future.successful(metadata)
    }
    
    override def deleteCheckpoint(pipelineId: String, version: Long): Future[Unit] = {
      checkpoints = checkpoints - pipelineId
      history = history - pipelineId
      Future.successful(())
    }
    
    // Test helper methods
    def clear(): Unit = {
      checkpoints = Map.empty
      history = Map.empty
    }
    
    def getStoredCheckpoints: Map[String, Checkpoint[String, String]] = checkpoints
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
  
  test("saveCheckpoint should store checkpoint successfully") {
    val manager = new MockCheckpointManager()
    val checkpoint = createTestCheckpoint("pipeline-1", 50L)
    
    val result = manager.saveCheckpoint(checkpoint)
    
    result.map { _ =>
      val stored = manager.getStoredCheckpoints
      assert(stored.contains("pipeline-1"))
      assertEquals(stored("pipeline-1").offset, 50L)
    }
  }
  
  test("loadCheckpoint should return stored checkpoint") {
    val manager = new MockCheckpointManager()
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
  
  test("loadCheckpoint should return None for non-existent pipeline") {
    val manager = new MockCheckpointManager()
    
    val result = manager.loadCheckpoint("non-existent")
    
    result.map { loaded =>
      assert(loaded.isEmpty)
    }
  }
  
  test("listCheckpoints should return checkpoint metadata") {
    val manager = new MockCheckpointManager()
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
      assert(metadata.exists(_.version == 100L))
      assert(metadata.exists(_.version == 200L))
      assert(metadata.forall(_.pipelineId == "pipeline-3"))
      assert(metadata.forall(_.checkpointType == Periodic))
    }
  }
  
  test("listCheckpoints should return empty list for non-existent pipeline") {
    val manager = new MockCheckpointManager()
    
    val result = manager.listCheckpoints("non-existent")
    
    result.map { metadata =>
      assert(metadata.isEmpty)
    }
  }
  
  test("deleteCheckpoint should remove checkpoint") {
    val manager = new MockCheckpointManager()
    val checkpoint = createTestCheckpoint("pipeline-4", 150L)
    
    val result = for {
      _ <- manager.saveCheckpoint(checkpoint)
      _ <- manager.deleteCheckpoint("pipeline-4", 150L)
      loaded <- manager.loadCheckpoint("pipeline-4")
    } yield loaded
    
    result.map { loaded =>
      assert(loaded.isEmpty)
    }
  }
  
  test("saveCheckpoint should handle multiple pipelines") {
    val manager = new MockCheckpointManager()
    val checkpoint1 = createTestCheckpoint("pipeline-a", 100L)
    val checkpoint2 = createTestCheckpoint("pipeline-b", 200L)
    
    val result = for {
      _ <- manager.saveCheckpoint(checkpoint1)
      _ <- manager.saveCheckpoint(checkpoint2)
      loaded1 <- manager.loadCheckpoint("pipeline-a")
      loaded2 <- manager.loadCheckpoint("pipeline-b")
    } yield (loaded1, loaded2)
    
    result.map { case (loaded1, loaded2) =>
      assert(loaded1.isDefined)
      assert(loaded2.isDefined)
      assertEquals(loaded1.get.offset, 100L)
      assertEquals(loaded2.get.offset, 200L)
    }
  }
  
  test("saveCheckpoint should update existing checkpoint") {
    val manager = new MockCheckpointManager()
    val checkpoint1 = createTestCheckpoint("pipeline-5", 100L)
    val checkpoint2 = createTestCheckpoint("pipeline-5", 200L)
    
    val result = for {
      _ <- manager.saveCheckpoint(checkpoint1)
      _ <- manager.saveCheckpoint(checkpoint2)
      loaded <- manager.loadCheckpoint("pipeline-5")
    } yield loaded
    
    result.map { loaded =>
      assert(loaded.isDefined)
      assertEquals(loaded.get.offset, 200L) // Should have the latest checkpoint
    }
  }
  
  test("checkpoint should contain all required fields") {
    val timestamp = Instant.now()
    val event = Event("test-key", "test-value", System.currentTimeMillis())
    val state = Map("count" -> "42", "status" -> "active")
    
    val checkpoint = createTestCheckpoint(
      pipelineId = "test-pipeline",
      offset = 123L,
      timestamp = timestamp,
      lastProcessedEvent = Some(event),
      state = state
    )
    
    assertEquals(checkpoint.pipelineId, "test-pipeline")
    assertEquals(checkpoint.offset, 123L)
    assertEquals(checkpoint.timestamp, timestamp)
    assert(checkpoint.lastProcessedEvent.isDefined)
    assertEquals(checkpoint.lastProcessedEvent.get.key, "test-key")
    assertEquals(checkpoint.state, state)
  }
  
  test("CheckpointMetadata should contain all required fields") {
    val timestamp = Instant.now()
    val metadata = CheckpointMetadata(
      pipelineId = "test-pipeline",
      version = 456L,
      createdAt = timestamp,
      checkpointType = Manual
    )
    
    assertEquals(metadata.pipelineId, "test-pipeline")
    assertEquals(metadata.version, 456L)
    assertEquals(metadata.createdAt, timestamp)
    assertEquals(metadata.checkpointType, Manual)
  }
  
  test("CheckpointType should have all expected variants") {
    val periodic: CheckpointType = Periodic
    val onFailure: CheckpointType = OnFailure
    val manual: CheckpointType = Manual
    
    // Ensure all types are distinct
    assert(periodic != onFailure)
    assert(onFailure != manual)
    assert(manual != periodic)
  }
}