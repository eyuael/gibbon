error id: file://<WORKSPACE>/src/main/scala/gibbon/core/Source.scala:
file://<WORKSPACE>/src/main/scala/gibbon/core/Source.scala
empty definition using pc, found symbol in pc: 
empty definition using semanticdb
empty definition using fallback
non-local guesses:
	 -i.
	 -i#
	 -i().
	 -scala/Predef.i.
	 -scala/Predef.i#
	 -scala/Predef.i().
offset: 21
uri: file://<WORKSPACE>/src/main/scala/gibbon/core/Source.scala
text:
```scala
package gibbon.core
i@@

trait Source[E] {
  def toAkkaSource(): akka.stream.scaladsl.Source[E, Any]
}

```


#### Short summary: 

empty definition using pc, found symbol in pc: 