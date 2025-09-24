package gibbon.runtime

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.scaladsl.{Source => PekkoSource, Flow => PekkoFlow, Sink => PekkoSink, RunnableGraph => PekkoRunnableGraph}
import org.apache.pekko.NotUsed
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

class PekkoStreamingRuntime extends StreamingRuntime {
  type ActorSystem = org.apache.pekko.actor.ActorSystem
  type Source[+Out, +Mat] = org.apache.pekko.stream.scaladsl.Source[Out, Mat]
  type Flow[-In, +Out, +Mat] = org.apache.pekko.stream.scaladsl.Flow[In, Out, Mat]
  type Sink[-In, +Mat] = org.apache.pekko.stream.scaladsl.Sink[In, Mat]
  type RunnableGraph[+Mat] = org.apache.pekko.stream.scaladsl.RunnableGraph[Mat]
  type NotUsed = org.apache.pekko.NotUsed
  
  def createActorSystem(name: String): ActorSystem = 
    org.apache.pekko.actor.ActorSystem(name)
  
  def terminateActorSystem(system: ActorSystem): Future[Unit] = {
    implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global
    system.terminate().map(_ => ())
  }

  def emptySource[T]: Source[T, NotUsed] = 
    org.apache.pekko.stream.scaladsl.Source.empty[T]
  
  def fromIterator[T](iterator: () => Iterator[T]): Source[T, NotUsed] = 
    org.apache.pekko.stream.scaladsl.Source.fromIterator(iterator)
  
  def tick[T](initialDelay: FiniteDuration, interval: FiniteDuration, tick: T): Source[T, Any] = 
    org.apache.pekko.stream.scaladsl.Source.tick(initialDelay, interval, tick)

  def mapFlow[In, Out](f: In => Out): Flow[In, Out, NotUsed] = 
    org.apache.pekko.stream.scaladsl.Flow[In].map(f)
    
  def filterFlow[T](predicate: T => Boolean): Flow[T, T, NotUsed] = 
    org.apache.pekko.stream.scaladsl.Flow[T].filter(predicate)
    
  def dropWhileFlow[T](predicate: T => Boolean): Flow[T, T, NotUsed] = 
    org.apache.pekko.stream.scaladsl.Flow[T].dropWhile(predicate)

  def foreachSink[T](f: T => Unit): Sink[T, Future[Unit]] = {
    implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global
    org.apache.pekko.stream.scaladsl.Sink.foreach(f).mapMaterializedValue(_.map(_ => ()))
  }
    
  def seqSink[T]: Sink[T, Future[Seq[T]]] = 
    org.apache.pekko.stream.scaladsl.Sink.seq[T]

  def runGraph[Mat](source: Source[Any, Any], sink: Sink[Any, Mat])(implicit system: ActorSystem): Mat = 
    source.runWith(sink)
    
  def createRunnableGraph[Mat](source: Source[Any, Any], sink: Sink[Any, Mat]): RunnableGraph[Mat] = 
    source.to(sink).asInstanceOf[RunnableGraph[Mat]]
}