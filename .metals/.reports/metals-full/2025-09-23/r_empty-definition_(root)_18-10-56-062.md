error id: file://<WORKSPACE>/src/main/scala/gibbon/runtime/AkkaStreamingRuntime.scala:
file://<WORKSPACE>/src/main/scala/gibbon/runtime/AkkaStreamingRuntime.scala
empty definition using pc, found symbol in pc: 
empty definition using semanticdb
empty definition using fallback
non-local guesses:
	 -akka/actor/stream.
	 -akka/stream.
	 -scala/Predef.akka.stream.
offset: 67
uri: file://<WORKSPACE>/src/main/scala/gibbon/runtime/AkkaStreamingRuntime.scala
text:
```scala
package gibbon.runtime

import akka.{actor => akka}
import akka.str@@eam.scaladsl.{Source => AkkaSource, Flow => AkkaFlow, Sink => AkkaSink}
import akka.{NotUsed => AkkaNotUsed}
import scala.concurrent.duration.FiniteDuration

class AkkaStreamingRuntime extends StreamingRuntime {
  type ActorSystem = akka.ActorSystem
  type Source[+Out, +Mat] = AkkaSource[Out, Mat]
  type Flow[-In, +Out, +Mat] = AkkaFlow[In, Out, Mat]
  type Sink[-In, +Mat] = AkkaSink[In, Mat]
  type NotUsed = AkkaNotUsed
  
  def createActorSystem(name: String): ActorSystem = 
    akka.ActorSystem(name)
    
  def terminateActorSystem(system: ActorSystem): Future[Unit] = 
    system.terminate().map(_ => ())
    
  def emptySource[T]: Source[T, NotUsed] = 
    AkkaSource.empty[T]
    
  def fromIterator[T](iterator: () => Iterator[T]): Source[T, NotUsed] = 
    AkkaSource.fromIterator(iterator)
    
  // ... implement all other methods
}
```


#### Short summary: 

empty definition using pc, found symbol in pc: 