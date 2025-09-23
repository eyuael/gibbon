error id: file://<WORKSPACE>/src/main/scala/gibbon/runtime/StreamingRuntime.scala:gibbon/runtime/StreamingRuntime#NotUsed#
file://<WORKSPACE>/src/main/scala/gibbon/runtime/StreamingRuntime.scala
empty definition using pc, found symbol in pc: 
found definition using semanticdb; symbol gibbon/runtime/StreamingRuntime#NotUsed#
empty definition using fallback
non-local guesses:

offset: 286
uri: file://<WORKSPACE>/src/main/scala/gibbon/runtime/StreamingRuntime.scala
text:
```scala
package gibbon.runtime
import scala.concurrent.{Future, ExecutionContext}
import scala.concurrent.duration.FiniteDuration

trait StreamingRuntime {
  type ActorSystem
  type Source[+Out, +Mat]
  type Flow[-In, +Out, +Mat]
  type Sink[-In, +Mat]
  type RunnableGraph[+Mat]
  type NotUsed@@
}


```


#### Short summary: 

empty definition using pc, found symbol in pc: 