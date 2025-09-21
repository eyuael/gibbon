file://<WORKSPACE>/src/main/scala/gibbon/core/RecoverablePipeline.scala
empty definition using pc, found symbol in pc: 
semanticdb not found
empty definition using fallback
non-local guesses:
	 -akka/a.
	 -akka/a#
	 -akka/a().
	 -a.
	 -a#
	 -a().
	 -scala/Predef.a.
	 -scala/Predef.a#
	 -scala/Predef.a().
offset: 193
uri: file://<WORKSPACE>/src/main/scala/gibbon/core/RecoverablePipeline.scala
text:
```scala
package gibbon.core

import gibbon.checkpoint.{CheckpointManager, Checkpoint}
import akka.stream.scaladsl.{Source => AkkaSource}
import scala.concurrent.{Future, ExecutionContext}
import akka.a@@

class RecoverablePipeline[I, O](
  pipelineId: String,
  source: Source[I],
  flow: Flow[I, O],
  sink: Sink[O],
  checkpointManager: CheckpointManager[_, _]
)(implicit ec: ExecutionContext) {
  
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
  private def runFromCheckpoint(checkpoint: Checkpoint[_, _]): Future[Any] = {
    // Skip to the checkpoint offset
    val recoveredSource = source.toAkkaSource()
      .drop(checkpoint.offset) // Simplified - real implementation would be more sophisticated
    
    recoveredSource
      .via(flow.toAkkaFlow())
      .runWith(sink.toAkkaSink())
  }
  
  private def runFromBeginning(): Future[Any] = {
    source.toAkkaSource()
      .via(flow.toAkkaFlow())
      .runWith(sink.toAkkaSink())
  }
}
```


#### Short summary: 

empty definition using pc, found symbol in pc: 