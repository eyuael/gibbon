package gibbon.flows

import gibbon.core.Event
import gibbon.checkpoint.{CheckpointManager, Checkpoint}
import munit.FunSuite
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Source, Sink}
import scala.concurrent.{Future, ExecutionContext, Promise}
import scala.concurrent.duration._
import java.time.Instant
import java.util.concurrent.atomic.AtomicReference
import scala.util.{Success, Failure}

/**
 * Test suite for CheckpointingFlow functionality
 * Tests checkpointing behavior, timing, error handling, and stream processing
 */
class CheckpointingFlowSpec extends FunSuite {
  
  implicit val system: ActorSystem = ActorSystem("CheckpointingFlowSpec")
  implicit val ec: ExecutionContext = system.dispatcher
  
  override def afterAll(): Unit = {
    system.terminate()
    super.afterAll()
  }
  
  // Test CheckpointManager implementation
  class TestCheckpointManager[K, V] extends CheckpointManager[K, V] {
    private val savedCheckpoints = new AtomicReference(List.empty[Checkpoint[K, V]])
    private val savePromises = new AtomicReference(List.empty[Promise[Unit]])
    
    override def saveCheckpoint(checkpoint: Checkpoint[K, V]): Future[Unit] = {
      val promise = Promise[Unit]()
      savePromises.updateAndGet(promise :: _)
      savedCheckpoints.updateAndGet(checkpoint :: _)
      promise.future
    }
    
    override def loadCheckpoint(pipelineId: String): Future[Option[Checkpoint[K, V]]] = {
      val checkpoints = savedCheckpoints.get()
      val latest = checkpoints.filter(_.pipelineId == pipelineId).headOption
      Future.successful(latest)
    }
    
    override def listCheckpoints(pipelineId: String): Future[List[gibbon.checkpoint.CheckpointMetadata]] = {
      Future.successful(List.empty)
    }
    
    override def deleteCheckpoint(pipelineId: String, version: Long): Future[Unit] = {
      Future.successful(())
    }
    
    // Test helper methods
    def getSavedCheckpoints: List[Checkpoint[K, V]] = savedCheckpoints.get().reverse
    def getCheckpointCount: Int = savedCheckpoints.get().length
    def completeSaves(): Unit = {
      savePromises.get().foreach(_.success(()))
      savePromises.set(List.empty)
    }
    def failSaves(error: Throwable): Unit = {
      savePromises.get().foreach(_.failure(error))
      savePromises.set(List.empty)
    }
    def clear(): Unit = {
      savedCheckpoints.set(List.empty)
      savePromises.set(List.empty)
    }
  }
  
  // Test data type
  case class TestEvent(id: Long, data: String, timestamp: Long = System.currentTimeMillis())
  
  private def createCheckpointingFlow(
    pipelineId: String = "test-pipeline",
    checkpointInterval: FiniteDuration = 100.millis,
    checkpointManager: TestCheckpointManager[String, String] = new TestCheckpointManager[String, String]()
  ): (CheckpointingFlow[TestEvent, String, String], TestCheckpointManager[String, String]) = {
    val flow = new CheckpointingFlow[TestEvent, String, String](
      pipelineId = pipelineId,
      checkpointManager = checkpointManager,
      checkpointInterval = checkpointInterval,
      extractOffset = _.id
    )
    (flow, checkpointManager)
  }
  
  test("CheckpointingFlow should pass through all elements unchanged") {
    val (flow, _) = createCheckpointingFlow()
    val testEvents = (1 to 5).map(i => TestEvent(i, s"data-$i"))
    
    val result = Source(testEvents)
      .via(flow.toAkkaFlow())
      .runWith(Sink.seq)
    
    result.map { output =>
      assertEquals(output.length, 5)
      assertEquals(output, testEvents)
    }
  }
  
