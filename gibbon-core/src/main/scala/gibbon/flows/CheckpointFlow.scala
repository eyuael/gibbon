package gibbon.flows

import gibbon.core.Flow
import gibbon.checkpoint.{CheckpointManager, CheckpointMetadata, Checkpoint, Periodic}
import gibbon.runtime.StreamingRuntime
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.FiniteDuration
import java.time.Instant
import java.util.concurrent.atomic.AtomicLong

class CheckpointingFlow[T, K, V](
  pipelineId: String,
  checkpointManager: CheckpointManager[K, V],
  checkpointInterval: FiniteDuration,
  extractOffset: T => Long
)(implicit ec: ExecutionContext) extends Flow[T, T] {
  
  override def toRuntimeFlow[RT <: StreamingRuntime]()(implicit runtime: RT): runtime.Flow[T, T, runtime.NotUsed] = {
    // Simplified implementation - stateful operations would be implemented in runtime-specific modules
    runtime.mapFlow[T, T] { element =>
      // Create checkpoint asynchronously without blocking the stream
      createCheckpoint(element, extractOffset(element), Instant.now())
      element
    }
  }
  
  private def createCheckpoint(element: T, offset: Long, timestamp: Instant): Unit = {
    // Create a proper Checkpoint object instead of CheckpointMetadata
    val checkpoint = Checkpoint[K, V](
      pipelineId = pipelineId,
      offset = offset,
      timestamp = timestamp,
      lastProcessedEvent = None,
      state = Map("lastProcessedOffset" -> offset.toString)
    )
    
    // Fire and forget - don't block the stream but log errors
    checkpointManager.saveCheckpoint(checkpoint).onComplete {
      case scala.util.Success(_) => 
        // Checkpoint saved successfully
      case scala.util.Failure(ex) => 
        println(s"Failed to save checkpoint: ${ex.getMessage}")
    }
  }
}