package gibbon.handling.monitoring

import java.time.Instant
import scala.concurrent.duration.FiniteDuration
import scala.collection.mutable

/**
 * Specialized metrics collector for adaptive batching operations.
 * Tracks batch performance, adaptation events, and system metrics.
 */
class BatchingMetricsCollector extends SimpleMetricsCollector {
  
  private val throughputHistory = mutable.Queue[Double]()
  private val latencyHistory = mutable.Queue[Double]()
  private val batchSizeHistory = mutable.Queue[Int]()
  private val maxHistorySize = 100
  
  /**
   * Records comprehensive batch processing metrics
   */
  def recordBatchMetrics(
    batchSize: Int,
    processingTimeMs: Long,
    queueDepth: Int,
    throughputEps: Double,
    cpuUsage: Option[Double] = None,
    memoryUsage: Option[Double] = None
  ): Unit = {
    val timestamp = Instant.now()
    val tags = Map("batch_id" -> java.util.UUID.randomUUID().toString.take(8))
    
    // Core batch metrics
    record(Metric("batch.size", batchSize.toDouble, timestamp, tags))
    record(Metric("batch.processing_time_ms", processingTimeMs.toDouble, timestamp, tags))
    record(Metric("batch.queue_depth", queueDepth.toDouble, timestamp, tags))
    record(Metric("batch.throughput_eps", throughputEps, timestamp, tags))
    
    // System resource metrics
    cpuUsage.foreach(cpu => record(Metric("batch.cpu_usage_percent", cpu, timestamp, tags)))
    memoryUsage.foreach(mem => record(Metric("batch.memory_usage_mb", mem, timestamp, tags)))
    
    // Update history for trend analysis
    updateHistory(throughputEps, processingTimeMs.toDouble, batchSize)
    
    // Calculate and record derived metrics
    recordDerivedMetrics(timestamp, tags)
  }
  
  /**
   * Records batch adaptation events when batch size changes
   */
  def recordAdaptationEvent(
    oldBatchSize: Int,
    newBatchSize: Int,
    reason: String,
    triggerMetric: String,
    triggerValue: Double
  ): Unit = {
    val timestamp = Instant.now()
    val tags = Map(
      "reason" -> reason,
      "old_size" -> oldBatchSize.toString,
      "trigger_metric" -> triggerMetric,
      "trigger_value" -> triggerValue.toString
    )
    
    record(Metric("batch.adaptation", newBatchSize.toDouble, timestamp, tags))
    record(Metric("batch.adaptation.size_change", (newBatchSize - oldBatchSize).toDouble, timestamp, tags))
    
    // Record adaptation frequency
    incr("batch.adaptation.count", Map("reason" -> reason))
  }
  
  /**
   * Records performance degradation events
   */
  def recordPerformanceDegradation(
    metricName: String,
    currentValue: Double,
    threshold: Double,
    severity: String
  ): Unit = {
    val timestamp = Instant.now()
    val tags = Map(
      "metric" -> metricName,
      "severity" -> severity,
      "threshold" -> threshold.toString
    )
    
    record(Metric("batch.performance.degradation", currentValue, timestamp, tags))
    incr("batch.performance.degradation.count", tags)
  }
  
  /**
   * Records backpressure events
   */
  def recordBackpressureEvent(
    queueDepth: Int,
    maxQueueSize: Int,
    backpressureLevel: String
  ): Unit = {
    val timestamp = Instant.now()
    val tags = Map(
      "level" -> backpressureLevel,
      "queue_utilization" -> (queueDepth.toDouble / maxQueueSize * 100).toString
    )
    
    record(Metric("batch.backpressure.queue_depth", queueDepth.toDouble, timestamp, tags))
    incr("batch.backpressure.events", tags)
  }
  
  /**
   * Gets current performance statistics for adaptation decisions
   */
  def getCurrentPerformanceStats: BatchPerformanceStats = {
    BatchPerformanceStats(
      avgThroughput = if (throughputHistory.nonEmpty) throughputHistory.sum / throughputHistory.size else 0.0,
      avgLatency = if (latencyHistory.nonEmpty) latencyHistory.sum / latencyHistory.size else 0.0,
      avgBatchSize = if (batchSizeHistory.nonEmpty) batchSizeHistory.sum.toDouble / batchSizeHistory.size else 0.0,
      throughputTrend = calculateTrend(throughputHistory.toList),
      latencyTrend = calculateTrend(latencyHistory.toList),
      sampleCount = throughputHistory.size
    )
  }
  
  /**
   * Gets metrics for a specific time window
   */
  def getMetricsInWindow(windowStart: Instant, windowEnd: Instant): List[Metric] = {
    getMetrics.filter { metric =>
      metric.timestamp.isAfter(windowStart) && metric.timestamp.isBefore(windowEnd)
    }
  }
  
  private def updateHistory(throughput: Double, latency: Double, batchSize: Int): Unit = {
    // Add new values
    throughputHistory.enqueue(throughput)
    latencyHistory.enqueue(latency)
    batchSizeHistory.enqueue(batchSize)
    
    // Maintain history size
    if (throughputHistory.size > maxHistorySize) throughputHistory.dequeue()
    if (latencyHistory.size > maxHistorySize) latencyHistory.dequeue()
    if (batchSizeHistory.size > maxHistorySize) batchSizeHistory.dequeue()
  }
  
  private def recordDerivedMetrics(timestamp: Instant, tags: Map[String, String]): Unit = {
    val stats = getCurrentPerformanceStats
    
    // Record trend metrics
    record(Metric("batch.throughput.trend", stats.throughputTrend, timestamp, tags))
    record(Metric("batch.latency.trend", stats.latencyTrend, timestamp, tags))
    
    // Record efficiency metrics
    val efficiency = if (stats.avgLatency > 0) stats.avgThroughput / stats.avgLatency else 0.0
    record(Metric("batch.efficiency", efficiency, timestamp, tags))
  }
  
  private def calculateTrend(values: List[Double]): Double = {
    if (values.size < 2) return 0.0
    
    val n = values.size
    val sumX = (1 to n).sum.toDouble
    val sumY = values.sum
    val sumXY = values.zipWithIndex.map { case (y, i) => y * (i + 1) }.sum
    val sumX2 = (1 to n).map(x => x * x).sum.toDouble
    
    // Linear regression slope
    (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX)
  }
}

/**
 * Performance statistics for batch processing
 */
case class BatchPerformanceStats(
  avgThroughput: Double,
  avgLatency: Double,
  avgBatchSize: Double,
  throughputTrend: Double,
  latencyTrend: Double,
  sampleCount: Int
) {
  def isPerformanceImproving: Boolean = throughputTrend > 0 && latencyTrend < 0
  def isPerformanceDegrading: Boolean = throughputTrend < 0 || latencyTrend > 0
}