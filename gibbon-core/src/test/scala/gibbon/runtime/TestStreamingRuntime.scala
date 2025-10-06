package gibbon.runtime

import scala.concurrent.{Future, ExecutionContext}
import scala.concurrent.duration.FiniteDuration
import scala.collection.mutable
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Test implementation of StreamingRuntime for unit testing
 * Uses simple in-memory collections and synchronous operations
 */
class TestStreamingRuntime(implicit ec: ExecutionContext) extends StreamingRuntime {
  
  type ActorSystem = TestActorSystem
  type Source[+Out, +Mat] = TestSource[Out, Mat]
  type Flow[-In, +Out, +Mat] = TestFlow[In, Out, Mat]
  type Sink[-In, +Mat] = TestSink[In, Mat]
  type RunnableGraph[+Mat] = TestRunnableGraph[Mat]
  type NotUsed = TestNotUsed.type

  def createActorSystem(name: String): TestActorSystem = new TestActorSystem(name)
  
  def terminateActorSystem(system: TestActorSystem): Future[Unit] = {
    system.terminate()
    Future.successful(())
  }

  def emptySource[T]: TestSource[T, TestNotUsed.type] = 
    TestSource.empty[T]

  def fromIterator[T](iterator: () => Iterator[T]): TestSource[T, TestNotUsed.type] = 
    TestSource.fromIterator(iterator)

  def tick[T](initialDelay: FiniteDuration, interval: FiniteDuration, tick: T): TestSource[T, Any] = 
    TestSource.tick(initialDelay, interval, tick)

  def mapFlow[In, Out](f: In => Out): TestFlow[In, Out, TestNotUsed.type] = 
    TestFlow.map(f)

  def filterFlow[T](predicate: T => Boolean): TestFlow[T, T, TestNotUsed.type] = 
    TestFlow.filter(predicate)

  def dropWhileFlow[T](predicate: T => Boolean): TestFlow[T, T, TestNotUsed.type] = 
    TestFlow.dropWhile(predicate)

  def groupedFlow[T](batchSize: Int): TestFlow[T, List[T], TestNotUsed.type] = 
    TestFlow.grouped(batchSize)

  def groupedWithinFlow[T](batchSize: Int, timeout: FiniteDuration): TestFlow[T, List[T], TestNotUsed.type] = 
    TestFlow.groupedWithin(batchSize, timeout)

  def batchFlow[T, S](seed: T => S)(aggregate: (S, T) => S): TestFlow[T, S, TestNotUsed.type] = 
    TestFlow.batch(seed)(aggregate)

  def foreachSink[T](f: T => Unit): TestSink[T, Future[Unit]] = 
    TestSink.foreach(f)

  def seqSink[T]: TestSink[T, Future[Seq[T]]] = 
    TestSink.seq[T]

  def runGraph[Mat](source: TestSource[Any, Any], sink: TestSink[Any, Mat])(implicit system: TestActorSystem): Mat = {
    source.runWith(sink)
  }

  def createRunnableGraph[Mat](source: TestSource[Any, Any], sink: TestSink[Any, Mat]): TestRunnableGraph[Mat] = 
    TestRunnableGraph(source, sink)

  // Helper method to convert any Flow to our TestFlow
  def asTestFlow[In, Out](flow: Flow[In, Out, NotUsed]): TestFlow[In, Out, TestNotUsed.type] = {
    flow match {
      case testFlow: TestFlow[In, Out, TestNotUsed.type] => testFlow
      case _ => 
        // For flows that aren't TestFlows, we need to create a wrapper
        // This is a simplified approach - in practice, you'd want more sophisticated handling
        TestFlow[In, Out, TestNotUsed.type]((elements: List[In]) => {
          elements.asInstanceOf[List[Out]] // Placeholder transformation
        }, TestNotUsed)
    }
  }

  // Helper method to convert any Sink to our TestSink
  def asTestSink[In](sink: Sink[In, NotUsed]): TestSink[In, Future[Unit]] = {
    sink match {
      case testSink: TestSink[In, Future[Unit]] => testSink
      case _ => 
        // For sinks that aren't TestSinks, we need to create a wrapper
        TestSink[In, Future[Unit]] { elements =>
          Future.successful(()) // Placeholder implementation
        }
    }
  }
}

// Test implementations
object TestNotUsed

