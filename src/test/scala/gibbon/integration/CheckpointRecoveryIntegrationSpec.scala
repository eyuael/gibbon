package gibbon.integration

import gibbon.core.{Event, RecoverablePipeline, Source, Flow, Sink}
import gibbon.checkpoint.{CheckpointManager, Checkpoint, CheckpointMetadata, CheckpointType, Periodic}
import gibbon.operations.EventStore
import gibbon.flows.CheckpointingFlow
import munit.FunSuite
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Source => AkkaSource, Sink => AkkaSink, Flow => AkkaFlow}
import scala.concurrent.{Future, ExecutionContext, Promise}
import scala.concurrent.duration._
import java.time.Instant
import java.util.concurrent.atomic.{AtomicReference, AtomicLong}
import scala.util.{Success, Failure}
import io.circe._
import io.circe.syntax._
import io.circe.parser._
import io.circe.generic.auto._

/**
 * Integration test suite for checkpoint recovery scenarios
 * Tests complete recovery workflows including state restoration and pipeline resumption
 */
class CheckpointRecoveryIntegrationSpec extends FunSuite {
  
  implicit val system: ActorSystem = ActorSystem("CheckpointRecoveryIntegrationSpec")
  implicit val ec: ExecutionContext = system.dispatcher
  
  override def afterAll(): Unit = {
    system.terminate()
    super.afterAll()
  }
  
  // Test data types
  case class TestEvent(id: Long, data: String, timestamp: Long = System.currentTimeMillis())
  case class ProcessedEvent(originalId: Long, processedData: String, processingTime: Long)
  
  // Test EventStore implementation
  class TestEventStore extends EventStore[String, TestEvent] {
    private val storage = new AtomicReference(Map.empty[String, TestEvent])
    
    override def get(key: String): Future[Option[TestEvent]] = {
      Future.successful(storage.get().get(key))
    }
    
    override def put(key: String, value: TestEvent): Future[Unit] = {
      storage.updateAndGet(_ + (key -> value))
      Future.successful(())
    }
    
    override def delete(key: String): Future[Unit] = {
      storage.updateAndGet(_ - key)
      Future.successful(())
    }
    
    override def getAll: Future[Map[String, TestEvent]] = {
      Future.successful(storage.get())
    }
    
    // Test helper methods
    def clear(): Unit = storage.set(Map.empty)
    def size: Int = storage.get().size
  }
  
  // Test CheckpointManager implementation
  class TestCheckpointManager extends CheckpointManager[String, TestEvent] {
    private val checkpoints = new AtomicReference(Map.empty[String, Checkpoint[String, TestEvent]])
    private val history = new AtomicReference(Map.empty[String, List[CheckpointMetadata]])
    
    override def saveCheckpoint(checkpoint: Checkpoint[String, TestEvent]): Future[Unit] = {
      checkpoints.updateAndGet(_ + (checkpoint.pipelineId -> checkpoint))
      val metadata = CheckpointMetadata(
        pipelineId = checkpoint.pipelineId,
        version = checkpoint.offset,
        createdAt = checkpoint.timestamp,
        checkpointType = Periodic
      )
      history.updateAndGet { h =>
        val currentHistory = h.getOrElse(checkpoint.pipelineId, List.empty)
        h + (checkpoint.pipelineId -> (metadata :: currentHistory))
      }
      Future.successful(())
    }
    
    override def loadCheckpoint(pipelineId: String): Future[Option[Checkpoint[String, TestEvent]]] = {
      Future.successful(checkpoints.get().get(pipelineId))
    }
    
    override def listCheckpoints(pipelineId: String): Future[List[CheckpointMetadata]] = {
      Future.successful(history.get().getOrElse(pipelineId, List.empty))
    }
    
    override def deleteCheckpoint(pipelineId: String, version: Long): Future[Unit] = {
      checkpoints.updateAndGet(_ - pipelineId)
      history.updateAndGet(_ - pipelineId)
      Future.successful(())
    }
    
    // Test helper methods
    def clear(): Unit = {
      checkpoints.set(Map.empty)
      history.set(Map.empty)
    }
    
    def hasCheckpoint(pipelineId: String): Boolean = checkpoints.get().contains(pipelineId)
    def getCheckpointCount: Int = checkpoints.get().size
  }
  
  // Test Source implementation
  class TestSource(events: List[TestEvent]) extends Source[TestEvent] {
    override def toAkkaSource(): AkkaSource[TestEvent, _] = {
      AkkaSource(events)
    }
  }
  
