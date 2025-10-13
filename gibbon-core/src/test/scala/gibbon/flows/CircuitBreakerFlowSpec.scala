package gibbon.flows

import gibbon.circuitbreaker.CircuitBreakerConfig
import gibbon.runtime.{TestStreamingRuntime, TestSource}
import munit.FunSuite
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Try, Success, Failure}

/**
 * Unit tests for [[gibbon.flows.CircuitBreakerFlow]]
 */
class CircuitBreakerFlowSpec extends FunSuite {

  implicit val runtime: TestStreamingRuntime = new TestStreamingRuntime()

  test("CircuitBreakerFlow should pass through elements when closed") {
    val cfg = CircuitBreakerConfig(failureThreshold = 3, successThreshold = 2, minimumNumberOfCalls = 0)
    val cbFlow = CircuitBreakerFlow[Int, Int]("cb-pass", _ * 2, cfg)

    val src = TestSource(1, 2, 3)
    val sink = runtime.seqSink[Int]

    val resultFut = src.viaRuntime(cbFlow.toRuntimeFlow).runWith(sink)
    val result = scala.concurrent.Await.result(resultFut, 3.seconds)

    assertEquals(result.toList, List(2, 4, 6))
    assertEquals(cbFlow.getState.toString, "Closed")
  }

  test("CircuitBreakerFlow should open after failure threshold and fallback when configured") {
    // Open after a single failure to simplify testing
    val cfg = CircuitBreakerConfig(failureThreshold = 1, successThreshold = 1, minimumNumberOfCalls = 0)

    // Transform fails for negative numbers
    val transform: Int => Int = n => if (n < 0) throw new RuntimeException("boom") else n

    val cbFlow = CircuitBreakerFlow[Int, Int]("cb-open", transform, cfg)
      .withFallback(identity)       // use the incoming element as fallback
      .failOnOpen(false)            // do NOT fail fast when open

    // Phase 1: expect the first element to fail while breaker is still closed
    val src1 = TestSource(-1)
    val sink1 = runtime.seqSink[Int]
    val failOutcome = Try {
      val fut = src1.viaRuntime(cbFlow.toRuntimeFlow).runWith(sink1)
      scala.concurrent.Await.result(fut, 3.seconds)
    }
    assert(failOutcome.isFailure)

    // Phase 2: breaker should now be open, next element should use fallback and succeed
    val src2 = TestSource(42)
    val sink2 = runtime.seqSink[Int]
    val succ = Await.result(src2.viaRuntime(cbFlow.toRuntimeFlow).runWith(sink2), 3.seconds)
    assertEquals(succ.toList, List(42))

    // Circuit breaker must be open now
    assertEquals(cbFlow.getState.toString, "Open")
  }
}