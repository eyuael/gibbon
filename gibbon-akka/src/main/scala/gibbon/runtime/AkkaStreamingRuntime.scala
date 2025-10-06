package gibbon.runtime

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Source => AkkaSource, Flow => AkkaFlow, Sink => AkkaSink, RunnableGraph => AkkaRunnableGraph}
import akka.NotUsed
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.Future

class AkkaStreamingRuntime extends StreamingRuntime {
  type ActorSystem = akka.actor.ActorSystem
  type Source[+Out, +Mat] = AkkaSource[Out, Mat]
  type Flow[-In, +Out, +Mat] = AkkaFlow[In, Out, Mat]
  type Sink[-In, +Mat] = AkkaSink[In, Mat]
  type RunnableGraph[+Mat] = AkkaRunnableGraph[Mat]
  type NotUsed = akka.NotUsed
  
  def createActorSystem(name: String): ActorSystem = 
    ActorSystem(name)
    
  def terminateActorSystem(system: ActorSystem): Future[Unit] = {
    implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global
    system.terminate().map(_ => ())
  }
    
  def emptySource[T]: Source[T, NotUsed] = 
    AkkaSource.empty[T]
    
  def fromIterator[T](iterator: () => Iterator[T]): Source[T, NotUsed] = 
    AkkaSource.fromIterator(iterator)
    
  def tick[T](initialDelay: FiniteDuration, interval: FiniteDuration, tick: T): Source[T, Any] = 
    AkkaSource.tick(initialDelay, interval, tick)

  def mapFlow[In, Out](f: In => Out): Flow[In, Out, NotUsed] = 
    AkkaFlow[In].map(f)
    
  def filterFlow[T](predicate: T => Boolean): Flow[T, T, NotUsed] = 
    AkkaFlow[T].filter(predicate)
    
  def dropWhileFlow[T](predicate: T => Boolean): Flow[T, T, NotUsed] = 
    AkkaFlow[T].dropWhile(predicate)

  // Batching support methods
  def groupedFlow[T](batchSize: Int): Flow[T, List[T], NotUsed] = 
    AkkaFlow[T].grouped(batchSize).map(_.toList)
    
  def groupedWithinFlow[T](batchSize: Int, timeout: FiniteDuration): Flow[T, List[T], NotUsed] = 
    AkkaFlow[T].groupedWithin(batchSize, timeout).map(_.toList)
    
  def batchFlow[T, S](seed: T => S)(aggregate: (S, T) => S): Flow[T, S, NotUsed] = 
    AkkaFlow[T].batch(1, seed)(aggregate)

  def foreachSink[T](f: T => Unit): Sink[T, Future[Unit]] = {
    implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global
    AkkaSink.foreach(f).mapMaterializedValue(_.map(_ => ()))
  }
    
  def seqSink[T]: Sink[T, Future[Seq[T]]] = 
    AkkaSink.seq[T]

  def runGraph[Mat](source: Source[Any, Any], sink: Sink[Any, Mat])(implicit system: ActorSystem): Mat = 
    source.runWith(sink)
    
  def createRunnableGraph[Mat](source: Source[Any, Any], sink: Sink[Any, Mat]): RunnableGraph[Mat] = 
    source.to(sink).asInstanceOf[RunnableGraph[Mat]]
}