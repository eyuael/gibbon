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
  
}