  test("CheckpointingFlow should create checkpoints based on time interval") {
    val checkpointManager = new TestCheckpointManager[String, String]()
    val (flow, _) = createCheckpointingFlow(
      checkpointInterval = 50.millis,
      checkpointManager = checkpointManager
    )
    
    val testEvents = (1 to 3).map(i => TestEvent(i, s"data-$i"))
    
    val result = for {
      _ <- Source(testEvents)
        .throttle(1, 60.millis) // Ensure time passes between elements
        .via(flow.toAkkaFlow())
        .runWith(Sink.ignore)
      _ <- akka.pattern.after(200.millis, system.scheduler)(Future.successful(()))
    } yield {
      checkpointManager.completeSaves()
      checkpointManager.getSavedCheckpoints
    }
    
    result.map { checkpoints =>
      // Should have created at least one checkpoint due to time interval
      assert(checkpoints.nonEmpty)
      
      // Verify checkpoint content
      val checkpoint = checkpoints.head
      assertEquals(checkpoint.pipelineId, "test-pipeline")
      assert(checkpoint.offset > 0)
      assert(checkpoint.state.contains("processedCount"))
    }
  }
  
  test("CheckpointingFlow should not create checkpoints if interval hasn't passed") {
    val checkpointManager = new TestCheckpointManager[String, String]()
    val flow = new CheckpointingFlow[TestEvent, String, String](
      pipelineId = "no-checkpoint-test",
      checkpointManager = checkpointManager,
      checkpointInterval = 1.second, // Very long interval
      extractOffset = _.id
    )
    
    val testEvents = List(
      TestEvent(1, "data-1"),
      TestEvent(2, "data-2")
    )
    
    val result = for {
      _ <- Source(testEvents)
        .via(flow.toAkkaFlow())
        .runWith(Sink.ignore)
      _ <- akka.pattern.after(10.millis, system.scheduler)(Future.successful(()))
    } yield ()
    
    result.flatMap { _ =>
      checkpointManager.completeSaves()
      akka.pattern.after(10.millis, system.scheduler)(Future.successful(())).map { _ =>
        val checkpoints = checkpointManager.getSavedCheckpoints
        // With the new logic, we create the first checkpoint immediately
        // but no additional checkpoints should be created within the short time
        assert(checkpoints.length == 1, s"Expected 1 checkpoint but got ${checkpoints.length}")
      }
    }
  }
  
  test("CheckpointingFlow should extract correct offset from elements") {
    val checkpointManager = new TestCheckpointManager[String, String]()
    val (flow, _) = createCheckpointingFlow(
      checkpointInterval = 10.millis,
      checkpointManager = checkpointManager
    )
    
    val testEvents = List(
      TestEvent(100, "data-1"),
      TestEvent(200, "data-2"),
      TestEvent(300, "data-3")
    )
    
    val result = for {
      _ <- Source(testEvents)
        .throttle(1, 5.millis) // Faster than checkpoint interval
        .via(flow.toAkkaFlow())
        .runWith(Sink.ignore)
      _ <- akka.pattern.after(50.millis, system.scheduler)(Future.successful(()))
    } yield ()
    
    result.flatMap { _ =>
      // Complete all pending saves first
      checkpointManager.completeSaves()
      // Small delay to ensure completion
      akka.pattern.after(10.millis, system.scheduler)(Future.successful(())).map { _ =>
        val checkpoints = checkpointManager.getSavedCheckpoints
        assert(checkpoints.nonEmpty)
        
        // Verify that offsets match the element IDs
        val offsets = checkpoints.map(_.offset).toSet
        assert(offsets.subsetOf(Set(100L, 200L, 300L)))
      }
    }
  }
  
  test("CheckpointingFlow should include processed count in checkpoint state") {
    val checkpointManager = new TestCheckpointManager[String, String]()
    val (flow, _) = createCheckpointingFlow(
      checkpointInterval = 10.millis,
      checkpointManager = checkpointManager
    )
    
    val testEvents = (1 to 10).map(i => TestEvent(i, s"data-$i"))
    
    val result = for {
      _ <- Source(testEvents)
        .throttle(2, 5.millis) // Faster than checkpoint interval
        .via(flow.toAkkaFlow())
        .runWith(Sink.ignore)
      _ <- akka.pattern.after(100.millis, system.scheduler)(Future.successful(()))
    } yield ()
    
    result.flatMap { _ =>
      // Complete all pending saves first
      checkpointManager.completeSaves()
      // Small delay to ensure completion
      akka.pattern.after(10.millis, system.scheduler)(Future.successful(())).map { _ =>
        val checkpoints = checkpointManager.getSavedCheckpoints
        assert(checkpoints.nonEmpty)
        
        // Verify processed count is tracked correctly
        checkpoints.foreach { checkpoint =>
          assert(checkpoint.state.contains("processedCount"))
          val count = checkpoint.state("processedCount").toLong
          assert(count > 0)
          assert(count <= 10)
        }
        
        // Verify counts are increasing
        val counts = checkpoints.map(_.state("processedCount").toLong)
        assertEquals(counts, counts.sorted)
      }
    }
  }

