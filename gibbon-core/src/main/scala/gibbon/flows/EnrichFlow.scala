package gibbon.flows

import gibbon.core.Flow
import gibbon.runtime.StreamingRuntime
import scala.concurrent.Future
import scala.concurrent.ExecutionContext

/**
 * EnrichFlow - enriches events with additional data
 * 
 * Example usage:
 * ```scala
 * val enrichFlow = EnrichFlow[Event[String, String], Event[String, String]](event => 
 *   event.copy(value = s"${event.value} - enriched")
 * )
 * ```
 */
class EnrichFlow[I, O](enricher: I => O) extends Flow[I, O] {
  
  override def toRuntimeFlow[R <: StreamingRuntime]()(implicit runtime: R): runtime.Flow[I, O, runtime.NotUsed] = {
    runtime.mapFlow(enricher)
  }
}

/**
 * AsyncEnrichFlow - enriches events with additional data asynchronously
 * 
 * Note: This implementation is simplified for the runtime abstraction.
 * For full async support, the StreamingRuntime trait would need mapAsync methods.
 * 
 * Example usage:
 * ```scala
 * val asyncEnrichFlow = AsyncEnrichFlow[Event[String, String], Event[String, String]](event => 
 *   Future.successful(event.copy(value = s"${event.value} - async enriched"))
 * )
 * ```
 */
class AsyncEnrichFlow[I, O](enricher: I => Future[O])(implicit ec: ExecutionContext) extends Flow[I, O] {
  
  override def toRuntimeFlow[R <: StreamingRuntime]()(implicit runtime: R): runtime.Flow[I, O, runtime.NotUsed] = {
    // Simplified implementation - blocks on Future for now
    // In a full implementation, we'd need mapAsync in StreamingRuntime
    runtime.mapFlow { input: I =>
      import scala.concurrent.Await
      import scala.concurrent.duration._
      Await.result(enricher(input), 10.seconds)
    }
  }
}

/**
 * EnrichFlow companion object with factory methods
 */
object EnrichFlow {
  
  /**
   * Create a simple enrich flow with an enricher function
   */
  def apply[I, O](enricher: I => O): EnrichFlow[I, O] = {
    new EnrichFlow[I, O](enricher)
  }
  
  /**
   * Create an async enrich flow with a future-based enricher
   */
  def async[I, O](enricher: I => Future[O])(implicit ec: ExecutionContext): AsyncEnrichFlow[I, O] = {
    new AsyncEnrichFlow[I, O](enricher)
  }
  
  /**
   * Create an enrich flow that adds a timestamp to events
   */
  def addTimestamp[T]: EnrichFlow[T, (T, Long)] = {
    new EnrichFlow[T, (T, Long)](event => (event, System.currentTimeMillis()))
  }
  
  /**
   * Create an enrich flow that adds a unique ID to events
   */
  def addUniqueId[T]: EnrichFlow[T, (T, String)] = {
    new EnrichFlow[T, (T, String)](event => (event, java.util.UUID.randomUUID().toString))
  }
  
  /**
   * Create an enrich flow that adds metadata to events
   */
  def addMetadata[T](metadata: Map[String, Any]): EnrichFlow[T, (T, Map[String, Any])] = {
    new EnrichFlow[T, (T, Map[String, Any])](event => (event, metadata))
  }
}