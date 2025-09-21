package gibbon.checkpoint

import gibbon.core.Event
import java.time.Instant

case class Checkpoint[+K, +V](
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