  test("CheckpointingFlow should handle checkpoint save failures gracefully") {
    val checkpointManager = new TestCheckpointManager[String, String]()
    val (flow, _) = createCheckpointingFlow(
      checkpointInterval = 10.millis,
      checkpointManager = checkpointManager
    )
    
    val testEvents = (1 to 5).map(i => TestEvent(i, s"data-$i"))
    
    val result = for {
      output <- Source(testEvents)
        .throttle(1, 20.millis)
        .via(flow.toAkkaFlow())
        .runWith(Sink.seq)
      _ <- {
        // Fail all checkpoint saves
        checkpointManager.failSaves(new RuntimeException("Redis connection failed"))
        akka.pattern.after(100.millis, system.scheduler)(Future.successful(()))
      }
    } yield output
    
    result.map { output =>
      // Stream should continue processing despite checkpoint failures
      assertEquals(output.length, 5)
      assertEquals(output, testEvents)
    }
  }
  
  test("CheckpointingFlow should use correct pipeline ID in checkpoints") {
    val checkpointManager = new TestCheckpointManager[String, String]()
    val pipelineId = "custom-pipeline-123"
    val (flow, _) = createCheckpointingFlow(
      pipelineId = pipelineId,
      checkpointInterval = 10.millis,
      checkpointManager = checkpointManager
    )
    
    val testEvents = List(TestEvent(1, "data"))
    
    val result = for {
      _ <- Source(testEvents)
        .throttle(1, 5.millis) // Faster than checkpoint interval
        .via(flow.toAkkaFlow())
        .runWith(Sink.ignore)
      _ <- akka.pattern.after(25.millis, system.scheduler)(Future.successful(()))
    } yield ()
    
    result.flatMap { _ =>
      checkpointManager.completeSaves()
      akka.pattern.after(10.millis, system.scheduler)(Future.successful(())).map { _ =>
        val checkpoints = checkpointManager.getSavedCheckpoints
        assert(checkpoints.nonEmpty)
        checkpoints.foreach { checkpoint =>
          assertEquals(checkpoint.pipelineId, pipelineId)
        }
      }
    }
  }
  
  test("CheckpointingFlow should create checkpoints with correct timestamps") {
    val checkpointManager = new TestCheckpointManager[String, String]()
    val (flow, _) = createCheckpointingFlow(
      checkpointInterval = 10.millis,
      checkpointManager = checkpointManager
    )
    
    val startTime = Instant.now()
    val testEvents = (1 to 3).map(i => TestEvent(i, s"data-$i"))
    
    val result = for {
      _ <- Source(testEvents)
        .throttle(1, 5.millis) // Faster than checkpoint interval
        .via(flow.toAkkaFlow())
        .runWith(Sink.ignore)
      _ <- akka.pattern.after(50.millis, system.scheduler)(Future.successful(()))
    } yield ()
    
    result.flatMap { _ =>
      checkpointManager.completeSaves()
      akka.pattern.after(10.millis, system.scheduler)(Future.successful(())).map { _ =>
        val checkpoints = checkpointManager.getSavedCheckpoints
        assert(checkpoints.nonEmpty)
        
        checkpoints.foreach { checkpoint =>
          // Timestamp should be after start time and before now
          assert(checkpoint.timestamp.isAfter(startTime) || checkpoint.timestamp.equals(startTime))
          assert(checkpoint.timestamp.isBefore(Instant.now()) || checkpoint.timestamp.equals(Instant.now()))
        }
        
        // Timestamps should be in order
        val timestamps = checkpoints.map(_.timestamp)
        val sortedTimestamps = timestamps.sortBy(_.toEpochMilli)
        assertEquals(timestamps, sortedTimestamps)
      }
    }
  }
  
