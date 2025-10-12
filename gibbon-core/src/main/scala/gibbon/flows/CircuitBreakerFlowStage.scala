package gibbon.flows

import gibbon.core.Flow
import gibbon.runtime.StreamingRuntime
import gibbon.circuitbreaker.{CircuitBreaker, CircuitBreakerConfig, CircuitBreakerState}
import scala.concurrent.{Future, ExecutionContext}
import scala.util.{Success, Failure}

/**
 * Specialized circuit breaker flow stage for common patterns
 */
sealed trait CircuitBreakerStage[I, O] extends Flow[I, O] {

  protected def circuitBreaker: CircuitBreaker
  protected def shouldFailOnOpen: Boolean

  def getState: CircuitBreakerState = circuitBreaker.getState
  def getMetrics = circuitBreaker.getMetrics
  def getName: String
}

/**
 * Circuit breaker that applies a transformation with protection
 */
case class ProtectedTransformStage[I, O](
  name: String,
  transform: I => O,
  config: CircuitBreakerConfig = CircuitBreakerConfig(),
  fallback: Option[I => O] = None,
  shouldFailOnOpen: Boolean = true
)(implicit ec: ExecutionContext) extends CircuitBreakerStage[I, O] {

  val circuitBreaker = new CircuitBreaker(name, config)

  override def getName: String = name

  override def toRuntimeFlow[R <: StreamingRuntime]()(implicit runtime: R): runtime.Flow[I, O, runtime.NotUsed] = {
    
    runtime.mapAsyncFlow[I, O](parallelism = 1) { element =>
      circuitBreaker.execute {
        Future { transform(element) }
      }.recover {
        case ex if ex.getMessage.contains("Circuit breaker") && !shouldFailOnOpen =>
          fallback.map(f => f(element)).getOrElse(element.asInstanceOf[O])
      }
    }
  }
}

/**
 * Circuit breaker that just monitors without transformation
 */
case class MonitorStage[I](
  name: String,
  config: CircuitBreakerConfig = CircuitBreakerConfig(),
  shouldFailOnOpen: Boolean = false
)(implicit ec: ExecutionContext) extends CircuitBreakerStage[I, I] {

  val circuitBreaker = new CircuitBreaker(name, config)

  override def getName: String = name

  override def toRuntimeFlow[R <: StreamingRuntime]()(implicit runtime: R): runtime.Flow[I, I, runtime.NotUsed] = {
    
    runtime.mapAsyncFlow[I, I](parallelism = 1) { element =>
      circuitBreaker.execute {
        Future { element }
      }.recover {
        case ex if ex.getMessage.contains("Circuit breaker") && !shouldFailOnOpen =>
          element
      }
    }
  }
}

/**
 * Factory for creating circuit breaker stages
 */
object CircuitBreakerStage {

  def protectTransform[I, O](
    name: String,
    transform: I => O,
    config: CircuitBreakerConfig = CircuitBreakerConfig()
  )(implicit ec: ExecutionContext): ProtectedTransformStage[I, O] = {
    ProtectedTransformStage(name, transform, config)
  }

  def monitor[I](
    name: String,
    config: CircuitBreakerConfig = CircuitBreakerConfig()
  )(implicit ec: ExecutionContext): MonitorStage[I] = {
    MonitorStage(name, config)
  }
}