case class TestActorSystem(name: String) {
  private val terminated = new AtomicBoolean(false)
  
  def terminate(): Unit = terminated.set(true)
  def isTerminated: Boolean = terminated.get()
}

case class TestSource[+Out, +Mat](elements: List[Out], mat: Mat) {
  def runWith[In, Mat2](sink: TestSink[In, Mat2])(implicit ev: Out <:< In): Mat2 = {
    sink.run(elements.asInstanceOf[List[In]])
  }
  
  def via[Out2, Mat2](flow: TestFlow[Out, Out2, Mat2]): TestSource[Out2, TestNotUsed.type] = {
    TestSource(flow.transform(elements), TestNotUsed)
  }
  
  // Helper method to work with runtime flows
  def viaRuntime[Out2](flow: TestStreamingRuntime#Flow[Out, Out2, TestStreamingRuntime#NotUsed]): TestSource[Out2, TestNotUsed.type] = {
    // Since we know this is our test runtime, we can safely cast
    flow match {
      case testFlow: TestFlow[Out, Out2, TestNotUsed.type] => via(testFlow)
      case _ => 
        // Create a TestFlow wrapper for any Flow that implements toRuntimeFlow
        val wrappedFlow = TestFlow[Out, Out2, TestNotUsed.type]((elements: List[Out]) => {
           // This is a placeholder - in a real implementation, we'd need to handle this properly
           elements.asInstanceOf[List[Out2]]
         }, TestNotUsed)
        via(wrappedFlow)
    }
  }
}

object TestSource {
  def empty[T]: TestSource[T, TestNotUsed.type] = 
    TestSource(List.empty[T], TestNotUsed)
    
  def fromIterator[T](iterator: () => Iterator[T]): TestSource[T, TestNotUsed.type] = 
    TestSource(iterator().toList, TestNotUsed)
    
  def tick[T](initialDelay: FiniteDuration, interval: FiniteDuration, tick: T): TestSource[T, Any] = 
    TestSource(List(tick), "tick-mat")
    
  def apply[T](elements: T*): TestSource[T, TestNotUsed.type] = 
    TestSource(elements.toList, TestNotUsed)
}

case class TestFlow[-In, +Out, +Mat](transform: List[In] => List[Out], mat: Mat)

object TestFlow {
  def map[In, Out](f: In => Out): TestFlow[In, Out, TestNotUsed.type] = 
    TestFlow(_.map(f), TestNotUsed)
    
  def filter[T](predicate: T => Boolean): TestFlow[T, T, TestNotUsed.type] = 
    TestFlow(_.filter(predicate), TestNotUsed)
    
  def dropWhile[T](predicate: T => Boolean): TestFlow[T, T, TestNotUsed.type] = 
    TestFlow(_.dropWhile(predicate), TestNotUsed)
    
  def grouped[T](batchSize: Int): TestFlow[T, List[T], TestNotUsed.type] = 
    TestFlow(_.grouped(batchSize).toList, TestNotUsed)
    
  def groupedWithin[T](batchSize: Int, timeout: FiniteDuration): TestFlow[T, List[T], TestNotUsed.type] = 
    TestFlow(_.grouped(batchSize).toList, TestNotUsed) // Simplified - ignores timeout in test
    
  def batch[T, S](seed: T => S)(aggregate: (S, T) => S): TestFlow[T, S, TestNotUsed.type] = 
    TestFlow({ elements =>
      elements.headOption match {
        case Some(first) => 
          val initial = seed(first)
          List(elements.tail.foldLeft(initial)(aggregate))
        case None => List.empty[S]
      }
    }, TestNotUsed)
}

case class TestSink[-In, +Mat](run: List[In] => Mat)

object TestSink {
  def foreach[T](f: T => Unit): TestSink[T, Future[Unit]] = 
    TestSink { elements =>
      elements.foreach(f)
      Future.successful(())
    }
    
  def seq[T]: TestSink[T, Future[Seq[T]]] = 
    TestSink(elements => Future.successful(elements.toSeq))
    
  def ignore[T]: TestSink[T, Future[Unit]] = 
    TestSink(_ => Future.successful(()))
}

case class TestRunnableGraph[+Mat](source: TestSource[Any, Any], sink: TestSink[Any, Mat]) {
  def run()(implicit system: TestActorSystem): Mat = {
    source.runWith(sink)
  }
}