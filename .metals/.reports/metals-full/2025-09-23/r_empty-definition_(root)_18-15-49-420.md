error id: file://<WORKSPACE>/src/main/scala/gibbon/core/Pipeline.scala:gibbon/core/Source#toAkkaSource().
file://<WORKSPACE>/src/main/scala/gibbon/core/Pipeline.scala
empty definition using pc, found symbol in pc: 
empty definition using semanticdb
empty definition using fallback
non-local guesses:
	 -source/toAkkaSource.
	 -source/toAkkaSource#
	 -source/toAkkaSource().
	 -scala/Predef.source.toAkkaSource.
	 -scala/Predef.source.toAkkaSource#
	 -scala/Predef.source.toAkkaSource().
offset: 278
uri: file://<WORKSPACE>/src/main/scala/gibbon/core/Pipeline.scala
text:
```scala
package gibbon.core
import gibbon.core.Source
import gibbon.core.Flow
import gibbon.core.Sink

final case class Pipeline[I,O](
  source: Source[I],
  flow: Flow[I,O],
  sink: Sink[O]
) {
  def toRunnableGraph(): akka.stream.scaladsl.RunnableGraph[Any] = {
    val base = source.@@toAkkaSource()
    val withFlows = base.via(flow.toAkkaFlow())
    withFlows.to(sink.toAkkaSink())
  }
}

```


#### Short summary: 

empty definition using pc, found symbol in pc: 