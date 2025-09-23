file://<WORKSPACE>/src/main/scala/gibbon/runtime/PekkoStreamingRuntime.scala
empty definition using pc, found symbol in pc: 
semanticdb not found
empty definition using fallback
non-local guesses:
	 -pek.
	 -pek#
	 -pek().
	 -scala/Predef.pek.
	 -scala/Predef.pek#
	 -scala/Predef.pek().
offset: 347
uri: file://<WORKSPACE>/src/main/scala/gibbon/runtime/PekkoStreamingRuntime.scala
text:
```scala
package gibbon.runtime

import org.apache.pekko.{actor => pekko}
import org.apache.pekko.stream.scaladsl.{Source => PekkoSource, Flow => PekkoFlow, Sink => PekkoSink}
import org.apache.pekko.{NotUsed => PekkoNotUsed}
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration
import pek@@

class PekkoStreamingRuntime extends StreamingRuntime {
  type ActorSystem = pekko.ActorSystem
  type Source[+Out, +Mat] = PekkoSource[Out, Mat]
  type Flow[-In, +Out, +Mat] = PekkoFlow[In, Out, Mat]
  type Sink[-In, +Mat] = PekkoSink[In, Mat]
  type NotUsed = PekkoNotUsed
  
  def createActorSystem(name: String): ActorSystem = 
    pekko.ActorSystem(name)
  
  def terminateActorSystem(system: ActorSystem)(implicit ec: ExecutionContext): Future[Unit] = 
    system.terminate().map(_ => ())

  def emptySource[T]: Source[T, NotUsed] = 
    PekkoSource.empty[T]
  
  def fromIterator[T](iterator: () => Iterator[T])(implicit ec: ExecutionContext): Source[T, NotUsed] = 
    PekkoSource.fromIterator(iterator)
  
  def tick[T](initialDelay: FiniteDuration, interval: FiniteDuration, tick: T)(implicit ec: ExecutionContext): Source[T, NotUsed] = 
    PekkoSource.tick(initialDelay, interval, tick).mapMaterializedValue(_ => pekko.Actor.Cancellable)
}
```


#### Short summary: 

empty definition using pc, found symbol in pc: 