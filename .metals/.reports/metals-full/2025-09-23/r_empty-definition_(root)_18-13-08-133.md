file://<WORKSPACE>/src/main/scala/gibbon/runtime/PekkoStreamingRuntime.scala
empty definition using pc, found symbol in pc: 
semanticdb not found
empty definition using fallback
non-local guesses:
	 -system.
	 -system#
	 -system().
	 -scala/Predef.system.
	 -scala/Predef.system#
	 -scala/Predef.system().
offset: 643
uri: file://<WORKSPACE>/src/main/scala/gibbon/runtime/PekkoStreamingRuntime.scala
text:
```scala
package gibbon.runtime

import org.apache.pekko.{actor => pekko}
import org.apache.pekko.stream.scaladsl.{Source => PekkoSource, Flow => PekkoFlow, Sink => PekkoSink}
import org.apache.pekko.{NotUsed => PekkoNotUsed}
import scala.concurrent.Future

class PekkoStreamingRuntime extends StreamingRuntime {
  type ActorSystem = pekko.ActorSystem
  type Source[+Out, +Mat] = PekkoSource[Out, Mat]
  type Flow[-In, +Out, +Mat] = PekkoFlow[In, Out, Mat]
  type Sink[-In, +Mat] = PekkoSink[In, Mat]
  type NotUsed = PekkoNotUsed
  
  def createActorSystem(name: String): ActorSystem = 
    pekko.ActorSystem(name)
  
  def terminateActorSystem(system@@: ActorSystem): Future[Unit] = 
    system.terminate().map(_ => ())
}
```


#### Short summary: 

empty definition using pc, found symbol in pc: 