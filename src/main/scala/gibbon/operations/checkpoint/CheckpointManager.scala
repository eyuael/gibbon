package gibbon.checkpoint

import scala.concurrent.{Future, ExecutionContext}
import scala.concurrent.duration.FiniteDuration
import java.time.Instant
import io.circe._
import io.circe.syntax._
import io.circe.parser._
import io.circe.generic.auto._
import redis.RedisClient

trait CheckpointManager[K, V] {
  def saveCheckpoint(checkpoint: Checkpoint[K, V]): Future[Unit]
  def loadCheckpoint(pipelineId: String): Future[Option[Checkpoint[K, V]]]
  def listCheckpoints(pipelineId: String): Future[List[CheckpointMetadata]]
  def deleteCheckpoint(pipelineId: String, version: Long): Future[Unit]
}

class RedisCheckpointManager[K: Encoder: Decoder, V: Encoder: Decoder](
  redis: RedisClient,
  keyPrefix: String = "gibbon:checkpoint"
)(implicit ec: ExecutionContext) extends CheckpointManager[K, V] {
  
  private def checkpointKey(pipelineId: String): String = 
    s"$keyPrefix:$pipelineId:latest"
  
  private def checkpointHistoryKey(pipelineId: String): String = 
    s"$keyPrefix:$pipelineId:history"
  
  override def saveCheckpoint(checkpoint: Checkpoint[K, V]): Future[Unit] = {
    val key = checkpointKey(checkpoint.pipelineId)
    val historyKey = checkpointHistoryKey(checkpoint.pipelineId)
    
    for {
      _ <- redis.set(key, checkpoint.asJson.noSpaces)
      _ <- redis.lpush(historyKey, checkpoint.asJson.noSpaces)
      _ <- redis.ltrim(historyKey, 0, 99) // Keep last 100 checkpoints
    } yield ()
  }
  
  override def loadCheckpoint(pipelineId: String): Future[Option[Checkpoint[K, V]]] = {
    redis.get(checkpointKey(pipelineId))
      .map(_.flatMap(byteString => decode[Checkpoint[K, V]](byteString.utf8String).toOption))
  }
  
  override def listCheckpoints(pipelineId: String): Future[List[CheckpointMetadata]] = {
    val historyKey = checkpointHistoryKey(pipelineId)
    redis.lrange(historyKey, 0, -1).map { checkpointStrings =>
      checkpointStrings.flatMap { checkpointBytes =>
        decode[Checkpoint[K, V]](checkpointBytes.utf8String).toOption.map { checkpoint =>
          CheckpointMetadata(
            pipelineId = checkpoint.pipelineId,
            version = checkpoint.offset,
            createdAt = checkpoint.timestamp,
            checkpointType = Periodic // Default to Periodic since it's not stored in Checkpoint
          )
        }
      }.toList
    }
  }
  
  override def deleteCheckpoint(pipelineId: String, version: Long): Future[Unit] = {
    val key = checkpointKey(pipelineId)
    val historyKey = checkpointHistoryKey(pipelineId)
    
    for {
      _ <- redis.del(key)
      _ <- redis.del(historyKey)
    } yield ()
  }
}