package gibbon.checkpoint

import scala.concurrent.{Future, ExecutionContext}
import scala.concurrent.duration.FiniteDuration
import java.time.Instant
import io.circe._
import io.circe.syntax._
import io.circe.parser._
import io.circe.generic.auto._

trait CheckpointManager[K, V] {
  def saveCheckpoint(checkpoint: Checkpoint[K, V]): Future[Unit]
  def loadCheckpoint(pipelineId: String): Future[Option[Checkpoint[K, V]]]
  def listCheckpoints(pipelineId: String): Future[List[CheckpointMetadata]]
  def deleteCheckpoint(pipelineId: String, version: Long): Future[Unit]
}

// Simplified in-memory implementation - Redis functionality would be in runtime-specific modules
class RedisCheckpointManager[K: Encoder: Decoder, V: Encoder: Decoder](
  connectionString: String,
  keyPrefix: String = "gibbon:checkpoint"
)(implicit ec: ExecutionContext) extends CheckpointManager[K, V] {
  
  private val store = scala.collection.mutable.Map[String, String]()
  private val history = scala.collection.mutable.Map[String, List[String]]()
  
  private def checkpointKey(pipelineId: String): String = 
    s"$keyPrefix:$pipelineId:latest"
  
  private def checkpointHistoryKey(pipelineId: String): String = 
    s"$keyPrefix:$pipelineId:history"
  
  override def saveCheckpoint(checkpoint: Checkpoint[K, V]): Future[Unit] = {
    val key = checkpointKey(checkpoint.pipelineId)
    val historyKey = checkpointHistoryKey(checkpoint.pipelineId)
    val checkpointJson = checkpoint.asJson.noSpaces
    
    store.put(key, checkpointJson)
    val currentHistory = history.getOrElse(historyKey, List.empty)
    val updatedHistory = (checkpointJson :: currentHistory).take(100) // Keep last 100 checkpoints
    history.put(historyKey, updatedHistory)
    
    Future.successful(())
  }
  
  override def loadCheckpoint(pipelineId: String): Future[Option[Checkpoint[K, V]]] = {
    val result = store.get(checkpointKey(pipelineId))
      .flatMap(jsonStr => decode[Checkpoint[K, V]](jsonStr).toOption)
    Future.successful(result)
  }
  
  override def listCheckpoints(pipelineId: String): Future[List[CheckpointMetadata]] = {
    val historyKey = checkpointHistoryKey(pipelineId)
    val checkpointStrings = history.getOrElse(historyKey, List.empty)
    val metadata = checkpointStrings.flatMap { checkpointStr =>
      decode[Checkpoint[K, V]](checkpointStr).toOption.map { checkpoint =>
        CheckpointMetadata(
          pipelineId = checkpoint.pipelineId,
          version = checkpoint.offset,
          createdAt = checkpoint.timestamp,
          checkpointType = Periodic // Default to Periodic since it's not stored in Checkpoint
        )
      }
    }
    Future.successful(metadata)
  }
  
  override def deleteCheckpoint(pipelineId: String, version: Long): Future[Unit] = {
    val key = checkpointKey(pipelineId)
    val historyKey = checkpointHistoryKey(pipelineId)
    
    store.remove(key)
    history.remove(historyKey)
    
    Future.successful(())
  }
}