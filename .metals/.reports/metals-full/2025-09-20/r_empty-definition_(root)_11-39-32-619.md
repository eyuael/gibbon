error id: file://<WORKSPACE>/src/main/scala/gibbon/core/Sink.scala:
file://<WORKSPACE>/src/main/scala/gibbon/core/Sink.scala
empty definition using pc, found symbol in pc: 
empty definition using semanticdb
empty definition using fallback
non-local guesses:
	 -core.
	 -scala/Predef.core.
offset: 28
uri: file://<WORKSPACE>/src/main/scala/gibbon/core/Sink.scala
text:
```scala
package gibbon.core
import c@@ore.Event

trait Sink[E] {
  def toAkkaSink(): akka.stream.scaladsl.Sink[E, _]
}

case object ConsoleSink extends Sink[Event[_,_]] {
  def toAkkaSink() = akka.stream.scaladsl.Sink.foreach(println)
}

```


#### Short summary: 

empty definition using pc, found symbol in pc: 