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
  
  override def toAkkaFlow(): AkkaFlow[T, T, Any] = {
    AkkaFlow[T]
      .statefulMapConcat { () =>
        var processedCount = 0L
        var lastCheckpointNanos: Option[Long] = None
        
        element => {
          processedCount += 1
          val currentNanos = System.nanoTime()
          
          val shouldCreateCheckpoint = lastCheckpointNanos match {
            case None => 
              // First element - initialize timing and create first checkpoint
              lastCheckpointNanos = Some(currentNanos)
              true
            case Some(lastNanos) =>
              // Check if interval has passed for subsequent checkpoints
              val nanosSinceLastCheckpoint = currentNanos - lastNanos
              val intervalNanos = checkpointInterval.toNanos
              if (nanosSinceLastCheckpoint >= intervalNanos) {
                lastCheckpointNanos = Some(currentNanos)
                true
              } else {
                false
              }
          }
          
          if (shouldCreateCheckpoint) {
            createCheckpoint(element, processedCount, Instant.now())
          }
          
          List(element)
        }
      }
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
    
    // Fire and forget - don't block the stream but log errors
    checkpointManager.saveCheckpoint(checkpoint).onComplete {
      case scala.util.Success(_) => 
        // Checkpoint saved successfully
      case scala.util.Failure(ex) => 
        // Log error but don't fail the stream
        println(s"Failed to save checkpoint: ${ex.getMessage}")
    }
  }
}