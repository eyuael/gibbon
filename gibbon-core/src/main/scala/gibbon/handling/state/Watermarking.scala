  package gibbon.handling.state

  import gibbon.runtime.StreamingRuntime

  import scala.concurrent.duration.FiniteDuration

  /**
   * Represents a watermark - a timestamp indicating that no events with
   * event time less than this watermark should arrive.
   *
   * @param timestamp The watermark timestamp in milliseconds
   * @param partition Optional partition identifier for per-partition watermarks
   */
  case class Watermark(
    timestamp: Long,
    partition: Option[String] = None
  ) {
    /**
     * Check if an event time is late relative to this watermark
     */
    def isLate(eventTime: Long): Boolean = eventTime < timestamp

    /**
     * Calculate the lateness of an event in milliseconds
     */
    def lateness(eventTime: Long): Long =
      if (isLate(eventTime)) timestamp - eventTime else 0L

    /**
     * Advance watermark to a new timestamp (monotonically increasing)
     */
    def advance(newTimestamp: Long): Watermark = {
      require(newTimestamp >= timestamp,
        s"Watermark cannot go backwards: current=$timestamp, new=$newTimestamp")
      copy(timestamp = newTimestamp)
    }
  }

  object Watermark {
    /**
     * Initial watermark representing the beginning of time
     */
    val Min: Watermark = Watermark(Long.MinValue)

    /**
     * Create a global watermark (no partition)
     */
    def global(timestamp: Long): Watermark = Watermark(timestamp, None)

    /**
     * Create a per-partition watermark
     */
    def forPartition(timestamp: Long, partition: String): Watermark =
      Watermark(timestamp, Some(partition))
  }

  /**
   * Strategy for generating watermarks
   */
  sealed trait WatermarkStrategy

  object WatermarkStrategy {
    /**
     * Emit watermarks at regular intervals based on system time
     *
     * @param interval How often to emit watermarks
     */
    case class Periodic(interval: FiniteDuration) extends WatermarkStrategy

    /**
     * Emit watermarks based on the maximum observed event time minus a fixed delay
     * (For future implementation)
     *
     * @param maxOutOfOrderness Maximum time events can be out of order
     */
    case class BoundedOutOfOrderness(maxOutOfOrderness: FiniteDuration) extends WatermarkStrategy

    /**
     * Extract watermarks from special marker events in the stream
     * (For future implementation)
     *
     * @param extractor Function to extract watermark from events (None if not a marker)
     */
    case class Punctuated[T](extractor: T => Option[Long]) extends WatermarkStrategy
  }

  /**
   * Immutable snapshot of watermark state across all partitions
   */
  case class WatermarkState(
    perPartition: Map[String, Watermark],
    globalWatermark: Watermark,
    lastUpdate: Long = System.currentTimeMillis()
  ) {
    /**
     * Get watermark for a specific partition
     */
    def getPartitionWatermark(partition: String): Watermark =
      perPartition.getOrElse(partition, Watermark.Min)

    /**
     * Update watermark for a specific partition
     * Global watermark is the minimum across all partitions
     */
    def updatePartition(partition: String, watermark: Watermark): WatermarkState = {
      val updated = perPartition + (partition -> watermark)
      val newGlobal = if (updated.isEmpty) Watermark.Min
                      else Watermark.global(updated.values.map(_.timestamp).min)

      copy(
        perPartition = updated,
        globalWatermark = newGlobal,
        lastUpdate = System.currentTimeMillis()
      )
    }

    /**
     * Advance watermark for a partition based on observed event time
     */
    def advancePartition(partition: String, eventTime: Long): WatermarkState = {
      val current = getPartitionWatermark(partition)
      if (eventTime > current.timestamp) {
        updatePartition(partition, Watermark.forPartition(eventTime, partition))
      } else {
        this
      }
    }

    /**
     * Check if an event is late relative to its partition's watermark
     */
    def isLate(partition: String, eventTime: Long): Boolean = {
      val watermark = getPartitionWatermark(partition)
      watermark.isLate(eventTime)
    }

    /**
     * Calculate lateness for an event
     */
    def lateness(partition: String, eventTime: Long): Long = {
      val watermark = getPartitionWatermark(partition)
      watermark.lateness(eventTime)
    }
  }

  object WatermarkState {
    /**
     * Create an empty watermark state
     */
    def empty: WatermarkState = WatermarkState(
      perPartition = Map.empty,
      globalWatermark = Watermark.Min
    )

    /**
     * Create initial state with given partitions
     */
    def withPartitions(partitions: Seq[String]): WatermarkState = {
      val initial = partitions.map(p => p -> Watermark.Min).toMap
      WatermarkState(
        perPartition = initial,
        globalWatermark = Watermark.Min
      )
    }
  }

  /**
   * Statistics about watermark progression and late events
   */
  case class WatermarkStats(
    currentGlobalWatermark: Long,
    partitionWatermarks: Map[String, Long],
    totalEvents: Long,
    lateEvents: Long,
    maxLateness: Long,
    avgLateness: Double,
    watermarkLag: Long  // Difference between current time and watermark
  ) {
    /**
     * Percentage of events that arrived late
     */
    def lateEventPercentage: Double =
      if (totalEvents > 0) (lateEvents.toDouble / totalEvents) * 100.0 else 0.0

    override def toString: String = {
      s"""WatermarkStats(
         |  globalWatermark=$currentGlobalWatermark,
         |  partitions=${partitionWatermarks.size},
         |  totalEvents=$totalEvents,
         |  lateEvents=$lateEvents (${f"$lateEventPercentage%.2f"}%%),
         |  maxLateness=${maxLateness}ms,
         |  avgLateness=${f"$avgLateness%.2f"}ms,
         |  watermarkLag=${watermarkLag}ms
         |)""".stripMargin
    }
  }

  object WatermarkStats {
    def empty: WatermarkStats = WatermarkStats(
      currentGlobalWatermark = Long.MinValue,
      partitionWatermarks = Map.empty,
      totalEvents = 0,
      lateEvents = 0,
      maxLateness = 0,
      avgLateness = 0.0,
      watermarkLag = 0
    )
  }