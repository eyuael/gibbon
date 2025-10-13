package gibbon.flows

import gibbon.ratelimiter.RateLimiterConfig
import gibbon.runtime.{TestStreamingRuntime, TestSource}
import munit.FunSuite
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import java.time.Duration

/**
 * Unit tests for [[gibbon.flows.RateLimitingFlow]]
 */
class RateLimitingFlowSpec extends FunSuite {

  implicit val runtime: TestStreamingRuntime = new TestStreamingRuntime()

  test("RateLimitingFlow should allow maxTokens within interval without delay") {
    val cfg = RateLimiterConfig(maxTokens = 3, refillEvery = Duration.ofSeconds(1), refillAmount = 3)
    val flow = RateLimitingFlow[Int, Int]("limiter", _ + 1, cfg)

    val src = TestSource(1, 2, 3)
    val sink = runtime.seqSink[Int]

    val fut = src.viaRuntime(flow.toRuntimeFlow).runWith(sink)
    val result = scala.concurrent.Await.result(fut, 3.seconds)

    assertEquals(result.toList, List(2, 3, 4))
  }

  test("RateLimitingFlow should queue when tokens exhausted and resume after refill") {
    val cfg = RateLimiterConfig(maxTokens = 1, refillEvery = Duration.ofMillis(200), refillAmount = 1)
    // Make the operation take ~250ms so that the refill interval elapses while it is executing
    val sleepTransform: Int => Int = n => { Thread.sleep(250); n * 10 }
    val flow = RateLimitingFlow[Int, Int]("limiter-queue", sleepTransform, cfg)

    val start = System.nanoTime()
    val src = TestSource(1, 2) // Requires 2 tokens but we only have 1 initially
    val sink = runtime.seqSink[Int]

    val fut = src.viaRuntime(flow.toRuntimeFlow).runWith(sink)
    val result = scala.concurrent.Await.result(fut, 5.seconds)
    val elapsedMs = (System.nanoTime() - start) / 1e6

    // First element processed immediately (~250ms), second should start only after refill (~>=250ms extra)
    assert(elapsedMs >= 400) // allow small jitter
    assertEquals(result.toList, List(10, 20))
  }
}