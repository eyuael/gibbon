package gibbon.flows

import gibbon.flows.{AdaptiveBatchingStrategy, ThroughputOptimizedStrategy, LatencyOptimizedStrategy, BalancedStrategy, BatchSizeAdjustment}
import gibbon.handling.monitoring.BatchPerformanceStats
import munit.FunSuite
import scala.concurrent.duration._

class AdaptiveBatchingStrategySpec extends FunSuite {

  test("ThroughputOptimizedStrategy should increase batch size when throughput is improving") {
    val strategy = new ThroughputOptimizedStrategy(
      latencyThreshold = 1.5,
      throughputImprovementThreshold = 0.05,
      aggressiveness = 0.2
    )
    
    val stats = BatchPerformanceStats(
      avgThroughput = 100.0,
      avgLatency = 50.0, // Below target
      avgBatchSize = 10.0,
      throughputTrend = 0.1, // Above improvement threshold
      latencyTrend = -2.0, // Decreasing latency
      sampleCount = 10
    )
    
    val adjustment = strategy.adjustBatchSize(
      currentBatchSize = 10,
      stats = stats,
      targetLatency = 100.millis,
      minBatchSize = 1,
      maxBatchSize = 1000
    )
    
    assert(adjustment.newBatchSize > 10) // Should increase
    assertEquals(adjustment.reason, "throughput_improving")
  }

  test("ThroughputOptimizedStrategy should decrease batch size when latency exceeds threshold") {
    val strategy = new ThroughputOptimizedStrategy(
      latencyThreshold = 1.5,
      throughputImprovementThreshold = 0.05,
      aggressiveness = 0.2
    )
    
    val stats = BatchPerformanceStats(
      avgThroughput = 100.0,
      avgLatency = 200.0, // 2x target (exceeds 1.5x threshold)
      avgBatchSize = 50.0,
      throughputTrend = -0.02,
      latencyTrend = 3.0,
      sampleCount = 10
    )
    
    val adjustment = strategy.adjustBatchSize(
      currentBatchSize = 50,
      stats = stats,
      targetLatency = 100.millis,
      minBatchSize = 1,
      maxBatchSize = 1000
    )
    
    assert(adjustment.newBatchSize < 50) // Should decrease
    assertEquals(adjustment.reason, "latency_exceeded_threshold")
  }

  test("LatencyOptimizedStrategy should maintain batch size when latency is within tolerance") {
    val strategy = new LatencyOptimizedStrategy(
      latencyTolerance = 0.1,
      minAdjustmentPercent = 0.1
    )
    
    val stats = BatchPerformanceStats(
      avgThroughput = 100.0,
      avgLatency = 105.0, // 5% above target (within 10% tolerance)
      avgBatchSize = 20.0,
      throughputTrend = 0.0,
      latencyTrend = 0.0,
      sampleCount = 10
    )
    
    val adjustment = strategy.adjustBatchSize(
      currentBatchSize = 20,
      stats = stats,
      targetLatency = 100.millis,
      minBatchSize = 1,
      maxBatchSize = 1000
    )
    
    assertEquals(adjustment.newBatchSize, 20) // Should maintain
    assertEquals(adjustment.reason, "latency_within_tolerance")
  }

  test("LatencyOptimizedStrategy should decrease batch size when latency exceeds tolerance") {
    val strategy = new LatencyOptimizedStrategy(
      latencyTolerance = 0.1,
      minAdjustmentPercent = 0.1
    )
    
    val stats = BatchPerformanceStats(
      avgThroughput = 100.0,
      avgLatency = 130.0, // 30% above target (exceeds 10% tolerance)
      avgBatchSize = 30.0,
      throughputTrend = 0.0,
      latencyTrend = 2.0,
      sampleCount = 10
    )
    
    val adjustment = strategy.adjustBatchSize(
      currentBatchSize = 30,
      stats = stats,
      targetLatency = 100.millis,
      minBatchSize = 1,
      maxBatchSize = 1000
    )
    
    assert(adjustment.newBatchSize < 30) // Should decrease
    assertEquals(adjustment.reason, "reduce_for_latency")
  }

  test("BalancedStrategy should combine both throughput and latency considerations") {
    val strategy = new BalancedStrategy(
      latencyWeight = 0.6,
      throughputWeight = 0.4
    )
    
    val stats = BatchPerformanceStats(
      avgThroughput = 100.0,
      avgLatency = 80.0, // Below target
      avgBatchSize = 15.0,
      throughputTrend = 0.1, // Positive throughput trend
      latencyTrend = 1.0, // Slight latency increase
      sampleCount = 10
    )
    
    val adjustment = strategy.adjustBatchSize(
      currentBatchSize = 15,
      stats = stats,
      targetLatency = 100.millis,
      minBatchSize = 1,
      maxBatchSize = 1000
    )
    
    // Should make some adjustment based on balanced strategy
    assert(adjustment.reason.startsWith("balanced_"))
  }

  test("All strategies should respect min and max batch size constraints") {
    val strategies = List(
      new ThroughputOptimizedStrategy(),
      new LatencyOptimizedStrategy(),
      new BalancedStrategy()
    )
    
    val stats = BatchPerformanceStats(
      avgThroughput = 10.0,
      avgLatency = 500.0, // Very high latency
      avgBatchSize = 10.0,
      throughputTrend = -0.5, // Very negative trend
      latencyTrend = 10.0, // Very positive trend
      sampleCount = 10
    )
    
    strategies.foreach { strategy =>
      val adjustment = strategy.adjustBatchSize(
        currentBatchSize = 10,
        stats = stats,
        targetLatency = 100.millis,
        minBatchSize = 5,
        maxBatchSize = 50
      )
      
      // Should not go below minimum
      assert(adjustment.newBatchSize >= 5)
      // Should not go above maximum
      assert(adjustment.newBatchSize <= 50)
    }
  }

  test("Strategies should handle stable performance scenarios") {
    val strategy = new ThroughputOptimizedStrategy()
    
    val stats = BatchPerformanceStats(
      avgThroughput = 100.0,
      avgLatency = 90.0, // Close to target
      avgBatchSize = 20.0,
      throughputTrend = 0.01, // Very small trend
      latencyTrend = 0.01, // Very small trend
      sampleCount = 10
    )
    
    val adjustment = strategy.adjustBatchSize(
      currentBatchSize = 20,
      stats = stats,
      targetLatency = 100.millis,
      minBatchSize = 1,
      maxBatchSize = 1000
    )
    
    // Should maintain current batch size when performance is stable
    assertEquals(adjustment.newBatchSize, 20)
    assertEquals(adjustment.reason, "no_adjustment_needed")
  }

  test("BatchSizeAdjustment should contain all required fields") {
    val adjustment = BatchSizeAdjustment(
      newBatchSize = 25,
      reason = "throughput_improving",
      confidence = 0.8,
      triggerMetric = "throughput_trend",
      triggerValue = 0.15
    )
    
    assertEquals(adjustment.newBatchSize, 25)
    assertEquals(adjustment.reason, "throughput_improving")
    assertEquals(adjustment.confidence, 0.8)
    assertEquals(adjustment.triggerMetric, "throughput_trend")
    assertEquals(adjustment.triggerValue, 0.15)
  }
}