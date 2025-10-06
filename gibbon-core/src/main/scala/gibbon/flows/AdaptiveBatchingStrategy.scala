package gibbon.flows

import gibbon.handling.monitoring.{BatchingMetricsCollector, BatchPerformanceStats}
import scala.concurrent.duration.FiniteDuration
import java.time.Instant

/**
 * Strategy for adaptive batch size adjustment based on performance metrics
 */
sealed trait AdaptiveBatchingStrategy {
  def adjustBatchSize(
    currentBatchSize: Int,
    stats: BatchPerformanceStats,
    targetLatency: FiniteDuration,
    minBatchSize: Int,
    maxBatchSize: Int
  ): BatchSizeAdjustment
}

/**
 * Result of batch size adjustment decision
 */
case class BatchSizeAdjustment(
  newBatchSize: Int,
  reason: String,
  confidence: Double,
  triggerMetric: String,
  triggerValue: Double
)

/**
 * Throughput-optimized adaptive strategy
 * Increases batch size when throughput is improving, decreases when latency exceeds target
 */
class ThroughputOptimizedStrategy(
  latencyThreshold: Double = 1.5, // Multiplier of target latency before reducing batch size
  throughputImprovementThreshold: Double = 0.05, // 5% improvement threshold
  aggressiveness: Double = 0.2 // How much to adjust batch size (20% by default)
) extends AdaptiveBatchingStrategy {

  override def adjustBatchSize(
    currentBatchSize: Int,
    stats: BatchPerformanceStats,
    targetLatency: FiniteDuration,
    minBatchSize: Int,
    maxBatchSize: Int
  ): BatchSizeAdjustment = {
    
    val targetLatencyMs = targetLatency.toMillis.toDouble
    val latencyRatio = stats.avgLatency / targetLatencyMs
    
    // Priority 1: Reduce batch size if latency is too high
    if (latencyRatio > latencyThreshold) {
      val reduction = Math.max(1, (currentBatchSize * aggressiveness).toInt)
      val newSize = Math.max(minBatchSize, currentBatchSize - reduction)
      return BatchSizeAdjustment(
        newSize,
        "latency_exceeded_threshold",
        0.9,
        "latency_ratio",
        latencyRatio
      )
    }
    
    // Priority 2: Increase batch size if throughput is improving and latency is acceptable
    if (stats.throughputTrend > throughputImprovementThreshold && latencyRatio < 0.8) {
      val increase = Math.max(1, (currentBatchSize * aggressiveness).toInt)
      val newSize = Math.min(maxBatchSize, currentBatchSize + increase)
      return BatchSizeAdjustment(
        newSize,
        "throughput_improving",
        0.7,
        "throughput_trend",
        stats.throughputTrend
      )
    }
    
    // Priority 3: Decrease batch size if throughput is declining
    if (stats.throughputTrend < -throughputImprovementThreshold) {
      val reduction = Math.max(1, (currentBatchSize * aggressiveness * 0.5).toInt)
      val newSize = Math.max(minBatchSize, currentBatchSize - reduction)
      return BatchSizeAdjustment(
        newSize,
        "throughput_declining",
        0.6,
        "throughput_trend",
        stats.throughputTrend
      )
    }
    
    // No adjustment needed
    BatchSizeAdjustment(
      currentBatchSize,
      "no_adjustment_needed",
      0.5,
      "stable_performance",
      0.0
    )
  }
}

/**
 * Latency-optimized adaptive strategy
 * Prioritizes meeting latency targets over maximizing throughput
 */
class LatencyOptimizedStrategy(
  latencyTolerance: Double = 0.1, // 10% tolerance above target latency
  minAdjustmentPercent: Double = 0.1 // Minimum 10% adjustment
) extends AdaptiveBatchingStrategy {

  override def adjustBatchSize(
    currentBatchSize: Int,
    stats: BatchPerformanceStats,
    targetLatency: FiniteDuration,
    minBatchSize: Int,
    maxBatchSize: Int
  ): BatchSizeAdjustment = {
    
    val targetLatencyMs = targetLatency.toMillis.toDouble
    val latencyDiff = (stats.avgLatency - targetLatencyMs) / targetLatencyMs
    
    if (Math.abs(latencyDiff) < latencyTolerance) {
      return BatchSizeAdjustment(
        currentBatchSize,
        "latency_within_tolerance",
        0.8,
        "latency_diff",
        latencyDiff
      )
    }
    
    // Adjust batch size inversely proportional to latency difference
    val adjustmentFactor = -latencyDiff * 0.5 // Negative because higher latency = smaller batch
    val adjustment = Math.max(
      (currentBatchSize * minAdjustmentPercent).toInt,
      Math.abs((currentBatchSize * adjustmentFactor).toInt)
    )
    
    val newSize = if (latencyDiff > 0) {
      // Latency too high, reduce batch size
      Math.max(minBatchSize, currentBatchSize - adjustment)
    } else {
      // Latency below target, can increase batch size
      Math.min(maxBatchSize, currentBatchSize + adjustment)
    }
    
    BatchSizeAdjustment(
      newSize,
      if (latencyDiff > 0) "reduce_for_latency" else "increase_for_efficiency",
      0.8,
      "latency_diff",
      latencyDiff
    )
  }
}

/**
 * Balanced adaptive strategy
 * Balances throughput and latency considerations
 */
class BalancedStrategy(
  latencyWeight: Double = 0.6,
  throughputWeight: Double = 0.4
) extends AdaptiveBatchingStrategy {

  private val throughputStrategy = new ThroughputOptimizedStrategy()
  private val latencyStrategy = new LatencyOptimizedStrategy()

  override def adjustBatchSize(
    currentBatchSize: Int,
    stats: BatchPerformanceStats,
    targetLatency: FiniteDuration,
    minBatchSize: Int,
    maxBatchSize: Int
  ): BatchSizeAdjustment = {
    
    val throughputAdjustment = throughputStrategy.adjustBatchSize(
      currentBatchSize, stats, targetLatency, minBatchSize, maxBatchSize
    )
    
    val latencyAdjustment = latencyStrategy.adjustBatchSize(
      currentBatchSize, stats, targetLatency, minBatchSize, maxBatchSize
    )
    
    // Weighted combination of both strategies
    val weightedSize = (
      latencyAdjustment.newBatchSize * latencyWeight +
      throughputAdjustment.newBatchSize * throughputWeight
    ).toInt
    
    val finalSize = Math.max(minBatchSize, Math.min(maxBatchSize, weightedSize))
    
    // Choose the reason from the strategy with higher confidence
    val (reason, triggerMetric, triggerValue) = if (latencyAdjustment.confidence > throughputAdjustment.confidence) {
      (s"balanced_latency_${latencyAdjustment.reason}", latencyAdjustment.triggerMetric, latencyAdjustment.triggerValue)
    } else {
      (s"balanced_throughput_${throughputAdjustment.reason}", throughputAdjustment.triggerMetric, throughputAdjustment.triggerValue)
    }
    
    BatchSizeAdjustment(
      finalSize,
      reason,
      Math.max(latencyAdjustment.confidence, throughputAdjustment.confidence) * 0.8,
      triggerMetric,
      triggerValue
    )
  }
}