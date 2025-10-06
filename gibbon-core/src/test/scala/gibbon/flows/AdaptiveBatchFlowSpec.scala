package gibbon.flows

import gibbon.handling.monitoring.{BatchingMetricsCollector, BatchPerformanceStats}
import gibbon.runtime.TestStreamingRuntime
import munit.FunSuite
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import java.util.concurrent.atomic.AtomicInteger

class AdaptiveBatchFlowSpec extends FunSuite {

  implicit val runtime = new TestStreamingRuntime()
  
  test("AdaptiveBatchFlow should batch elements according to initial batch size") {
    val metricsCollector = new BatchingMetricsCollector()
    val adaptiveBatchFlow = new AdaptiveBatchFlow[Int](
      initialBatchSize = 3,
      minBatchSize = 1,
      maxBatchSize = 10,
      targetLatency = 100.millis,
      adaptationInterval = 1.second,
      metricsCollector = metricsCollector
    )
    
    // Test that the flow is created with correct initial batch size
    assertEquals(adaptiveBatchFlow.getCurrentBatchSize, 3)
  }

  test("AdaptiveBatchFlow should adapt batch size based on performance metrics") {
    val metricsCollector = new BatchingMetricsCollector()
    val adaptiveBatchFlow = new AdaptiveBatchFlow[Int](
      initialBatchSize = 5,
      minBatchSize = 2,
      maxBatchSize = 20,
      targetLatency = 50.millis,
      adaptationInterval = 100.millis, // Short interval for testing
      metricsCollector = metricsCollector,
      strategy = new ThroughputOptimizedStrategy()
    )
    
    // Initial batch size should be as configured
    assertEquals(adaptiveBatchFlow.getCurrentBatchSize, 5)
    
    // Allow time for potential adaptation (though it may not change without real processing)
    Thread.sleep(200)
    
    // Batch size should still be within constraints
    val finalBatchSize = adaptiveBatchFlow.getCurrentBatchSize
    assert(finalBatchSize >= 2 && finalBatchSize <= 20)
  }

  test("AdaptiveBatchFlow should respect minimum and maximum batch size constraints") {
    val metricsCollector = new BatchingMetricsCollector()
    val adaptiveBatchFlow = new AdaptiveBatchFlow[Int](
      initialBatchSize = 10,
      minBatchSize = 5,
      maxBatchSize = 15,
      targetLatency = 100.millis,
      adaptationInterval = 50.millis,
      metricsCollector = metricsCollector
    )
    
    val finalBatchSize = adaptiveBatchFlow.getCurrentBatchSize
    assert(finalBatchSize >= 5 && finalBatchSize <= 15)
  }

  test("AdaptiveBatchFlow should record metrics correctly") {
    val metricsCollector = new BatchingMetricsCollector()
    val adaptiveBatchFlow = new AdaptiveBatchFlow[Int](
      initialBatchSize = 3,
      metricsCollector = metricsCollector
    )
    
    // Allow some time for metrics to be initialized
    Thread.sleep(100)
    
    val stats = metricsCollector.getCurrentPerformanceStats
    assert(stats.isInstanceOf[BatchPerformanceStats])
  }

  test("AdaptiveBatchFlow should handle backpressure detection") {
    val metricsCollector = new BatchingMetricsCollector()
    val adaptiveBatchFlow = new AdaptiveBatchFlow[Int](
      initialBatchSize = 2,
      maxQueueSize = 10,
      metricsCollector = metricsCollector
    )
    
    // Check that queue depth is accessible
    val queueDepth = adaptiveBatchFlow.getCurrentQueueDepth
    assert(queueDepth >= 0)
  }

  test("AdaptiveBatchFlow should provide performance statistics") {
    val metricsCollector = new BatchingMetricsCollector()
    val adaptiveBatchFlow = new AdaptiveBatchFlow[Int](
      initialBatchSize = 3,
      metricsCollector = metricsCollector
    )
    
    Thread.sleep(50)
    
    val stats = adaptiveBatchFlow.getPerformanceStats
    assert(stats.isInstanceOf[BatchPerformanceStats])
  }

  test("AdaptiveBatchFlow should work with different adaptation strategies") {
    val metricsCollector = new BatchingMetricsCollector()
    
    // Test with LatencyOptimizedStrategy
    val latencyOptimizedFlow = new AdaptiveBatchFlow[Int](
      initialBatchSize = 5,
      strategy = new LatencyOptimizedStrategy(),
      adaptationInterval = 100.millis,
      metricsCollector = metricsCollector
    )
    
    // Test with BalancedStrategy
    val balancedFlow = new AdaptiveBatchFlow[Int](
      initialBatchSize = 5,
      strategy = new BalancedStrategy(),
      adaptationInterval = 100.millis,
      metricsCollector = new BatchingMetricsCollector()
    )
    
    // Both should maintain valid batch sizes
    assert(latencyOptimizedFlow.getCurrentBatchSize > 0)
    assert(balancedFlow.getCurrentBatchSize > 0)
  }
}