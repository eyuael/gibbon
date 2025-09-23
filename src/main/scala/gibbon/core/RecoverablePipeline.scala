package gibbon.core

import gibbon.checkpoint.{CheckpointManager, Checkpoint}
import akka.stream.scaladsl.{Source => AkkaSource}
import scala.concurrent.{Future, ExecutionContext}
import akka.actor.ActorSystem
import gibbon.runtime.StreamingRuntime

class RecoverablePipeline[I, O, K, V](
  pipelineId: String,
  source: Source[I],
  flow: Flow[I, O],
  sink: Sink[O],
  checkpointManager: CheckpointManager[K, V],
  extractOffset: I => Long = (element: I) => element.asInstanceOf[{def id: Long}].id // Default assumes element has id field
)(implicit ec: ExecutionContext, system: ActorSystem) {
  
  def runWithRecovery(): Future[Any] = {
    for {
      checkpoint <- checkpointManager.loadCheckpoint(pipelineId)
      _ <- checkpoint match {
        case Some(cp) => 
          println(s"Recovering from checkpoint at offset ${cp.offset}")
          runFromCheckpoint(cp)
        case None => 
          println("No checkpoint found, starting from beginning")
          runFromBeginning()
      }
    } yield ()
  }
  private def runFromCheckpoint(checkpoint: Checkpoint[K, V]): Future[Any] = {
    // Skip to the checkpoint offset
    val recoveredSource = source.toAkkaSource()
      .dropWhile { element => 
        val elementOffset = extractOffset(element)
        elementOffset <= checkpoint.offset
      }
    
    val flowResult = recoveredSource
      .via(flow.toAkkaFlow())
    
    flowResult.runWith(sink.toAkkaSink())
  }
  
  private def runFromBeginning(): Future[Any] = {
    source.toAkkaSource()
      .via(flow.toAkkaFlow())
      .runWith(sink.toAkkaSink())
  }
}