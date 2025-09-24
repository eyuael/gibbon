package gibbon.core
import gibbon.core.Source
import gibbon.core.Flow
import gibbon.core.Sink
import gibbon.runtime.StreamingRuntime

final case class Pipeline[I,O](
  source: Source[I],
  flow: Flow[I,O],
  sink: Sink[O]
) {
  def toRunnableGraph[R <: StreamingRuntime]()(implicit runtime: R): runtime.RunnableGraph[Any] = {
    val runtimeSource = source.toRuntimeSource()
    val runtimeSink = sink.toRuntimeSink()
    
    // Use the runtime's createRunnableGraph method to connect source and sink
    runtime.createRunnableGraph(
      runtimeSource.asInstanceOf[runtime.Source[Any, Any]], 
      runtimeSink.asInstanceOf[runtime.Sink[Any, Any]]
    ).asInstanceOf[runtime.RunnableGraph[Any]]
  }
}
