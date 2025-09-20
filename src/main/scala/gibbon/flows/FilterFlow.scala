package gibbon.flows

import gibbon.core.Flow
import akka.stream.scaladsl.{Flow => AkkaFlow}

/**
 * FilterFlow - filters events based on predicates
 * 
 * Example usage:
 * ```scala
 * val filterFlow = FilterFlow[Event[String, Int]](event => event.value > 100)
 * ```
 */
class FilterFlow[T](predicate: T => Boolean) extends Flow[T, T] {
  
  override def toAkkaFlow(): AkkaFlow[T, T, Any] = {
    AkkaFlow[T].filter(predicate)
  }
}

/**
 * FilterFlow companion object with factory methods
 */
object FilterFlow {
  
  /**
   * Create a simple filter flow with a predicate function
   */
  def apply[T](predicate: T => Boolean): FilterFlow[T] = {
    new FilterFlow[T](predicate)
  }
  
  /**
   * Create a filter flow that filters out null values
   */
  def filterNull[T]: FilterFlow[T] = {
    new FilterFlow[T](_ != null)
  }
  
  /**
   * Create a filter flow that filters out None values from Options
   */
  def filterNone[T]: FilterFlow[Option[T]] = {
    new FilterFlow[Option[T]](_.isDefined)
  }
  
  /**
   * Create a filter flow with a type-safe predicate
   */
  def typed[T](predicate: T => Boolean): FilterFlow[T] = {
    new FilterFlow[T](predicate)
  }
}