error id: file://<WORKSPACE>/src/main/scala/gibbon/core/Pipeline.scala:akka/stream/scaladsl/Source#via().
file://<WORKSPACE>/src/main/scala/gibbon/core/Pipeline.scala
empty definition using pc, found symbol in pc: 
found definition using semanticdb; symbol akka/stream/scaladsl/Source#via().
empty definition using fallback
non-local guesses:

offset: 391
uri: file://<WORKSPACE>/src/main/scala/gibbon/core/Pipeline.scala
text:
```scala
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
    val base = source.toRuntimeSource()
    val withFlows = base.@@via(flow.toRuntimeFlow())
    withFlows.to(sink.toRuntimeSink())
  }
}

```


#### Short summary: 

empty definition using pc, found symbol in pc: 