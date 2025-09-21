file://<WORKSPACE>/src/main/scala/gibbon/operations/checkpoint/CheckpointManager.scala
empty definition using pc, found symbol in pc: 
semanticdb not found
empty definition using fallback
non-local guesses:
	 -scalar.
	 -scalar#
	 -scalar().
	 -scala/Predef.scalar.
	 -scala/Predef.scalar#
	 -scala/Predef.scalar().
offset: 40
uri: file://<WORKSPACE>/src/main/scala/gibbon/operations/checkpoint/CheckpointManager.scala
text:
```scala
package gibbon.checkpoint

import scalar@@
import scala.concurrent.{Future, ExecutionContext}
import scala.concurrent.duration.FiniteDuration
import java.time.Instant

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
      .map(_.flatMap(decode[Checkpoint[K, V]](_).toOption))
  }
  
  // ... other methods
}
```


#### Short summary: 

empty definition using pc, found symbol in pc: 