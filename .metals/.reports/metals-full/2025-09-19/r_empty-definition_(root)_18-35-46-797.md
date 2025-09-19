error id: file://<WORKSPACE>/src/main/scala/gibbon/Sink.scala:
file://<WORKSPACE>/src/main/scala/gibbon/Sink.scala
empty definition using pc, found symbol in pc: 
empty definition using semanticdb
empty definition using fallback
non-local guesses:
	 -I#
	 -scala/Predef.I#
offset: 28
uri: file://<WORKSPACE>/src/main/scala/gibbon/Sink.scala
text:
```scala
package gibbon

trait Sink[I@@] {
  def toAkkaSink(): akka.stream.scaladsl.Sink[I, _]
}

```


#### Short summary: 

empty definition using pc, found symbol in pc: 