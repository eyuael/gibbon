package gibbon.handling.monitoring

import java.time.Instant
import scala.collection.mutable

final case class Metric(
  name: String,
  value: Double,
  timestamp: Instant,
  tags: Map[String, String] = Map.empty
)

trait MetricsCollector {
  def record(metric: Metric): Unit
  def incr(name: String, tags: Map[String, String] = Map.empty): Unit
  def recordGauge(name: String, value: Double, tags: Map[String, String] = Map.empty): Unit
  def getMetrics: List[Metric]
}

class SimpleMetricsCollector extends MetricsCollector {
  private val metrics = mutable.ListBuffer.empty[Metric]

  override def record(metric: Metric): Unit = metrics.append(metric)
  override def incr(name: String, tags: Map[String, String] = Map.empty): Unit = record(Metric(name, 1, Instant.now(), tags))
  override def recordGauge(name: String, value: Double, tags: Map[String, String] = Map.empty): Unit = record(Metric(name, value, Instant.now(), tags))
  override def getMetrics: List[Metric] = metrics.toList
}

class BatchMetricsCollector extends MetricsCollector {
  private val metrics = mutable.ListBuffer.empty[Metric]

  override def record(metric: Metric): Unit = metrics.append(metric)
  override def incr(name: String, tags: Map[String, String] = Map.empty): Unit = record(Metric(name, 1, Instant.now(), tags))
  override def recordGauge(name: String, value: Double, tags: Map[String, String] = Map.empty): Unit = record(Metric(name, value, Instant.now(), tags))
  override def getMetrics: List[Metric] = metrics.toList
}