package gibbon.runtime

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.scaladsl.{Source => PekkoSource, Flow => PekkoFlow, Sink => PekkoSink, RunnableGraph => PekkoRunnableGraph}
import org.apache.pekko.NotUsed
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

class PekkoStreamingRuntime extends StreamingRuntime {
  type ActorSystem = org.apache.pekko.actor.ActorSystem
  type Source[+Out, +Mat] = PekkoSource[Out, Mat]
  type Flow[-In, +Out, +Mat] = PekkoFlow[In, Out, Mat]
  type Sink[-In, +Mat] = PekkoSink[In, Mat]
  type RunnableGraph[+Mat] = PekkoRunnableGraph[Mat]
  type NotUsed = org.apache.pekko.NotUsed
  
  private def pekkoFlow[T] = PekkoFlow[T]
  private def pekkoSink = PekkoSink
  
  def createActorSystem(name: String): ActorSystem = 
    ActorSystem(name)
  
  def terminateActorSystem(system: ActorSystem): Future[Unit] = {
    implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global
    system.terminate().map(_ => ())
  }

  def emptySource[T]: Source[T, NotUsed] = 
    PekkoSource.empty[T]
  
  def fromIterator[T](iterator: () => Iterator[T]): Source[T, NotUsed] = 
    PekkoSource.fromIterator(iterator)
  
  def tick[T](initialDelay: FiniteDuration, interval: FiniteDuration, tick: T): Source[T, Any] = 
    PekkoSource.tick(initialDelay, interval, tick)

  def mapFlow[In, Out](f: In => Out): Flow[In, Out, NotUsed] = 
    pekkoFlow[In].map(f)
    
  def filterFlow[T](predicate: T => Boolean): Flow[T, T, NotUsed] = 
    pekkoFlow[T].filter(predicate)
    
  def dropWhileFlow[T](predicate: T => Boolean): Flow[T, T, NotUsed] = 
    pekkoFlow[T].dropWhile(predicate)

  def foreachSink[T](f: T => Unit): Sink[T, Future[Unit]] = {
    implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global
    pekkoSink.foreach(f).mapMaterializedValue(_.map(_ => ()))
  }
    
  def seqSink[T]: Sink[T, Future[Seq[T]]] = 
    pekkoSink.seq[T]

  def runGraph[Mat](source: Source[Any, Any], sink: Sink[Any, Mat])(implicit system: ActorSystem): Mat = 
    source.runWith(sink)
}