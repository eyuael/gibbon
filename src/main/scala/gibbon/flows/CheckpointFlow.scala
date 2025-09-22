package gibbon.flows

import gibbon.core.Flow
import gibbon.checkpoint.{CheckpointManager, Checkpoint}
import akka.stream.scaladsl.{Flow => AkkaFlow}
import scala.concurrent.{ExecutionContext, Future}
import akka.actor.ActorSystem
import scala.concurrent.duration.FiniteDuration
import java.time.Instant
import java.util.concurrent.atomic.AtomicLong

class CheckpointingFlow[T, K, V](
  pipelineId: String,
  checkpointManager: CheckpointManager[K, V],
  checkpointInterval: FiniteDuration,
  extractOffset: T => Long
)(implicit ec: ExecutionContext) extends Flow[T, T] {
  
  private val processedCount = new AtomicLong(0)
  private var lastCheckpointTime = Instant.now()
  
  override def toAkkaFlow(): AkkaFlow[T, T, Any] = {
    AkkaFlow[T]
      .map { element =>
        val currentCount = processedCount.incrementAndGet()
        val now = Instant.now()
        
        if (shouldCheckpoint(now, currentCount)) {
          createCheckpoint(element, currentCount, now)
          lastCheckpointTime = now
        }
        
        element
      }
  }
  
  private def shouldCheckpoint(now: Instant, count: Long): Boolean = {
    val timeSinceLastCheckpoint = java.time.Duration.between(lastCheckpointTime, now)
    timeSinceLastCheckpoint.compareTo(java.time.Duration.ofMillis(checkpointInterval.toMillis)) >= 0
  }
  
  private def createCheckpoint(element: T, count: Long, timestamp: Instant): Unit = {
    // Async checkpoint creation - don't block the stream
    val checkpoint = Checkpoint[K, V](
      pipelineId = pipelineId,
      offset = extractOffset(element),
      timestamp = timestamp,
      lastProcessedEvent = None, // Could be enhanced to store the actual event
      state = Map("processedCount" -> count.toString)
    )
    
    checkpointManager.saveCheckpoint(checkpoint)
      .recover { case ex => 
        // Log error but don't fail the stream
        println(s"Failed to save checkpoint: ${ex.getMessage}")
      }
  }
}