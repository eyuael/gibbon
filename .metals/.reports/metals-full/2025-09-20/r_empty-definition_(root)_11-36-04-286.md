error id: file://<WORKSPACE>/src/main/scala/gibbon/core/Pipeline.scala:
file://<WORKSPACE>/src/main/scala/gibbon/core/Pipeline.scala
empty definition using pc, found symbol in pc: 
empty definition using semanticdb
empty definition using fallback
non-local guesses:
	 -Source.
	 -Source#
	 -Source().
	 -scala/Predef.Source.
	 -scala/Predef.Source#
	 -scala/Predef.Source().
offset: 33
uri: file://<WORKSPACE>/src/main/scala/gibbon/core/Pipeline.scala
text:
```scala
package gibbon.core
import Source@@
import Sink

final case class Pipeline[I,O](
    source: Source[I],
    flow: Flow[I,O],
    sink: Sink[O]
) {
  def toRunnableGraph(): akka.stream.scaladsl.RunnableGraph[_] = {
    val base = source.toAkkaSource()
    val withFlows = base.via(flow.toAkkaFlow())
    withFlows.to(sink.toAkkaSink())
  }
}

```


#### Short summary: 

empty definition using pc, found symbol in pc: 