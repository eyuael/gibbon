package gibbon.flows

import gibbon.core.Flow
import gibbon.runtime.StreamingRuntime
import gibbon.handling.monitoring.{BatchingMetricsCollector, BatchPerformanceStats}
import scala.concurrent.duration.FiniteDuration
import scala.collection.mutable
import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}
import java.util.concurrent.atomic.{AtomicInteger, AtomicLong, AtomicReference}
import scala.util.{Success, Failure}

/**
 * Adaptive batching flow that dynamically adjusts batch size based on performance metrics
 */
class AdaptiveBatchFlow[T](
  initialBatchSize: Int = 100,
  minBatchSize: Int = 10,
  maxBatchSize: Int = 1000,
  targetLatency: FiniteDuration = FiniteDuration(100, "ms"),
  adaptationInterval: FiniteDuration = FiniteDuration(5, "seconds"),
  metricsCollector: BatchingMetricsCollector = new BatchingMetricsCollector(),
  strategy: AdaptiveBatchingStrategy = new ThroughputOptimizedStrategy(),
  maxQueueSize: Int = 10000
)(implicit ec: ExecutionContext) extends Flow[T, List[T]] {

  // Thread-safe state management
  private val currentBatchSize = new AtomicInteger(initialBatchSize)
  private val lastAdaptationTime = new AtomicLong(System.currentTimeMillis())
  private val batchBuffer = new AtomicReference(mutable.Queue.empty[T])
  private val processedElements = new AtomicLong(0)
  private val batchCounter = new AtomicLong(0)
  
  // Performance tracking
  private val batchStartTime = new AtomicLong(System.currentTimeMillis())
  private val lastThroughputMeasurement = new AtomicLong(System.currentTimeMillis())
  private val elementsInLastInterval = new AtomicLong(0)

  override def toRuntimeFlow[RT <: StreamingRuntime]()(implicit runtime: RT): runtime.Flow[T, List[T], runtime.NotUsed] = {
    runtime.mapFlow[T, List[T]] { element =>
      processElement(element)
    }
  }

  /**
   * Process a single element and return a batch if ready
   */
  private def processElement(element: T): List[T] = {
    val currentTime = System.currentTimeMillis()
    val buffer = batchBuffer.get()
    
    synchronized {
      // Add element to buffer
      buffer.enqueue(element)
      processedElements.incrementAndGet()
      elementsInLastInterval.incrementAndGet()
      
      // Check if we should emit a batch
      val shouldEmitBatch = buffer.size >= currentBatchSize.get() || 
                           shouldForceEmitDueToTime(currentTime)
      
      if (shouldEmitBatch && buffer.nonEmpty) {
        val batch = buffer.dequeueAll(_ => true).toList
        emitBatch(batch, currentTime)
      } else {
        List.empty[T]
      }
    }
  }

  /**
   * Emit a batch and record metrics
   */
  private def emitBatch(batch: List[T], currentTime: Long): List[T] = {
    val batchId = batchCounter.incrementAndGet()
    val processingStartTime = batchStartTime.get()
    val processingTime = currentTime - processingStartTime
    
    // Calculate current throughput
    val timeSinceLastMeasurement = currentTime - lastThroughputMeasurement.get()
    val throughput = if (timeSinceLastMeasurement > 0) {
      (elementsInLastInterval.get().toDouble / timeSinceLastMeasurement) * 1000.0 // elements per second
    } else 0.0
    
    // Record batch metrics
    val queueDepth = batchBuffer.get().size
    metricsCollector.recordBatchMetrics(
      batchSize = batch.size,
      processingTimeMs = processingTime,
      queueDepth = queueDepth,
      throughputEps = throughput,
      cpuUsage = getCurrentCpuUsage(),
      memoryUsage = getCurrentMemoryUsage()
    )
    
    // Check for backpressure
    if (queueDepth > maxQueueSize * 0.8) {
      val backpressureLevel = if (queueDepth > maxQueueSize * 0.95) "critical" 
                             else if (queueDepth > maxQueueSize * 0.9) "high" 
                             else "medium"
      metricsCollector.recordBackpressureEvent(queueDepth, maxQueueSize, backpressureLevel)
    }
    
    // Reset measurement counters
    batchStartTime.set(currentTime)
    lastThroughputMeasurement.set(currentTime)
    elementsInLastInterval.set(0)
    
    // Trigger adaptation if needed
    triggerAdaptationIfNeeded(currentTime)
    
    batch
  }

  /**
   * Check if we should force emit a batch due to time constraints
   */
  private def shouldForceEmitDueToTime(currentTime: Long): Boolean = {
    val timeSinceLastBatch = currentTime - batchStartTime.get()
    timeSinceLastBatch > targetLatency.toMillis * 2 // Force emit if we're taking too long
  }

  /**
   * Trigger batch size adaptation if the adaptation interval has passed
   */
  private def triggerAdaptationIfNeeded(currentTime: Long): Unit = {
    val timeSinceLastAdaptation = currentTime - lastAdaptationTime.get()
    
    if (timeSinceLastAdaptation >= adaptationInterval.toMillis) {
      Future {
        performAdaptation()
      }.onComplete {
        case Success(_) => // Adaptation completed successfully
        case Failure(ex) => 
          metricsCollector.record(
            gibbon.handling.monitoring.Metric(
              "batch.adaptation.error", 
              1.0, 
              Instant.now(), 
              Map("error" -> ex.getMessage)
            )
          )
      }
      
      lastAdaptationTime.set(currentTime)
    }
  }

  /**
   * Perform batch size adaptation based on current performance metrics
   */
  private def performAdaptation(): Unit = {
    val stats = metricsCollector.getCurrentPerformanceStats
    
    // Only adapt if we have enough data
    if (stats.sampleCount < 3) return
    
    val oldBatchSize = currentBatchSize.get()
    val adjustment = strategy.adjustBatchSize(
      oldBatchSize,
      stats,
      targetLatency,
      minBatchSize,
      maxBatchSize
    )
    
    // Apply the adjustment if it's significant enough
    if (adjustment.newBatchSize != oldBatchSize && adjustment.confidence > 0.5) {
      currentBatchSize.set(adjustment.newBatchSize)
      
      // Record the adaptation event
      metricsCollector.recordAdaptationEvent(
        oldBatchSize,
        adjustment.newBatchSize,
        adjustment.reason,
        adjustment.triggerMetric,
        adjustment.triggerValue
      )
      
      // Check for performance degradation
      if (adjustment.reason.contains("declining") || adjustment.reason.contains("exceeded")) {
        metricsCollector.recordPerformanceDegradation(
          adjustment.triggerMetric,
          adjustment.triggerValue,
          if (adjustment.triggerMetric == "latency_ratio") 1.5 else 0.05,
          if (adjustment.confidence > 0.8) "high" else "medium"
        )
      }
    }
  }

  /**
   * Get current CPU usage (simplified implementation)
   */
  private def getCurrentCpuUsage(): Option[Double] = {
    try {
      val runtime = Runtime.getRuntime
      val processors = runtime.availableProcessors()
      // This is a simplified CPU usage estimation
      Some(((processors - runtime.freeMemory().toDouble / runtime.totalMemory()) / processors) * 100)
    } catch {
      case _: Exception => None
    }
  }

  /**
   * Get current memory usage in MB
   */
  private def getCurrentMemoryUsage(): Option[Double] = {
    try {
      val runtime = Runtime.getRuntime
      val usedMemory = runtime.totalMemory() - runtime.freeMemory()
      Some(usedMemory / (1024.0 * 1024.0)) // Convert to MB
    } catch {
      case _: Exception => None
    }
  }

  /**
   * Get current batch size (for monitoring/debugging)
   */
  def getCurrentBatchSize: Int = currentBatchSize.get()

  /**
   * Get current queue depth (for monitoring/debugging)
   */
  def getCurrentQueueDepth: Int = batchBuffer.get().size

  /**
   * Get performance statistics
   */
  def getPerformanceStats: BatchPerformanceStats = metricsCollector.getCurrentPerformanceStats
}
 