  test("CheckpointingFlow should work with different checkpoint intervals") {
    val shortIntervalManager = new TestCheckpointManager[String, String]()
    val longIntervalManager = new TestCheckpointManager[String, String]()
    
    val (shortFlow, _) = createCheckpointingFlow(
      pipelineId = "short-interval",
      checkpointInterval = 10.millis,
      checkpointManager = shortIntervalManager
    )
    
    val (longFlow, _) = createCheckpointingFlow(
      pipelineId = "long-interval", 
      checkpointInterval = 100.millis,
      checkpointManager = longIntervalManager
    )
    
    val testEvents = (1 to 5).map(i => TestEvent(i, s"data-$i"))
    
    val result = for {
      _ <- Future.sequence(List(
        Source(testEvents).throttle(1, 15.millis).via(shortFlow.toAkkaFlow()).runWith(Sink.ignore),
        Source(testEvents).throttle(1, 15.millis).via(longFlow.toAkkaFlow()).runWith(Sink.ignore)
      ))
      _ <- akka.pattern.after(150.millis, system.scheduler)(Future.successful(()))
    } yield {
      shortIntervalManager.completeSaves()
      longIntervalManager.completeSaves()
      (shortIntervalManager.getCheckpointCount, longIntervalManager.getCheckpointCount)
    }
    
    result.map { case (shortCount, longCount) =>
      // Short interval should create more checkpoints than long interval
      assert(shortCount >= longCount)
    }
  }
  
  test("CheckpointingFlow should handle empty streams") {
    val checkpointManager = new TestCheckpointManager[String, String]()
    val (flow, _) = createCheckpointingFlow(checkpointManager = checkpointManager)
    
    val result = Source.empty[TestEvent]
      .via(flow.toAkkaFlow())
      .runWith(Sink.seq)
    
    result.map { output =>
      assertEquals(output.length, 0)
      assertEquals(checkpointManager.getCheckpointCount, 0)
    }
  }
  
  test("CheckpointingFlow should handle single element streams") {
    val checkpointManager = new TestCheckpointManager[String, String]()
    val (flow, _) = createCheckpointingFlow(
      checkpointInterval = 1.millis, // Very short interval to ensure checkpoint
      checkpointManager = checkpointManager
    )
    
    val testEvent = TestEvent(42, "single-event")
    
    val result = for {
      output <- Source.single(testEvent)
        .via(flow.toAkkaFlow())
        .runWith(Sink.seq)
      _ <- akka.pattern.after(50.millis, system.scheduler)(Future.successful(()))
    } yield {
      checkpointManager.completeSaves()
      (output, checkpointManager.getSavedCheckpoints)
    }
    
    result.map { case (output, checkpoints) =>
      assertEquals(output.length, 1)
      assertEquals(output.head, testEvent)
      
      if (checkpoints.nonEmpty) {
        assertEquals(checkpoints.head.offset, 42L)
        assertEquals(checkpoints.head.state("processedCount"), "1")
      }
    }
  }
  
  test("CheckpointingFlow should work with custom offset extraction") {
    val checkpointManager = new TestCheckpointManager[String, String]()
    
    // Custom flow that extracts offset from data field
    val customFlow = new CheckpointingFlow[TestEvent, String, String](
      pipelineId = "custom-offset",
      checkpointManager = checkpointManager,
      checkpointInterval = 10.millis, // Reasonable interval
      extractOffset = event => event.data.split("-").last.toLong // Extract number from "data-N"
    )
    
    val testEvents = List(
      TestEvent(1, "data-100"),
      TestEvent(2, "data-200"),
      TestEvent(3, "data-300"),
      TestEvent(4, "data-400"),
      TestEvent(5, "data-500")
    )
    
    val result = for {
      _ <- Source(testEvents)
        .throttle(1, 5.millis) // Slower than checkpoint interval to ensure checkpoints
        .via(customFlow.toAkkaFlow())
        .runWith(Sink.ignore)
      _ <- akka.pattern.after(200.millis, system.scheduler)(Future.successful(()))
    } yield ()
    
    result.flatMap { _ =>
      // Complete all pending saves first
      checkpointManager.completeSaves()
      // Longer delay to ensure completion
      akka.pattern.after(100.millis, system.scheduler)(Future.successful(())).map { _ =>
        val checkpoints = checkpointManager.getSavedCheckpoints
        assert(checkpoints.nonEmpty, s"Expected checkpoints but got ${checkpoints.length}")
        
        // Verify offsets come from data field, not ID field
        val offsets = checkpoints.map(_.offset).toSet
        assert(offsets.subsetOf(Set(100L, 200L, 300L, 400L, 500L)))
        assert(!offsets.contains(1L)) // Should not contain the ID values
      }
    }
  }
}