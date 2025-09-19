error id: file://<WORKSPACE>/src/main/scala/gibbon/Pipeline.scala:
file://<WORKSPACE>/src/main/scala/gibbon/Pipeline.scala
empty definition using pc, found symbol in pc: 
empty definition using semanticdb
empty definition using fallback
non-local guesses:
	 -toRunn.
	 -toRunn#
	 -toRunn().
	 -scala/Predef.toRunn.
	 -scala/Predef.toRunn#
	 -scala/Predef.toRunn().
offset: 126
uri: file://<WORKSPACE>/src/main/scala/gibbon/Pipeline.scala
text:
```scala
package gibbon

final case class Pipeline[I,O](
    source: Source[I],
    flow: Flow[I,O],
    sink: Sink[O]
) {
  def toRunn@@
}

```


#### Short summary: 

empty definition using pc, found symbol in pc: 