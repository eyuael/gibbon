package gibbon.circuitbreaker

import scala.concurrent.duration.FiniteDuration
import java.util.concurrent.atomic.AtomicReference
import java.time.Instant
import scala.concurrent.{Future, ExecutionContext}
import scala.util.{Success, Failure}

sealed trait CircuitBreakerState
final case object Closed extends CircuitBreakerState
final case object Open extends CircuitBreakerState  
final case object HalfOpen extends CircuitBreakerState

case class CircuitBreakerConfig(
    failureThreshold: Int = 5,
    successThreshold: Int = 2,
    timeout: FiniteDuration = FiniteDuration(60, "seconds"),
    slidingWindowSize: Int = 10,
    minimumNumberOfCalls: Int = 10
)

case class CircuitBreakerMetrics(
    totalCalls: Long = 0,
    successfulCalls: Long = 0,
    failedCalls: Long = 0,
    consecutiveFailures: Int = 0,
    consecutiveSuccesses: Int = 0,
    lastFailureTime: Option[Instant] = None,
    lastStateChangeTime: Instant = Instant.now()
)

class CircuitBreaker(
  name: String,
  config: CircuitBreakerConfig = CircuitBreakerConfig()
)(implicit ec: ExecutionContext){

  private val state = new AtomicReference[CircuitBreakerState](Closed)
  private val metrics = new AtomicReference[CircuitBreakerMetrics](CircuitBreakerMetrics())

  def getState: CircuitBreakerState = state.get()
  def getMetrics: CircuitBreakerMetrics = metrics.get()

  def isClosed: Boolean = getState == Closed
  def isOpen: Boolean = getState == Open
  def isHalfOpen: Boolean = getState == HalfOpen

  def excecute[T](operation: => Future[T]): Future[T] = {
    state.get() match {
      case Open => 
        if (shouldAttemptReset){
          state.compareAndSet(Open, HalfOpen)
          executeOperation(operation)
        } else {
          Future.failed(new RuntimeException(s"Circuit breaker $name is open"))
        }
      case HalfOpen =>
        executeOperation(operation)
        
      case Closed =>
        executeOperation(operation)
    }
  }
  
  private def executeOperation[T](operation: => Future[T]): Future[T] = {
    operation.andThen {
      case Success(result) =>
        onSuccess()
      case Failure(exception) =>
        onFailure()
    }
  }
  private def onSuccess(): Unit = {
    val currentMetrics = metrics.get()
    val newMetrics = currentMetrics.copy(
      totalCalls = currentMetrics.totalCalls + 1,
      successfulCalls = currentMetrics.successfulCalls + 1,
      consecutiveFailures = 0,
      consecutiveSuccesses = currentMetrics.consecutiveSuccesses + 1
    )
    
    metrics.set(newMetrics)
    
    state.get() match {
      case HalfOpen =>
        if (newMetrics.consecutiveSuccesses >= config.successThreshold) {
          state.compareAndSet(HalfOpen, Closed)
          metrics.set(newMetrics.copy(
            consecutiveSuccesses = 0,
            lastStateChangeTime = Instant.now()
          ))
        }
      case _ => // Do nothing for Closed state
    }
  }
  
  private def onFailure(): Unit = {
    val currentMetrics = metrics.get()
    val newMetrics = currentMetrics.copy(
      totalCalls = currentMetrics.totalCalls + 1,
      failedCalls = currentMetrics.failedCalls + 1,
      consecutiveFailures = currentMetrics.consecutiveFailures + 1,
      consecutiveSuccesses = 0,
      lastFailureTime = Some(Instant.now())
    )
    
    metrics.set(newMetrics)
    
    state.get() match {
      case Closed =>
        if (newMetrics.consecutiveFailures >= config.failureThreshold && 
            newMetrics.totalCalls >= config.minimumNumberOfCalls) {
          state.compareAndSet(Closed, Open)
          metrics.set(newMetrics.copy(
            lastStateChangeTime = Instant.now()
          ))
        }
      case HalfOpen =>
        state.compareAndSet(HalfOpen, Open)
        metrics.set(newMetrics.copy(
          lastStateChangeTime = Instant.now()
        ))
      case _ => 
    }
  }
  
  private def shouldAttemptReset: Boolean = {
    metrics.get().lastStateChangeTime.plusSeconds(config.timeout.toSeconds).isBefore(Instant.now())
  }
  
  def reset(): Unit = {
    state.set(Closed)
    metrics.set(CircuitBreakerMetrics())
  }
  // Add to CircuitBreaker class
def executeAsync[T](operation: => Future[T])(implicit ec: ExecutionContext): Future[T] = {
  execute(operation).flatMap(identity)
}

// Add better error handling for flow stages
def executeFlowStage[T](operation: => T): T = {
  if (state == state.Open) {
    throw new RuntimeException(s"Circuit breaker '$name' is OPEN")
  }
  
  try {
    val result = operation
    onSuccess()
    result
  } catch {
    case ex: Exception =>
      onFailure(ex)
      throw ex
  }
}
}