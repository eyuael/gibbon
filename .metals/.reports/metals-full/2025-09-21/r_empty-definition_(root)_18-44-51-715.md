error id: file://<WORKSPACE>/src/main/scala/gibbon/operations/checkpoint/CheckpointMetadata.scala:Checkpoint
file://<WORKSPACE>/src/main/scala/gibbon/operations/checkpoint/CheckpointMetadata.scala
empty definition using pc, found symbol in pc: 
semanticdb not found

found definition using fallback; symbol Checkpoint
offset: 99
uri: file://<WORKSPACE>/src/main/scala/gibbon/operations/checkpoint/CheckpointMetadata.scala
text:
```scala
package gibbon.checkpoint

import gibbon.core.Event
import java.time.Instant

case class Checkpoint@@[+K, +V](
  pipelineId: String,
  offset: Long,
  timestamp: Instant,
  lastProcessedEvent: Option[Event[K, V]],
  state: Map[String, String] = Map.empty
)

case class CheckpointMetadata(
  pipelineId: String,
  version: Long,
  createdAt: Instant,
  checkpointType: CheckpointType
)

sealed trait CheckpointType
case object Periodic extends CheckpointType
case object OnFailure extends CheckpointType
case object Manual extends CheckpointType
```


#### Short summary: 

empty definition using pc, found symbol in pc: 