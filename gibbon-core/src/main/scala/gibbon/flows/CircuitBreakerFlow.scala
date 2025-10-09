package gibbon.flows

import gibbon.core.Flow
import gibbon.runtime.StreamingRuntime
import gibbon.circuitbreaker.{CircuitBreaker, CircuitBreakerConfig}
import gibbon.error.{CircuitBreakerOpenError, ProcessingError}
import scala.concurrent.{Future, ExecutionContext}
import scala.util.{Success, Failure}

/**
 * CircuitBreakerFlow - wraps a flow with circuit breaker protection for downstream systems
 * 
 * Example usage:
 * ```scala
 * val protectedFlow = CircuitBreakerFlow[Event[String, String], Event[String, String]](
 *   "database-sink-protector",
 *   CircuitBreakerConfig(
 *     failureThreshold = 5,
 *     successThreshold = 3,
 *     timeout = 30.seconds
 *   )
 * )
 * 
 * // Use in pipeline
 * val pipeline = source.via(protectedFlow).via(transformFlow).to(sink)
 * ```
 */
class CircuitBreakerFlow[I, O](
  circuitBreakerName: String,
  config: CircuitBreakerConfig = CircuitBreakerConfig(),
  fallback: Option[I => O] = None,
  shouldFailOnOpen: Boolean = true
)(implicit ec: ExecutionContext) extends Flow[I, O] {

  private val circuitBreaker = new CircuitBreaker(circuitBreakerName, config)

  override def toRuntimeFlow[R <: StreamingRuntime]()(implicit runtime: R): runtime.Flow[I, O, runtime.NotUsed] = {
    runtime.mapFlow[I, O] { element =>
      // For synchronous operations, we need to handle the circuit breaker logic
      // In a real implementation, this would be async, but for simplicity we'll use Future
      val resultFuture = circuitBreaker.excecute {
        Future {
          // This is a placeholder - the actual transformation would be applied here
          // In practice, you'd wrap an actual flow operation
          element.asInstanceOf[O]
        }
      }

      // Block and wait for result (not ideal for production, but works for this example)
      import scala.concurrent.duration._
      try {
        import scala.concurrent.Await
        Await.result(resultFuture, 5.seconds)
      } catch {
        case ex: RuntimeException if ex.getMessage.contains("Circuit breaker") =>
          if (shouldFailOnOpen) {
            throw CircuitBreakerOpenError(s"Circuit breaker '$circuitBreakerName' is OPEN", Option(ex))
          } else {
\            fallback.map(f => f(element)).getOrElse(element.asInstanceOf[O])
          }
        case ex: Throwable =>
          throw ProcessingError(s"Circuit breaker flow processing failed", Some(ex))
      }
    }
  }

  def getCircuitBreaker: CircuitBreaker = circuitBreaker
  def getState = circuitBreaker.getState
  def getMetrics = circuitBreaker.getMetrics
}

/**
 * CircuitBreakerFlow companion object with factory methods
 */
object CircuitBreakerFlow {

  /**
   * Create a simple circuit breaker flow with basic configuration
   */
  def apply[I, O](
    name: String,
    config: CircuitBreakerConfig = CircuitBreakerConfig()
  )(implicit ec: ExecutionContext): CircuitBreakerFlow[I, O] = {
    new CircuitBreakerFlow[I, O](name, config)
  }

  /**
   * Create a circuit breaker flow with fallback function
   */
  def withFallback[I, O](
    name: String,
    fallback: I => O,
    config: CircuitBreakerConfig = CircuitBreakerConfig()
  )(implicit ec: ExecutionContext): CircuitBreakerFlow[I, O] = {
    new CircuitBreakerFlow[I, O](name, config, Some(fallback), shouldFailOnOpen = false)
  }

  /**
   * Create a circuit breaker flow that passes through on open circuit (no failure)
   */
  def passThroughOnOpen[I, O](
    name: String,
    config: CircuitBreakerConfig = CircuitBreakerConfig()
  )(implicit ec: ExecutionContext): CircuitBreakerFlow[I, O] = {
    new CircuitBreakerFlow[I, O](name, config, None, shouldFailOnOpen = false)
  }

  /**
   * Wrap an existing flow with circuit breaker protection
   * This is a more advanced usage pattern
   */
  def wrapFlow[I, O](
    name: String,
    wrappedFlow: Flow[I, O],
    config: CircuitBreakerConfig = CircuitBreakerConfig()
  )(implicit ec: ExecutionContext): Flow[I, O] = {
    // This would require more complex implementation to properly wrap existing flows
    // For now, this is a placeholder that creates a new CircuitBreakerFlow
    new CircuitBreakerFlow[I, O](name, config)
  }
}
  