  // Test Flow implementation
  class TestFlow extends Flow[TestEvent, ProcessedEvent] {
    override def toAkkaFlow(): AkkaFlow[TestEvent, ProcessedEvent, _] = {
      AkkaFlow[TestEvent].map { event =>
        ProcessedEvent(
          originalId = event.id,
          processedData = s"processed-${event.data}",
          processingTime = System.currentTimeMillis()
        )
      }
    }
  }
  
  // Test Sink implementation
  class TestSink extends Sink[ProcessedEvent] {
    private val results = new AtomicReference(List.empty[ProcessedEvent])
    
    override def toAkkaSink(): AkkaSink[ProcessedEvent, Future[Unit]] = {
      AkkaSink.foreach[ProcessedEvent] { event =>
        results.updateAndGet(event :: _)
      }.mapMaterializedValue(_ => Future.successful(()))
    }
    
    override def close(): Future[Unit] = Future.successful(())
    
    def getResults: List[ProcessedEvent] = {
      val currentResults = results.get().reverse
      currentResults
    }
    def clear(): Unit = results.set(List.empty)
  }
  
  private def createTestPipeline(
    pipelineId: String,
    events: List[TestEvent],
    checkpointManager: TestCheckpointManager,
    sink: TestSink
  ): RecoverablePipeline[TestEvent, ProcessedEvent, String, TestEvent] = {
    val source = new TestSource(events)
    val flow = new TestFlow()
    
    new RecoverablePipeline(
      pipelineId = pipelineId,
      source = source,
      flow = flow,
      sink = sink,
      checkpointManager = checkpointManager,
      extractOffset = (event: TestEvent) => event.id
    )
  }
  
  test("RecoverablePipeline should start from beginning when no checkpoint exists") {
    val checkpointManager = new TestCheckpointManager()
    val sink = new TestSink()
    val events = List(
      TestEvent(1, "event1"),
      TestEvent(2, "event2"),
      TestEvent(3, "event3")
    )
    
    val pipeline = createTestPipeline("test-pipeline-1", events, checkpointManager, sink)
    
    val result = pipeline.runWithRecovery()
    
    result.flatMap { _ =>
      // Wait for stream completion and then get results
      akka.pattern.after(100.millis, system.scheduler)(Future.successful(())).map { _ =>
        val finalResults = sink.getResults
        
        // Should process all events from the beginning
        assertEquals(finalResults.length, 3)
        assertEquals(finalResults.map(_.originalId), List(1L, 2L, 3L))
        
        // Should not have any checkpoints initially
        assertEquals(checkpointManager.getCheckpointCount, 0)
      }
    }
  }
  
  test("RecoverablePipeline should recover from existing checkpoint") {
    val checkpointManager = new TestCheckpointManager()
    val sink = new TestSink()
    val events = List(
      TestEvent(1, "event1"),
      TestEvent(2, "event2"),
      TestEvent(3, "event3"),
      TestEvent(4, "event4"),
      TestEvent(5, "event5")
    )
    
    // Create a checkpoint at offset 2 (processed first 2 events)
    val checkpoint = Checkpoint[String, TestEvent](
      pipelineId = "test-pipeline-2",
      offset = 2L,
      timestamp = Instant.now(),
      lastProcessedEvent = Some(Event("key2", TestEvent(2, "event2"), System.currentTimeMillis())),
      state = Map("processedCount" -> "2")
    )
    
    val result = for {
      _ <- checkpointManager.saveCheckpoint(checkpoint)
      pipeline = createTestPipeline("test-pipeline-2", events, checkpointManager, sink)
      completion <- pipeline.runWithRecovery()
      _ <- akka.pattern.after(100.millis, system.scheduler)(Future.successful(())) // Delay to ensure all elements are processed
    } yield sink.getResults
    
    result.map { results =>
      // Should only process events after the checkpoint (events 3, 4, 5)
      assertEquals(results.length, 3)
      assertEquals(results.map(_.originalId), List(3L, 4L, 5L))
    }
  }
  
