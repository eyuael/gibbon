package gibbon.flows

import gibbon.core.Flow
import gibbon.runtime.StreamingRuntime
import gibbon.circuitbreaker.{CircuitBreaker, CircuitBreakerConfig}
import scala.concurrent.{Future, ExecutionContext}
import scala.util.{Success, Failure}
import scala.concurrent.duration._

/**
 * CircuitBreakerFlow implemented as a proper flow stage
 * 
 * This creates a transformation stage that can be inserted anywhere in a flow
 * without wrapping the entire flow structure.
 */
class CircuitBreakerFlow[I, O] private (
  private val circuitBreaker: CircuitBreaker,
  private val transform: I => O,
  private val fallback: Option[I => O] = None,
  private val shouldFailOnOpen: Boolean = true
)(implicit ec: ExecutionContext) extends Flow[I, O] {

  override def toRuntimeFlow[R <: StreamingRuntime]()
                     (implicit runtime: R): runtime.Flow[I, O, runtime.NotUsed] = {

    val parallelism = 1

    runtime.mapAsyncFlow[I, O](parallelism) { element =>
      circuitBreaker.execute {
        Future { transform(element: I) }
      }.recover {
        case ex if ex.getMessage.contains("Circuit breaker") && !shouldFailOnOpen =>
          fallback.map(f => f(element: I)).getOrElse(element.asInstanceOf[O])
      }
    }
  }

  // Builder methods for configuration
  def withFallback(fallbackFn: I => O): CircuitBreakerFlow[I, O] = {
    new CircuitBreakerFlow[I, O](circuitBreaker, transform, Some(fallbackFn), shouldFailOnOpen)
  }

  def failOnOpen(shouldFail: Boolean): CircuitBreakerFlow[I, O] = {
    new CircuitBreakerFlow[I, O](circuitBreaker, transform, fallback, shouldFail)
  }

  def getMetrics = circuitBreaker.getMetrics
  def getState = circuitBreaker.getState
}

/**
 * Companion object with factory methods
 */
object CircuitBreakerFlow {

  /**
   * Create a circuit breaker flow stage with a transformation function
   */
  def apply[I, O](
    name: String,
    transform: I => O,
    config: CircuitBreakerConfig = CircuitBreakerConfig()
  )(implicit ec: ExecutionContext): CircuitBreakerFlow[I, O] = {
    val circuitBreaker = new CircuitBreaker(name, config)
    new CircuitBreakerFlow[I, O](circuitBreaker, transform)
  }

  /**
   * Create a circuit breaker that protects an existing transformation
   */
  def protect[I, O](
    name: String,
    protectedTransform: I => O,
    config: CircuitBreakerConfig = CircuitBreakerConfig()
  )(implicit ec: ExecutionContext): CircuitBreakerFlow[I, O] = {
    apply(name, protectedTransform, config)
  }

  /**
   * Create a pass-through circuit breaker (no transformation, just protection)
   */
  def passThrough[I](
    name: String,
    config: CircuitBreakerConfig = CircuitBreakerConfig()
  )(implicit ec: ExecutionContext): CircuitBreakerFlow[I, I] = {
    apply(name, identity[I], config)
  }
}