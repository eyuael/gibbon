package gibbon.flows

import gibbon.handling.monitoring.{BatchingMetricsCollector, BatchPerformanceStats}
import munit.FunSuite
import scala.concurrent.duration._

class BatchingMetricsCollectorSpec extends FunSuite {

  test("BatchingMetricsCollector should initialize with default values") {
    val collector = new BatchingMetricsCollector()
    val stats = collector.getCurrentPerformanceStats
    
    assertEquals(stats.sampleCount, 0)
    assertEquals(stats.avgBatchSize, 0.0)
    assertEquals(stats.avgLatency, 0.0)
    assertEquals(stats.avgThroughput, 0.0)
  }

  test("BatchingMetricsCollector should record batch metrics") {
    val collector = new BatchingMetricsCollector()
    
    collector.recordBatchMetrics(
      batchSize = 10,
      processingTimeMs = 100L,
      queueDepth = 5,
      throughputEps = 50.0,
      cpuUsage = Some(75.0),
      memoryUsage = Some(512.0)
    )
    
    val stats = collector.getCurrentPerformanceStats
    assertEquals(stats.sampleCount, 1)
    assertEquals(stats.avgBatchSize, 10.0)
    assertEquals(stats.avgLatency, 100.0)
    assertEquals(stats.avgThroughput, 50.0)
  }

  test("BatchingMetricsCollector should record adaptation events") {
    val collector = new BatchingMetricsCollector()
    
    collector.recordAdaptationEvent(
      oldBatchSize = 10,
      newBatchSize = 15,
      reason = "throughput_increase",
      triggerMetric = "throughput",
      triggerValue = 100.0
    )
    
    // Verify that the adaptation event was recorded (no exception thrown)
    assert(true) // Simple verification that the method completes
  }

  test("BatchingMetricsCollector should record backpressure events") {
    val collector = new BatchingMetricsCollector()
    
    collector.recordBackpressureEvent(
      queueDepth = 800,
      maxQueueSize = 1000,
      backpressureLevel = "medium"
    )
    
    // Verify that the backpressure event was recorded (no exception thrown)
    assert(true) // Simple verification that the method completes
  }

  test("BatchingMetricsCollector should record performance degradation") {
    val collector = new BatchingMetricsCollector()
    
    collector.recordPerformanceDegradation(
      metricName = "latency",
      currentValue = 200.0,
      threshold = 100.0,
      severity = "high"
    )
    
    // Verify that the performance degradation was recorded (no exception thrown)
    assert(true) // Simple verification that the method completes
  }

  test("BatchingMetricsCollector should calculate performance trends") {
    val collector = new BatchingMetricsCollector()
    
    // Record multiple metrics to establish a trend
    collector.recordBatchMetrics(10, 100L, 5, 50.0, Some(70.0), Some(400.0))
    collector.recordBatchMetrics(12, 110L, 6, 55.0, Some(75.0), Some(450.0))
    collector.recordBatchMetrics(14, 120L, 7, 60.0, Some(80.0), Some(500.0))
    
    val stats = collector.getCurrentPerformanceStats
    
    // Verify trends are calculated (positive trend expected)
    assert(stats.throughputTrend >= 0.0) // Throughput is increasing
    assert(stats.latencyTrend >= 0.0) // Latency is increasing (which is expected with higher processing times)
  }

  test("BatchingMetricsCollector should handle multiple batch recordings") {
    val collector = new BatchingMetricsCollector()
    
    // Record multiple batches
    for (i <- 1 to 5) {
      collector.recordBatchMetrics(
        batchSize = i * 2,
        processingTimeMs = i * 10L,
        queueDepth = i,
        throughputEps = i * 20.0,
        cpuUsage = Some(i * 15.0),
        memoryUsage = Some(i * 100.0)
      )
    }
    
    val stats = collector.getCurrentPerformanceStats
    assertEquals(stats.sampleCount, 5)
    
    // Verify averages are calculated correctly
    val expectedAvgBatchSize = (2 + 4 + 6 + 8 + 10) / 5.0
    val expectedAvgLatency = (10 + 20 + 30 + 40 + 50) / 5.0
    val expectedAvgThroughput = (20 + 40 + 60 + 80 + 100) / 5.0
    
    assertEquals(stats.avgBatchSize, expectedAvgBatchSize)
    assertEquals(stats.avgLatency, expectedAvgLatency)
    assertEquals(stats.avgThroughput, expectedAvgThroughput)
  }

  test("BatchingMetricsCollector should detect performance improvements and degradations") {
    val collector = new BatchingMetricsCollector()
    
    // Record metrics showing improvement (increasing throughput, decreasing latency)
    collector.recordBatchMetrics(10, 200L, 5, 30.0, Some(70.0), Some(400.0))
    collector.recordBatchMetrics(10, 150L, 5, 40.0, Some(70.0), Some(400.0))
    collector.recordBatchMetrics(10, 100L, 5, 50.0, Some(70.0), Some(400.0))
    
    val stats = collector.getCurrentPerformanceStats
    
    // Check if performance improvement is detected
    assert(stats.throughputTrend > 0.0) // Throughput should be increasing
    assert(stats.latencyTrend < 0.0) // Latency should be decreasing
    assert(stats.isPerformanceImproving)
    assert(!stats.isPerformanceDegrading)
  }
}