  test("CheckpointingFlow should create checkpoints during processing and enable recovery") {
    val checkpointManager = new TestCheckpointManager()
    val eventStore = new TestEventStore()
    
    val events = (1 to 10).map(i => TestEvent(i, s"event$i")).toList
    val source = AkkaSource(events)
    
    // Create checkpointing flow with short interval
    val checkpointingFlow = new CheckpointingFlow[TestEvent, String, TestEvent](
      pipelineId = "checkpointing-test",
      checkpointInterval = 50.millis,
      extractOffset = _.id,
      checkpointManager = checkpointManager
    )
    
    val result = for {
      // Process events with checkpointing
      processedEvents <- source
        .throttle(1, 20.millis) // Slow processing to allow checkpoints
        .via(checkpointingFlow.toAkkaFlow())
        .runWith(AkkaSink.seq)
      
      // Wait for checkpoints to be saved
      _ <- akka.pattern.after(200.millis, system.scheduler)(Future.successful(()))
      
      // Verify checkpoints were created
      checkpoints <- checkpointManager.listCheckpoints("checkpointing-test")
    } yield (processedEvents, checkpoints)
    
    result.map { case (processedEvents, checkpoints) =>
      // All events should be processed
      assertEquals(processedEvents.length, 10)
      
      // Should have created at least one checkpoint
      assert(checkpoints.nonEmpty)
      
      // Checkpoint should have valid metadata
      val latestCheckpoint = checkpoints.head
      assertEquals(latestCheckpoint.pipelineId, "checkpointing-test")
      assert(latestCheckpoint.version > 0)
      assertEquals(latestCheckpoint.checkpointType, Periodic)
    }
  }
  
  test("Integration: EventStore state should be preserved across pipeline restarts") {
    val checkpointManager = new TestCheckpointManager()
    val eventStore = new TestEventStore()
    val sink = new TestSink()
    val sink2 = new TestSink()
    
    val initialEvents = List(
      TestEvent(1, "event1"),
      TestEvent(2, "event2"),
      TestEvent(3, "event3")
    )
    
    val additionalEvents = List(
      TestEvent(4, "event4"),
      TestEvent(5, "event5")
    )
    
    val result = for {
      // Store some initial state in EventStore
      _ <- eventStore.put("state1", TestEvent(100, "initial-state"))
      _ <- eventStore.put("state2", TestEvent(200, "another-state"))
      
      // Run first pipeline and create checkpoint
      pipeline1 = createTestPipeline("state-test", initialEvents, checkpointManager, sink)
      _ <- pipeline1.runWithRecovery()
      
      // Simulate checkpoint creation after processing
      checkpoint = Checkpoint[String, TestEvent](
        pipelineId = "state-test",
        offset = 2L, // Changed from 3L to 2L - checkpoint after processing events 1,2
        timestamp = Instant.now(),
        lastProcessedEvent = Some(Event("key2", TestEvent(2, "event2"), System.currentTimeMillis())),
        state = Map("processedCount" -> "2", "stateKey" -> "preserved") // Changed from "3" to "2"
      )
      _ <- checkpointManager.saveCheckpoint(checkpoint)
      
      // Create a new sink for second run to avoid any race conditions
      // (sink2 already declared at test start)
      
      // Run second pipeline with additional events (should recover from checkpoint)
      pipeline2 = createTestPipeline("state-test", initialEvents ++ additionalEvents, checkpointManager, sink2)
      _ <- pipeline2.runWithRecovery()
      
      // Verify EventStore state is preserved
      state1 <- eventStore.get("state1")
      state2 <- eventStore.get("state2")
      allStates <- eventStore.getAll
      
    } yield (sink.getResults, sink2.getResults, state1, state2, allStates)
    
    result.flatMap { case (firstResults, secondResults, state1, state2, allStates) =>
      // Wait for stream completion and then get results
      akka.pattern.after(100.millis, system.scheduler)(Future.successful(())).map { _ =>
        val finalFirstResults = sink.getResults
        val finalSecondResults = sink2.getResults
        
        // First run should process all events (no checkpoint exists yet)
        assertEquals(finalFirstResults.length, 3) // Events 1, 2, 3
        assertEquals(finalFirstResults.map(_.originalId), List(1L, 2L, 3L))
        
        // Second run should process events 3, 4, 5 after recovery from offset 2
        assertEquals(finalSecondResults.length, 3) // Events 3, 4, 5 from recovery
        assertEquals(finalSecondResults.map(_.originalId), List(3L, 4L, 5L))
        
        // EventStore state should be preserved
        assert(state1.isDefined)
        assertEquals(state1.get.id, 100L)
        assertEquals(state1.get.data, "initial-state")
        
        assert(state2.isDefined)
        assertEquals(state2.get.id, 200L)
        assertEquals(state2.get.data, "another-state")
        
        // All states should be accessible
        assertEquals(allStates.size, 2)
      }
    }
  }
  
