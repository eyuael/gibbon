error id: file://<WORKSPACE>/src/main/scala/gibbon/Sink.scala:gibbon/Sink#toAkkaSink().
file://<WORKSPACE>/src/main/scala/gibbon/Sink.scala
empty definition using pc, found symbol in pc: 
found definition using semanticdb; symbol gibbon/Sink#toAkkaSink().
empty definition using fallback
non-local guesses:

offset: 48
uri: file://<WORKSPACE>/src/main/scala/gibbon/Sink.scala
text:
```scala
package gibbon

trait Sink[E] {
  def toAkkaSink@@(): akka.stream.scaladsl.Sink[E, _]
}

case object ConsoleSink extends Sink[Event[_,_]] {
  def toAkkaSink() = akka.stream.scaladsl.Sink.foreach(println)
}

```


#### Short summary: 

empty definition using pc, found symbol in pc: 