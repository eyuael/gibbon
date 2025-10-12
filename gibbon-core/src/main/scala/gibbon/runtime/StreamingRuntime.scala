package gibbon.runtime
import scala.concurrent.{Future, ExecutionContext}
import scala.concurrent.duration.FiniteDuration

trait StreamingRuntime {
  type ActorSystem
  type Source[+Out, +Mat]
  type Flow[-In, +Out, +Mat]
  type Sink[-In, +Mat]
  type RunnableGraph[+Mat]
  type NotUsed

  def createActorSystem(name: String): ActorSystem  
  def terminateActorSystem(system: ActorSystem): Future[Unit]

  def emptySource[T]: Source[T, NotUsed]
  def fromIterator[T](iterator: () => Iterator[T]): Source[T, NotUsed]
  def tick[T](initialDelay: FiniteDuration, interval: FiniteDuration, tick: T): Source[T, Any]

  def mapFlow[In, Out](f: In => Out): Flow[In, Out, NotUsed]
  def filterFlow[T](predicate: T => Boolean): Flow[T, T, NotUsed]
  def dropWhileFlow[T](predicate: T => Boolean): Flow[T, T, NotUsed]

  def foreachSink[T](f: T => Unit): Sink[T, Future[Unit]]
  def seqSink[T]: Sink[T, Future[Seq[T]]]

  // Batching support methods
  def groupedFlow[T](batchSize: Int): Flow[T, List[T], NotUsed]
  def groupedWithinFlow[T](batchSize: Int, timeout: FiniteDuration): Flow[T, List[T], NotUsed]
  def batchFlow[T, S](seed: T => S)(aggregate: (S, T) => S): Flow[T, S, NotUsed]

  def runGraph[Mat](source: Source[Any, Any], sink: Sink[Any, Mat])(implicit system: ActorSystem): Mat
  
  // Helper method to create a RunnableGraph from source and sink
  def createRunnableGraph[Mat](source: Source[Any, Any], sink: Sink[Any, Mat]): RunnableGraph[Mat]

  def mapAsyncFlow[In, Out](parallelism: Int)(f: In => Future[Out]): Flow[In, Out, NotUsed]
}