  test("Recovery should handle corrupted checkpoint gracefully") {
    val checkpointManager = new TestCheckpointManager()
    val sink = new TestSink()
    val events = List(
      TestEvent(1, "event1"),
      TestEvent(2, "event2")
    )
    
    // Create a checkpoint with invalid offset
    val corruptedCheckpoint = Checkpoint[String, TestEvent](
      pipelineId = "corrupted-test",
      offset = -1L, // Invalid offset
      timestamp = Instant.now(),
      lastProcessedEvent = None,
      state = Map.empty
    )
    
    val result = for {
      _ <- checkpointManager.saveCheckpoint(corruptedCheckpoint)
      pipeline = createTestPipeline("corrupted-test", events, checkpointManager, sink)
      _ <- pipeline.runWithRecovery()
    } yield sink.getResults
    
    result.flatMap { _ =>
      // Wait for stream completion and then get results
      akka.pattern.after(100.millis, system.scheduler)(Future.successful(())).map { _ =>
        val finalResults = sink.getResults
        
        // Should still process events (fallback to beginning or handle gracefully)
        assert(finalResults.nonEmpty)
      }
    }
  }
  
  test("Multiple pipelines should maintain separate checkpoint states") {
    val checkpointManager = new TestCheckpointManager()
    val sink1 = new TestSink()
    val sink2 = new TestSink()
    
    val events1 = List(TestEvent(1, "pipeline1-event1"), TestEvent(2, "pipeline1-event2"))
    val events2 = List(TestEvent(10, "pipeline2-event1"), TestEvent(20, "pipeline2-event2"))
    
    val checkpoint1 = Checkpoint[String, TestEvent](
      pipelineId = "pipeline-1",
      offset = 1L,
      timestamp = Instant.now(),
      lastProcessedEvent = Some(Event("key1", TestEvent(1, "pipeline1-event1"), System.currentTimeMillis())),
      state = Map("processedCount" -> "1")
    )
    
    val checkpoint2 = Checkpoint[String, TestEvent](
      pipelineId = "pipeline-2",
      offset = 10L,
      timestamp = Instant.now(),
      lastProcessedEvent = Some(Event("key10", TestEvent(10, "pipeline2-event1"), System.currentTimeMillis())),
      state = Map("processedCount" -> "1")
    )
    
    val result = for {
      _ <- checkpointManager.saveCheckpoint(checkpoint1)
      _ <- checkpointManager.saveCheckpoint(checkpoint2)
      
      pipeline1 = createTestPipeline("pipeline-1", events1, checkpointManager, sink1)
      pipeline2 = createTestPipeline("pipeline-2", events2, checkpointManager, sink2)
      
      _ <- pipeline1.runWithRecovery()
      _ <- pipeline2.runWithRecovery()
      
      checkpoints1 <- checkpointManager.listCheckpoints("pipeline-1")
      checkpoints2 <- checkpointManager.listCheckpoints("pipeline-2")
      
    } yield (sink1.getResults, sink2.getResults, checkpoints1, checkpoints2)
    
    result.flatMap { case (results1, results2, checkpoints1, checkpoints2) =>
      // Wait for stream completion and then get results
      akka.pattern.after(100.millis, system.scheduler)(Future.successful(())).map { _ =>
        val finalResults1 = sink1.getResults
        val finalResults2 = sink2.getResults
        
        // Each pipeline should process remaining events after their respective checkpoints
        assertEquals(finalResults1.length, 1) // Only event 2 after checkpoint at offset 1
        assertEquals(finalResults2.length, 1) // Only event 20 after checkpoint at offset 10
        
        assertEquals(finalResults1.head.originalId, 2L)
        assertEquals(finalResults2.head.originalId, 20L)
        
        // Each pipeline should have separate checkpoint history
        assert(checkpoints1.nonEmpty)
        assert(checkpoints2.nonEmpty)
        assertEquals(checkpoints1.head.pipelineId, "pipeline-1")
        assertEquals(checkpoints2.head.pipelineId, "pipeline-2")
      }
    }
  }
  
  test("Recovery should work with empty event streams") {
    val checkpointManager = new TestCheckpointManager()
    val sink = new TestSink()
    val emptyEvents = List.empty[TestEvent]
    
    val checkpoint = Checkpoint[String, TestEvent](
      pipelineId = "empty-test",
      offset = 0L,
      timestamp = Instant.now(),
      lastProcessedEvent = None,
      state = Map("processedCount" -> "0")
    )
    
    val result = for {
      _ <- checkpointManager.saveCheckpoint(checkpoint)
      pipeline = createTestPipeline("empty-test", emptyEvents, checkpointManager, sink)
      _ <- pipeline.runWithRecovery()
    } yield sink.getResults
    
    result.map { results =>
      // Should handle empty streams gracefully
      assertEquals(results.length, 0)
    }
  }
  
