package gibbon.flows

import gibbon.core.Flow
import akka.stream.scaladsl.{Flow => AkkaFlow}
import scala.concurrent.duration.FiniteDuration
import scala.collection.immutable


class WindowFlow[T, R](
  windowDuration: FiniteDuration,
  extractor: T => R,
  aggregator: (R, R) => R
) extends Flow[T, R] {
  
  override def toAkkaFlow(): AkkaFlow[T, R, Any] = {
    AkkaFlow[T]
      .groupedWithin(Int.MaxValue, windowDuration)
      .map { events =>
        if (events.nonEmpty) {
          events.map(extractor).reduce(aggregator)
        } else {
          throw new NoSuchElementException("No events in window")
        }
      }
  }
}


class TumblingWindowFlow[T, R](
  windowDuration: FiniteDuration,
  extractor: T => R,
  aggregator: immutable.Seq[R] => R
) extends Flow[T, R] {
  
  override def toAkkaFlow(): AkkaFlow[T, R, Any] = {
    AkkaFlow[T]
      .groupedWithin(Int.MaxValue, windowDuration)
      .map { events =>
        if (events.nonEmpty) {
          aggregator(events.map(extractor))
        } else {
          throw new NoSuchElementException("No events in window")
        }
      }
  }
}


class SlidingWindowFlow[T, R](
  windowDuration: FiniteDuration,
  slideDuration: FiniteDuration,
  extractor: T => R,
  aggregator: immutable.Seq[R] => R
) extends Flow[T, R] {
  
  override def toAkkaFlow(): AkkaFlow[T, R, Any] = {
    // Note: This is a simplified implementation
    // For true sliding windows, you'd need more complex state management
    AkkaFlow[T]
      .groupedWithin(Int.MaxValue, windowDuration)
      .map { events =>
        if (events.nonEmpty) {
          aggregator(events.map(extractor))
        } else {
          throw new NoSuchElementException("No events in window")
        }
      }
  }
}


class CountWindowFlow[T, R](
  windowSize: Int,
  extractor: T => R,
  aggregator: immutable.Seq[R] => R
) extends Flow[T, R] {
  
  override def toAkkaFlow(): AkkaFlow[T, R, Any] = {
    AkkaFlow[T]
      .grouped(windowSize)
      .map { events =>
        if (events.nonEmpty) {
          aggregator(events.map(extractor))
        } else {
          throw new NoSuchElementException("No events in window")
        }
      }
  }
}

/**
 * WindowFlow companion object with factory methods
 */
object WindowFlow {
  
  /**
   * Create a simple time-based window flow
   */
  def apply[T, R](
    windowDuration: FiniteDuration,
    extractor: T => R,
    aggregator: (R, R) => R
  ): WindowFlow[T, R] = {
    new WindowFlow[T, R](windowDuration, extractor, aggregator)
  }
  
  /**
   * Create a tumbling window flow
   */
  def tumbling[T, R](
    windowDuration: FiniteDuration,
    extractor: T => R,
    aggregator: immutable.Seq[R] => R
  ): TumblingWindowFlow[T, R] = {
    new TumblingWindowFlow[T, R](windowDuration, extractor, aggregator)
  }
  
  /**
   * Create a sliding window flow
   */
  def sliding[T, R](
    windowDuration: FiniteDuration,
    slideDuration: FiniteDuration,
    extractor: T => R,
    aggregator: immutable.Seq[R] => R
  ): SlidingWindowFlow[T, R] = {
    new SlidingWindowFlow[T, R](windowDuration, slideDuration, extractor, aggregator)
  }
  
  /**
   * Create a count-based window flow
   */
  def count[T, R](
    windowSize: Int,
    extractor: T => R,
    aggregator: immutable.Seq[R] => R
  ): CountWindowFlow[T, R] = {
    new CountWindowFlow[T, R](windowSize, extractor, aggregator)
  }
}