  test("Checkpoint recovery should preserve processing state and metadata") {
    val checkpointManager = new TestCheckpointManager()
    val eventStore = new TestEventStore()
    
    val events = List(
      TestEvent(1, "event1"),
      TestEvent(2, "event2"),
      TestEvent(3, "event3")
    )
    
    val complexState = Map(
      "processedCount" -> "2",
      "lastProcessedId" -> "2",
      "errorCount" -> "0",
      "status" -> "running",
      "customMetadata" -> "test-value"
    )
    
    val checkpoint = Checkpoint[String, TestEvent](
      pipelineId = "state-preservation-test",
      offset = 2L,
      timestamp = Instant.now().minusSeconds(60), // 1 minute ago
      lastProcessedEvent = Some(Event("key2", TestEvent(2, "event2"), System.currentTimeMillis())),
      state = complexState
    )
    
    val result = for {
      _ <- checkpointManager.saveCheckpoint(checkpoint)
      loadedCheckpoint <- checkpointManager.loadCheckpoint("state-preservation-test")
      metadata <- checkpointManager.listCheckpoints("state-preservation-test")
    } yield (loadedCheckpoint, metadata)
    
    result.map { case (loadedCheckpoint, metadata) =>
      // Checkpoint should be loaded correctly
      assert(loadedCheckpoint.isDefined)
      val cp = loadedCheckpoint.get
      
      assertEquals(cp.pipelineId, "state-preservation-test")
      assertEquals(cp.offset, 2L)
      assertEquals(cp.state, complexState)
      
      // Last processed event should be preserved
      assert(cp.lastProcessedEvent.isDefined)
      assertEquals(cp.lastProcessedEvent.get.value.id, 2L)
      assertEquals(cp.lastProcessedEvent.get.value.data, "event2")
      
      // Metadata should be available
      assert(metadata.nonEmpty)
      val meta = metadata.head
      assertEquals(meta.pipelineId, "state-preservation-test")
      assertEquals(meta.version, 2L)
      assertEquals(meta.checkpointType, Periodic)
    }
  }
  
  test("Integration: Full recovery workflow with EventStore and CheckpointManager") {
    val checkpointManager = new TestCheckpointManager()
    val eventStore = new TestEventStore()
    val sink = new TestSink()
    
    val events = (1 to 5).map(i => TestEvent(i, s"event$i")).toList
    
    val result = for {
      // Store initial application state
      _ <- eventStore.put("config", TestEvent(999, "app-config"))
      _ <- eventStore.put("counter", TestEvent(0, "initial-counter"))
      
      // Create and save a checkpoint
      checkpoint = Checkpoint[String, TestEvent](
        pipelineId = "full-integration-test",
        offset = 3L,
        timestamp = Instant.now(),
        lastProcessedEvent = Some(Event("key3", TestEvent(3, "event3"), System.currentTimeMillis())),
        state = Map(
          "processedCount" -> "3",
          "lastOffset" -> "3",
          "appState" -> "running"
        )
      )
      _ <- checkpointManager.saveCheckpoint(checkpoint)
      
      // Run pipeline with recovery
      pipeline = createTestPipeline("full-integration-test", events, checkpointManager, sink)
      _ <- pipeline.runWithRecovery()
      
      // Verify final state
      finalConfig <- eventStore.get("config")
      finalCounter <- eventStore.get("counter")
      finalCheckpoint <- checkpointManager.loadCheckpoint("full-integration-test")
      
    } yield (sink.getResults, finalConfig, finalCounter, finalCheckpoint)
    
    result.flatMap { case (results, config, counter, checkpoint) =>
      // Wait for stream completion and then get results
      akka.pattern.after(200.millis, system.scheduler)(Future.successful(())).map { _ =>
        val finalResults = sink.getResults
        
        // Should process events 4 and 5 after recovery from offset 3
        assertEquals(finalResults.length, 2)
        assertEquals(finalResults.map(_.originalId), List(4L, 5L))
        
        // EventStore state should be preserved
        assert(config.isDefined)
        assertEquals(config.get.data, "app-config")
        
        assert(counter.isDefined)
        assertEquals(counter.get.data, "initial-counter")
        
        // Checkpoint should be preserved
        assert(checkpoint.isDefined)
        assertEquals(checkpoint.get.offset, 3L)
        assertEquals(checkpoint.get.state("processedCount"), "3")
      }
    }
  }
}